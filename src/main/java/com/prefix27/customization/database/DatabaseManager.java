package com.prefix27.customization.database;

import com.prefix27.customization.PlayerCustomizationPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;

public class DatabaseManager {
    
    private final PlayerCustomizationPlugin plugin;
    private Connection connection;
    private final ExecutorService executor;
    private final ConcurrentHashMap<String, PreparedStatement> preparedStatements;
    
    public DatabaseManager(PlayerCustomizationPlugin plugin) {
        this.plugin = plugin;
        this.executor = Executors.newFixedThreadPool(3);
        this.preparedStatements = new ConcurrentHashMap<>();
    }
    
    public boolean initialize() {
        try {
            String databasePath = plugin.getConfig().getString("database.file", "customization.db");
            
            // Make sure the database directory exists
            File dbFile = new File(plugin.getDataFolder(), databasePath);
            if (!dbFile.getParentFile().exists()) {
                dbFile.getParentFile().mkdirs();
            }
            
            // Set up the SQLite connection
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            
            // Set up our tables
            createTables();
            
            // Get all our prepared statements ready
            prepareStatements();
            
            // Start the backup system
            startBackupTask();
            
            plugin.getLogger().info("Database initialized successfully!");
            return true;
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database", e);
            return false;
        }
    }
    
    private void createTables() throws SQLException {
        // Players table
        String createPlayersTable = """
            CREATE TABLE IF NOT EXISTS players (
                uuid VARCHAR(36) PRIMARY KEY,
                username VARCHAR(16) NOT NULL,
                current_name_color VARCHAR(50),
                current_name_gradient VARCHAR(100),
                current_prefix_id VARCHAR(50),
                current_nickname VARCHAR(32),
                rank VARCHAR(20) NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        
        // Available_Prefixes table
        String createAvailablePrefixesTable = """
            CREATE TABLE IF NOT EXISTS available_prefixes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid VARCHAR(36),
                prefix_id VARCHAR(50),
                prefix_type VARCHAR(20),
                earned_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                expires_at TIMESTAMP NULL,
                FOREIGN KEY (player_uuid) REFERENCES players(uuid)
            )
        """;
        
        // Custom_Prefix_Requests table
        String createCustomPrefixRequestsTable = """
            CREATE TABLE IF NOT EXISTS custom_prefix_requests (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid VARCHAR(36),
                requested_text VARCHAR(32),
                status VARCHAR(20) DEFAULT 'pending',
                requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                reviewed_by VARCHAR(36) NULL,
                reviewed_at TIMESTAMP NULL,
                review_reason TEXT NULL,
                expires_at TIMESTAMP NULL,
                FOREIGN KEY (player_uuid) REFERENCES players(uuid)
            )
        """;
        
        // Prefix_Definitions table
        String createPrefixDefinitionsTable = """
            CREATE TABLE IF NOT EXISTS prefix_definitions (
                prefix_id VARCHAR(50) PRIMARY KEY,
                display_text VARCHAR(32),
                category VARCHAR(20),
                color_options TEXT,
                gradient_options TEXT,
                is_active BOOLEAN DEFAULT TRUE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        
        // Usage_Analytics table
        String createUsageAnalyticsTable = """
            CREATE TABLE IF NOT EXISTS usage_analytics (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid VARCHAR(36),
                action_type VARCHAR(20),
                old_value VARCHAR(100),
                new_value VARCHAR(100),
                timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createPlayersTable);
            stmt.execute(createAvailablePrefixesTable);
            stmt.execute(createCustomPrefixRequestsTable);
            stmt.execute(createPrefixDefinitionsTable);
            stmt.execute(createUsageAnalyticsTable);
        }
        
        plugin.getLogger().info("Database tables created successfully!");
    }
    
    private void prepareStatements() throws SQLException {
        // Player data queries
        preparedStatements.put("SELECT_PLAYER", connection.prepareStatement(
            "SELECT * FROM players WHERE uuid = ?"));
        
        preparedStatements.put("INSERT_PLAYER", connection.prepareStatement(
            "INSERT OR REPLACE INTO players (uuid, username, rank) VALUES (?, ?, ?)"));
        
        preparedStatements.put("UPDATE_PLAYER_USERNAME", connection.prepareStatement(
            "UPDATE players SET username = ?, updated_at = CURRENT_TIMESTAMP WHERE uuid = ?"));
        
        preparedStatements.put("UPDATE_PLAYER_COLOR", connection.prepareStatement(
            "UPDATE players SET current_name_color = ?, updated_at = CURRENT_TIMESTAMP WHERE uuid = ?"));
        
        preparedStatements.put("UPDATE_PLAYER_GRADIENT", connection.prepareStatement(
            "UPDATE players SET current_name_gradient = ?, updated_at = CURRENT_TIMESTAMP WHERE uuid = ?"));
        
        preparedStatements.put("UPDATE_PLAYER_PREFIX", connection.prepareStatement(
            "UPDATE players SET current_prefix_id = ?, updated_at = CURRENT_TIMESTAMP WHERE uuid = ?"));
        
        preparedStatements.put("UPDATE_PLAYER_NICKNAME", connection.prepareStatement(
            "UPDATE players SET current_nickname = ?, updated_at = CURRENT_TIMESTAMP WHERE uuid = ?"));
        
        preparedStatements.put("UPDATE_PLAYER_RANK", connection.prepareStatement(
            "UPDATE players SET rank = ?, updated_at = CURRENT_TIMESTAMP WHERE uuid = ?"));
        
        // Prefix queries
        preparedStatements.put("SELECT_AVAILABLE_PREFIXES", connection.prepareStatement(
            "SELECT * FROM available_prefixes WHERE player_uuid = ?"));
        
        preparedStatements.put("INSERT_AVAILABLE_PREFIX", connection.prepareStatement(
            "INSERT INTO available_prefixes (player_uuid, prefix_id, prefix_type, expires_at) VALUES (?, ?, ?, ?)"));
        
        preparedStatements.put("SELECT_CUSTOM_PREFIX_REQUESTS", connection.prepareStatement(
            "SELECT * FROM custom_prefix_requests WHERE status = 'pending' ORDER BY requested_at"));
        
        preparedStatements.put("INSERT_CUSTOM_PREFIX_REQUEST", connection.prepareStatement(
            "INSERT INTO custom_prefix_requests (player_uuid, requested_text, expires_at) VALUES (?, ?, ?)"));
        
        preparedStatements.put("UPDATE_CUSTOM_PREFIX_REQUEST", connection.prepareStatement(
            "UPDATE custom_prefix_requests SET status = ?, reviewed_by = ?, reviewed_at = CURRENT_TIMESTAMP, review_reason = ? WHERE id = ?"));
        
        // Analytics queries
        preparedStatements.put("INSERT_ANALYTICS", connection.prepareStatement(
            "INSERT INTO usage_analytics (player_uuid, action_type, old_value, new_value) VALUES (?, ?, ?, ?)"));
    }
    
    public CompletableFuture<PlayerData> getPlayerData(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PreparedStatement stmt = preparedStatements.get("SELECT_PLAYER");
                stmt.setString(1, uuid.toString());
                
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return new PlayerData(
                        uuid,
                        rs.getString("username"),
                        rs.getString("current_name_color"),
                        rs.getString("current_name_gradient"),
                        rs.getString("current_prefix_id"),
                        rs.getString("current_nickname"),
                        rs.getString("rank")
                    );
                }
                return null;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error getting player data", e);
                return null;
            }
        }, executor);
    }
    
    public CompletableFuture<Void> savePlayerData(PlayerData data) {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement stmt = preparedStatements.get("INSERT_PLAYER");
                stmt.setString(1, data.getUuid().toString());
                stmt.setString(2, data.getUsername());
                stmt.setString(3, data.getRank());
                stmt.executeUpdate();
                
                // Update other fields
                if (data.getCurrentNameColor() != null) {
                    updatePlayerColor(data.getUuid(), data.getCurrentNameColor());
                }
                if (data.getCurrentNameGradient() != null) {
                    updatePlayerGradient(data.getUuid(), data.getCurrentNameGradient());
                }
                if (data.getCurrentPrefixId() != null) {
                    updatePlayerPrefix(data.getUuid(), data.getCurrentPrefixId());
                }
                if (data.getCurrentNickname() != null) {
                    updatePlayerNickname(data.getUuid(), data.getCurrentNickname());
                }
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error saving player data", e);
            }
        }, executor);
    }
    
    public CompletableFuture<Void> updatePlayerUsername(UUID uuid, String username) {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement stmt = preparedStatements.get("UPDATE_PLAYER_USERNAME");
                stmt.setString(1, username);
                stmt.setString(2, uuid.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error updating player username", e);
            }
        }, executor);
    }
    
    public CompletableFuture<Void> updatePlayerColor(UUID uuid, String color) {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement stmt = preparedStatements.get("UPDATE_PLAYER_COLOR");
                stmt.setString(1, color);
                stmt.setString(2, uuid.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error updating player color", e);
            }
        }, executor);
    }
    
    public CompletableFuture<Void> updatePlayerGradient(UUID uuid, String gradient) {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement stmt = preparedStatements.get("UPDATE_PLAYER_GRADIENT");
                stmt.setString(1, gradient);
                stmt.setString(2, uuid.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error updating player gradient", e);
            }
        }, executor);
    }
    
    public CompletableFuture<Void> updatePlayerPrefix(UUID uuid, String prefixId) {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement stmt = preparedStatements.get("UPDATE_PLAYER_PREFIX");
                stmt.setString(1, prefixId);
                stmt.setString(2, uuid.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error updating player prefix", e);
            }
        }, executor);
    }
    
    public CompletableFuture<Void> updatePlayerNickname(UUID uuid, String nickname) {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement stmt = preparedStatements.get("UPDATE_PLAYER_NICKNAME");
                stmt.setString(1, nickname);
                stmt.setString(2, uuid.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error updating player nickname", e);
            }
        }, executor);
    }
    
    public CompletableFuture<Void> updatePlayerRank(UUID uuid, String rank) {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement stmt = preparedStatements.get("UPDATE_PLAYER_RANK");
                stmt.setString(1, rank);
                stmt.setString(2, uuid.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error updating player rank", e);
            }
        }, executor);
    }
    
    public CompletableFuture<Void> logAnalytics(UUID uuid, String actionType, String oldValue, String newValue) {
        return CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement stmt = preparedStatements.get("INSERT_ANALYTICS");
                stmt.setString(1, uuid.toString());
                stmt.setString(2, actionType);
                stmt.setString(3, oldValue);
                stmt.setString(4, newValue);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error logging analytics", e);
            }
        }, executor);
    }
    
    private void startBackupTask() {
        int backupInterval = plugin.getConfig().getInt("database.backup_interval", 24);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                performBackup();
            }
        }.runTaskTimerAsynchronously(plugin, 20L * 60 * 60, 20L * 60 * 60 * backupInterval);
    }
    
    private void performBackup() {
        try {
            String backupPath = plugin.getDataFolder() + "/backups/customization_backup_" + 
                System.currentTimeMillis() + ".db";
            
            File backupFile = new File(backupPath);
            backupFile.getParentFile().mkdirs();
            
            // Simple backup by copying the database file
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("BACKUP TO '" + backupPath + "'");
            }
            
            plugin.getLogger().info("Database backup created: " + backupPath);
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to create database backup", e);
        }
    }
    
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
            executor.shutdown();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error closing database connection", e);
        }
    }
}