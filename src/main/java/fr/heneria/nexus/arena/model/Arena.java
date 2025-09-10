package fr.heneria.nexus.arena.model;

import org.bukkit.Location;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a game arena in memory.
 */
public class Arena {

    private int id; // Database ID, set after retrieval/saving
    private final String name;
    private final int maxPlayers;
    // Map<TeamID, Map<SpawnNumber, Location>>
    private final Map<Integer, Map<Integer, Location>> spawns;

    public Arena(String name, int maxPlayers) {
        this.name = name;
        this.maxPlayers = maxPlayers;
        this.spawns = new ConcurrentHashMap<>();
    }

    // --- Getters ---

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public Map<Integer, Map<Integer, Location>> getSpawns() {
        return spawns;
    }

    // --- Setters ---

    public void setId(int id) {
        this.id = id;
    }

    // --- Business Logic ---

    /**
     * Adds or updates a spawn point for a specific team and spawn number.
     * @param teamId The ID of the team.
     * @param spawnNumber The number of the spawn point for that team.
     * @param location The location of the spawn point.
     */
    public void setSpawn(int teamId, int spawnNumber, Location location) {
        spawns.computeIfAbsent(teamId, k -> new ConcurrentHashMap<>()).put(spawnNumber, location);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Arena arena = (Arena) o;
        return name.equals(arena.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "Arena{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", maxPlayers=" + maxPlayers +
                ", spawnsCount=" + spawns.values().stream().mapToLong(Map::size).sum() +
                '}';
    }
}
