package fr.heneria.nexus;

import fr.heneria.nexus.arena.manager.ArenaManager;
import fr.heneria.nexus.arena.repository.ArenaRepository;
import fr.heneria.nexus.arena.repository.JdbcArenaRepository;
import fr.heneria.nexus.command.ArenaCommand;
import fr.heneria.nexus.db.HikariDataSourceProvider;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.Scope;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.logging.LogService;
import liquibase.logging.LogType;
import liquibase.logging.Logger;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.logging.Level;

public final class Nexus extends JavaPlugin {

    private HikariDataSourceProvider dataSourceProvider;
    private ArenaManager arenaManager;

    @Override
    public void onEnable() {
        try {
            // 1. Initialiser le pool de connexions
            this.dataSourceProvider = new HikariDataSourceProvider();
            this.dataSourceProvider.init(this);

            if (this.dataSourceProvider.getDataSource() == null) {
                getLogger().severe("La source de données n'a pas pu être initialisée. Le plugin Nexus va se désactiver.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }

            // 2. Exécuter les migrations Liquibase
            runLiquibaseMigrations();

            // 3. Initialiser le Repository
            ArenaRepository arenaRepository = new JdbcArenaRepository(this.dataSourceProvider.getDataSource());

            // 4. Initialiser le Manager
            this.arenaManager = new ArenaManager(arenaRepository);

            // 5. Charger les arènes existantes
            this.arenaManager.loadArenas();
            getLogger().info(this.arenaManager.getAllArenas().size() + " arène(s) chargée(s).");

            // 6. Enregistrer les commandes
            Objects.requireNonNull(getCommand("nx"), "La commande 'nx' n'est pas définie dans plugin.yml")
                   .setExecutor(new ArenaCommand(this.arenaManager));

            getLogger().info("Le plugin Nexus a été activé avec succès !");

        } catch (Exception e) {
            getLogger().severe("Erreur critique lors du démarrage du plugin Nexus ! Le plugin va se désactiver.");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void runLiquibaseMigrations() throws SQLException, LiquibaseException {
        // **LA CORRECTION EST ICI**
        // On configure manuellement le logger de Liquibase pour qu'il utilise celui de Bukkit.
        Scope.set(Scope.Attr.logService, new LogService() {
            private final Logger bukkitLogger = new Logger() {
                @Override
                public void log(Level level, String message, Throwable e) {
                    getLogger().log(level, "[Liquibase] " + message, e);
                }
                // Implémentations minimales pour les autres méthodes
                @Override public void setName(String name) {}
                @Override public void setLogLevel(String logLevel) {}
                @Override public void setLogLevel(Level level) {}
                @Override public void severe(String message) { getLogger().severe("[Liquibase] " + message); }
                @Override public void severe(String message, Throwable e) { getLogger().log(Level.SEVERE, "[Liquibase] " + message, e); }
                @Override public void warning(String message) { getLogger().warning("[Liquibase] " + message); }
                @Override public void warning(String message, Throwable e) { getLogger().log(Level.WARNING, "[Liquibase] " + message, e); }
                @Override public void info(String message) { getLogger().info("[Liquibase] " + message); }
                @Override public void info(String message, Throwable e) { getLogger().log(Level.INFO, "[Liquibase] " + message, e); }
                @Override public void config(String message) { getLogger().info("[Liquibase] " + message); }
                @Override public void config(String message, Throwable e) { getLogger().log(Level.INFO, "[Liquibase] " + message, e); }
                @Override public void fine(String message) { /* Ignoré pour ne pas spammer */ }
                @Override public void fine(String message, Throwable e) { /* Ignoré */ }
                @Override public void debug(String message) { /* Ignoré */ }
                @Override public void debug(String message, Throwable e) { /* Ignoré */ }
                @Override public int getPriority() { return 5; }
            };
            @Override public Logger getLog(Class clazz) { return bukkitLogger; }
        });
        // **FIN DE LA CORRECTION**


        try (Connection connection = this.dataSourceProvider.getDataSource().getConnection()) {
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            String changelogFile = "db/changelog/db.changelog-master.xml";
            Liquibase liquibase = new Liquibase(changelogFile, new ClassLoaderResourceAccessor(this.getClassLoader()), database);
            liquibase.update(new Contexts(), new LabelExpression());
            getLogger().info("Les migrations Liquibase ont été vérifiées et appliquées si nécessaire.");
        }
    }

    @Override
    public void onDisable() {
        // Fermer le pool de connexions
        if (this.dataSourceProvider != null) {
            this.dataSourceProvider.close();
            getLogger().info("Pool de connexions à la base de données fermé.");
        }
        getLogger().info("Le plugin Nexus a été désactivé.");
    }
}
