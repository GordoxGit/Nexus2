package fr.heneria.nexus.arena.repository;

import fr.heneria.nexus.arena.model.Arena;

import java.util.List;
import java.util.Optional;

/**
 * Defines the contract for the persistence of arenas.
 */
public interface ArenaRepository {

    /**
     * Saves a new arena or updates an existing one.
     * If the arena is new (ID = 0), it should be inserted and its ID updated.
     * If the arena already has an ID, its data should be updated.
     * This method should also persist the associated spawns.
     *
     * @param arena The arena object to save.
     */
    void save(Arena arena);

    /**
     * Loads all arenas and their spawns from the database.
     *
     * @return A list of all arenas found.
     */
    List<Arena> loadAll();

    /**
     * Finds an arena by its unique name.
     *
     * @param name The name of the arena to find.
     * @return An Optional containing the arena if found, otherwise empty.
     */
    Optional<Arena> findByName(String name);

    /**
     * Deletes an arena from the database by its name.
     *
     * @param name The name of the arena to delete.
     * @return true if the arena was deleted, false otherwise.
     */
    boolean deleteByName(String name);
}
