package com.yourserver.playercustomisation.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.yourserver.playercustomisation.PlayerCustomisation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;

public class MySQL {
    private final PlayerCustomisation plugin;
    private HikariDataSource dataSource;

    public MySQL(PlayerCustomisation plugin) {
        this.plugin = plugin;
    }

    public boolean connect() {
        try {
            HikariConfig config = new HikariConfig();
            
            String host = plugin.getConfig().getString("mysql.host");
            int port = plugin.getConfig().getInt("mysql.port");
            String database = plugin.getConfig().getString("mysql.database");
            String username = plugin.getConfig().getString("mysql.username");
            String password = plugin.getConfig().getString("mysql.password");
            int poolSize = plugin.getConfig().getInt("mysql.pool-size", 10);
            
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
            config.setUsername(username);
            config.setPassword(password);
            config.setMaximumPoolSize(poolSize);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            config.setLeakDetectionThreshold(60000);
            
            // MySQL optimizations
            config.addDataSourceProperty("useSSL", "false");
            config.addDataSourceProperty("allowPublicKeyRetrieval", "true");
            config.addDataSourceProperty("serverTimezone", "UTC");
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            
            dataSource = new HikariDataSource(config);
            
            // Test connection
            try (Connection connection = dataSource.getConnection()) {
                plugin.getLogger().info("Successfully connected to MySQL database!");
            }
            
            // Create tables
            createTables();
            
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to MySQL database!", e);
            return false;
        }
    }

    private void createTables() {
        String playerDataTable = """
            CREATE TABLE IF NOT EXISTS player_data (
                uuid VARCHAR(36) PRIMARY KEY,
                username VARCHAR(16) NOT NULL,
                nickname VARCHAR(16),
                name_color VARCHAR(50),
                prefix_style VARCHAR(100),
                custom_prefix TEXT,
                last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )""";

        String tagRequestsTable = """
            CREATE TABLE IF NOT EXISTS custom_tag_requests (
                id INT AUTO_INCREMENT PRIMARY KEY,
                uuid VARCHAR(36) NOT NULL,
                username VARCHAR(16) NOT NULL,
                requested_tag TEXT NOT NULL,
                status ENUM('pending', 'approved', 'denied') DEFAULT 'pending',
                reviewed_by VARCHAR(36),
                requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                reviewed_at TIMESTAMP
            )""";

        try (Connection connection = getConnection()) {
            try (PreparedStatement stmt = connection.prepareStatement(playerDataTable)) {
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = connection.prepareStatement(tagRequestsTable)) {
                stmt.executeUpdate();
            }
            plugin.getLogger().info("Database tables created successfully!");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create database tables!", e);
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Database connection is not available!");
        }
        return dataSource.getConnection();
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Disconnected from MySQL database.");
        }
    }

    public boolean isConnected() {
        try {
            return dataSource != null && !dataSource.isClosed() && dataSource.getConnection().isValid(1);
        } catch (SQLException e) {
            return false;
        }
    }
}