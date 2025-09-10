package fr.heneria.nexus.arena.manager;

import fr.heneria.nexus.arena.model.Arena;
import fr.heneria.nexus.arena.repository.ArenaRepository;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ArenaManager {

    private final ArenaRepository arenaRepository;
    private final Map<String, Arena> arenasByName = new ConcurrentHashMap<>();

    public ArenaManager(ArenaRepository arenaRepository) {
        this.arenaRepository = arenaRepository;
    }

    /**
     * Loads all arenas from the database into memory.
     * This should be called on plugin startup.
     */
    public void loadArenas() {
        this.arenasByName.clear();
        Map<String, Arena> loadedArenas = arenaRepository.loadAll().stream()
                .collect(Collectors.toMap(Arena::getName, Function.identity()));
        this.arenasByName.putAll(loadedArenas);
    }

    /**
     * Creates a new arena in memory.
     * The arena is not persisted until {@link #saveArena(Arena)} is called.
     *
     * @param name       The unique name of the arena.
     * @param maxPlayers The maximum number of players for this arena.
     * @return The newly created Arena object, or null if an arena with that name already exists.
     */
    public Arena createArena(String name, int maxPlayers) {
        if (arenasByName.containsKey(name)) {
            return null; // Arena with this name already exists in memory
        }
        Arena newArena = new Arena(name, maxPlayers);
        arenasByName.put(name, newArena);
        return newArena;
    }

    /**
     * Persists an arena and its spawns to the database.
     *
     * @param arena The arena to save.
     */
    public void saveArena(Arena arena) {
        if (arena == null) return;
        arenaRepository.save(arena);
        // Ensure the in-memory map is consistent
        arenasByName.put(arena.getName(), arena);
    }

    /**
     * Deletes an arena from memory and the database.
     *
     * @param name The name of the arena to delete.
     * @return true if the arena was deleted, false otherwise.
     */
    public boolean deleteArena(String name) {
        if (!arenasByName.containsKey(name)) {
            return false;
        }
        boolean deletedFromDb = arenaRepository.deleteByName(name);
        if (deletedFromDb) {
            arenasByName.remove(name);
            return true;
        }
        return false;
    }

    /**
     * Gets an arena by its name.
     *
     * @param name The name of the arena.
     * @return An Optional containing the arena if found.
     */
    public Optional<Arena> getArena(String name) {
        return Optional.ofNullable(arenasByName.get(name));
    }

    /**
     * Gets a collection of all loaded arenas.
     *
     * @return A collection of all arenas.
     */
    public Collection<Arena> getAllArenas() {
        return arenasByName.values();
    }
}
