package fr.heneria.nexus.arena.repository;

import fr.heneria.nexus.arena.model.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JdbcArenaRepository implements ArenaRepository {

    private final DataSource dataSource;

    private static final String INSERT_ARENA = "INSERT INTO arenas (name, max_players) VALUES (?, ?)";
    private static final String UPDATE_ARENA = "UPDATE arenas SET name = ?, max_players = ? WHERE id = ?";
    private static final String FIND_ARENA_BY_NAME = "SELECT id, name, max_players FROM arenas WHERE name = ?";
    private static final String SELECT_ALL_ARENAS = "SELECT id, name, max_players FROM arenas";
    private static final String SELECT_SPAWNS_FOR_ARENA = "SELECT team_id, spawn_number, world, x, y, z, yaw, pitch FROM arena_spawns WHERE arena_id = ?";
    private static final String DELETE_SPAWNS_FOR_ARENA = "DELETE FROM arena_spawns WHERE arena_id = ?";
    private static final String INSERT_SPAWN = "INSERT INTO arena_spawns (arena_id, team_id, spawn_number, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String DELETE_ARENA_BY_NAME = "DELETE FROM arenas WHERE name = ?";

    public JdbcArenaRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(Arena arena) {
        try (Connection conn = dataSource.getConnection()) {
            // Use findByName to see if it exists and get its ID if it does
            Optional<Arena> existingArena = findByName(arena.getName());

            if (existingArena.isPresent()) {
                // Update existing arena
                arena.setId(existingArena.get().getId());
                try (PreparedStatement ps = conn.prepareStatement(UPDATE_ARENA)) {
                    ps.setString(1, arena.getName());
                    ps.setInt(2, arena.getMaxPlayers());
                    ps.setInt(3, arena.getId());
                    ps.executeUpdate();
                }
            } else {
                // Insert new arena
                try (PreparedStatement ps = conn.prepareStatement(INSERT_ARENA, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, arena.getName());
                    ps.setInt(2, arena.getMaxPlayers());
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            arena.setId(rs.getInt(1));
                        }
                    }
                }
            }

            // --- Save Spawns ---
            // 1. Delete all existing spawns for this arena
            try (PreparedStatement ps = conn.prepareStatement(DELETE_SPAWNS_FOR_ARENA)) {
                ps.setInt(1, arena.getId());
                ps.executeUpdate();
            }

            // 2. Insert all current spawns from the arena object
            if (arena.getSpawns() != null && !arena.getSpawns().isEmpty()) {
                try (PreparedStatement ps = conn.prepareStatement(INSERT_SPAWN)) {
                    for (Map.Entry<Integer, Map<Integer, Location>> teamEntry : arena.getSpawns().entrySet()) {
                        int teamId = teamEntry.getKey();
                        for (Map.Entry<Integer, Location> spawnEntry : teamEntry.getValue().entrySet()) {
                            int spawnNumber = spawnEntry.getKey();
                            Location loc = spawnEntry.getValue();

                            ps.setInt(1, arena.getId());
                            ps.setInt(2, teamId);
                            ps.setInt(3, spawnNumber);
                            ps.setString(4, loc.getWorld().getName());
                            ps.setDouble(5, loc.getX());
                            ps.setDouble(6, loc.getY());
                            ps.setDouble(7, loc.getZ());
                            ps.setFloat(8, loc.getYaw());
                            ps.setFloat(9, loc.getPitch());
                            ps.addBatch();
                        }
                    }
                    ps.executeBatch();
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save arena " + arena.getName(), e);
        }
    }

    @Override
    public List<Arena> loadAll() {
        List<Arena> arenas = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL_ARENAS);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Arena arena = new Arena(rs.getString("name"), rs.getInt("max_players"));
                arena.setId(rs.getInt("id"));
                loadSpawnsForArena(conn, arena);
                arenas.add(arena);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load all arenas", e);
        }
        return arenas;
    }

    @Override
    public Optional<Arena> findByName(String name) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_ARENA_BY_NAME)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Arena arena = new Arena(rs.getString("name"), rs.getInt("max_players"));
                    arena.setId(rs.getInt("id"));
                    loadSpawnsForArena(conn, arena);
                    return Optional.of(arena);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find arena by name: " + name, e);
        }
        return Optional.empty();
    }

    private void loadSpawnsForArena(Connection conn, Arena arena) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_SPAWNS_FOR_ARENA)) {
            ps.setInt(1, arena.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int teamId = rs.getInt("team_id");
                    int spawnNumber = rs.getInt("spawn_number");
                    World world = Bukkit.getWorld(rs.getString("world"));
                    if (world != null) {
                        Location loc = new Location(
                                world,
                                rs.getDouble("x"),
                                rs.getDouble("y"),
                                rs.getDouble("z"),
                                rs.getFloat("yaw"),
                                rs.getFloat("pitch")
                        );
                        arena.setSpawn(teamId, spawnNumber, loc);
                    } else {
                        // Log a warning if the world is not loaded
                        Bukkit.getLogger().warning("[Nexus] Could not load spawn for arena '" + arena.getName() + "' because world '" + rs.getString("world") + "' is not loaded.");
                    }
                }
            }
        }
    }

    @Override
    public boolean deleteByName(String name) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_ARENA_BY_NAME)) {
            ps.setString(1, name);
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete arena: " + name, e);
        }
    }
}
