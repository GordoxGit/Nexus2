package fr.heneria.nexus.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import javax.sql.DataSource;

public class HikariDataSourceProvider {

    private HikariDataSource dataSource;

    /**
     * Initializes the connection pool.
     * @param plugin The main plugin instance, used for logging.
     */
    public void init(JavaPlugin plugin) {
        HikariConfig config = new HikariConfig();

        // TODO: Externalize this configuration into a config.yml file.
        config.setJdbcUrl("jdbc:mariadb://localhost:3306/nexus?useSSL=false");
        config.setUsername("nexus_user");
        config.setPassword("nexus_password");

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        try {
            this.dataSource = new HikariDataSource(config);
            plugin.getLogger().info("Le pool de connexions à la base de données a été initialisé avec succès.");
        } catch (Exception e) {
            plugin.getLogger().severe("Impossible d'initialiser le pool de connexions à la base de données !");
            plugin.getLogger().severe(e.getMessage());
            // It might be a good idea to disable the plugin if the DB connection fails
            // but for now, we'll just log the error.
        }
    }

    /**
     * Closes the connection pool.
     */
    public void close() {
        if (this.dataSource != null && !this.dataSource.isClosed()) {
            this.dataSource.close();
        }
    }

    /**
     * Gets the configured DataSource.
     * @return The a DataSource instance.
     */
    public DataSource getDataSource() {
        return this.dataSource;
    }
}
