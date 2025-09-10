package fr.heneria.nexus.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import javax.sql.DataSource;

public class HikariDataSourceProvider {

    private HikariDataSource dataSource;

    /**
     * Initialise le pool de connexions à partir du fichier config.yml du plugin.
     * @param plugin L'instance principale du plugin.
     */
    public void init(JavaPlugin plugin) {
        // Sauvegarde la configuration par défaut si elle n'existe pas
        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();

        HikariConfig hikariConfig = new HikariConfig();

        // Lecture des informations de la base de données depuis config.yml
        String host = config.getString("database.host", "localhost");
        int port = config.getInt("database.port", 3306);
        String dbName = config.getString("database.database", "nexus");
        String username = config.getString("database.username", "nexus_user");
        String password = config.getString("database.password", "your_password_here");

        // Construction de l'URL JDBC pour MariaDB
        String jdbcUrl = String.format("jdbc:mariadb://%s:%d/%s?useSSL=false", host, port, dbName);
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);

        // **IMPORTANT** : Spécifier explicitement le driver relocalisé pour éviter les conflits
        hikariConfig.setDriverClassName("fr.heneria.nexus.libs.mariadb.jdbc.Driver");

        // Configuration du pool HikariCP depuis config.yml
        hikariConfig.setMaximumPoolSize(config.getInt("database.hikari.maximum-pool-size", 10));
        hikariConfig.setMinimumIdle(config.getInt("database.hikari.minimum-idle", 5));
        hikariConfig.setConnectionTimeout(config.getLong("database.hikari.connection-timeout", 30000));
        hikariConfig.setIdleTimeout(config.getLong("database.hikari.idle-timeout", 600000));
        hikariConfig.setMaxLifetime(config.getLong("database.hikari.max-lifetime", 1800000));
        hikariConfig.setLeakDetectionThreshold(config.getLong("database.hikari.leak-detection-threshold", 60000));

        // Propriétés recommandées pour MariaDB
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        hikariConfig.setPoolName("Nexus-HikariPool");

        try {
            this.dataSource = new HikariDataSource(hikariConfig);
            plugin.getLogger().info("✅ Le pool de connexions à la base de données a été initialisé avec succès.");
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Impossible d'initialiser le pool de connexions à la base de données !");
            plugin.getLogger().severe("Vérifiez les informations dans votre fichier 'config.yml'.");
            plugin.getLogger().severe("Erreur détaillée : " + e.getMessage());
            // Propage l'exception pour que la méthode onEnable puisse désactiver le plugin
            throw new RuntimeException("La connexion à la base de données a échoué.", e);
        }
    }

    /**
     * Ferme le pool de connexions.
     */
    public void close() {
        if (this.dataSource != null && !this.dataSource.isClosed()) {
            this.dataSource.close();
        }
    }

    /**
     * Récupère le DataSource configuré.
     * @return L'instance du DataSource.
     */
    public DataSource getDataSource() {
        return this.dataSource;
    }
}
