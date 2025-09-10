package fr.heneria.nexus;

import fr.heneria.nexus.arena.manager.ArenaManager;
import fr.heneria.nexus.arena.repository.ArenaRepository;
import fr.heneria.nexus.arena.repository.JdbcArenaRepository;
import fr.heneria.nexus.command.ArenaCommand;
import fr.heneria.nexus.db.HikariDataSourceProvider;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

public final class Nexus extends JavaPlugin {

    private HikariDataSourceProvider dataSourceProvider;
    private ArenaManager arenaManager;

    @Override
    public void onEnable() {
        // 1. Initialiser le pool de connexions
        this.dataSourceProvider = new HikariDataSourceProvider();
        this.dataSourceProvider.init(this);

        if (this.dataSourceProvider.getDataSource() == null) {
            getLogger().severe("La source de données n'a pas pu être initialisée. Le plugin Nexus va se désactiver.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 2. Exécuter les migrations Liquibase
        try {
            runLiquibaseMigrations();
        } catch (SQLException | LiquibaseException e) {
            getLogger().severe("Erreur critique lors de l'exécution des migrations de la base de données ! Le plugin va se désactiver.");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 3. Initialiser le Repository
        ArenaRepository arenaRepository = new JdbcArenaRepository(this.dataSourceProvider.getDataSource());

        // 4. Initialiser le Manager
        this.arenaManager = new ArenaManager(arenaRepository);

        // 5. Charger les arènes existantes
        try {
            this.arenaManager.loadArenas();
            getLogger().info(this.arenaManager.getAllArenas().size() + " arène(s) chargée(s).");
        } catch (Exception e) {
            getLogger().severe("Une erreur est survenue lors du chargement des arènes !");
            e.printStackTrace();
        }

        // 6. Enregistrer les commandes
        Objects.requireNonNull(getCommand("nx"), "La commande 'nx' n'est pas définie dans plugin.yml")
               .setExecutor(new ArenaCommand(this.arenaManager));

        getLogger().info("Le plugin Nexus a été activé avec succès !");
    }

    private void runLiquibaseMigrations() throws SQLException, LiquibaseException {
        try (Connection connection = this.dataSourceProvider.getDataSource().getConnection()) {
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));

            // Spécifiez le chemin vers le fichier changelog maître
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
