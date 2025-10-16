package dev.hxrry.hxprefix.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import dev.hxrry.hxcore.utils.Log;

import dev.hxrry.hxprefix.HxPrefix;
import dev.hxrry.hxprefix.api.models.PlayerCustomization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.*;
import java.util.UUID;

/**
 * handles all database operations
 */
public class DatabaseManager {
    private final HxPrefix plugin;
    private HikariDataSource dataSource;
    private final boolean useMySQL;
    
    // table names
    private static final String PLAYERS_TABLE = "hxprefix_players";
    private static final String TAGS_TABLE = "hxprefix_tags";
    
    // schema version for migrations
    private static final int CURRENT_SCHEMA_VERSION = 1;
    
    public DatabaseManager(@NotNull HxPrefix plugin) {
        this.plugin = plugin;
        this.useMySQL = plugin.getConfigManager().getDatabaseType().equalsIgnoreCase("mysql");
    }
    
    /**
     * initialize database connection
     */
    public boolean initialize() {
        try {
            setupDataSource();
            createTables();
            runMigrations();
            
            Log.info("database initialized successfully (" + (useMySQL ? "mysql" : "sqlite") + ")");
            return true;
            
        } catch (Exception e) {
            Log.error("failed to initialize database", e);
            return false;
        }
    }
    
    /**
     * setup hikari data source
     */
    private void setupDataSource() throws Exception {
        HikariConfig config = new HikariConfig();
        
        if (useMySQL) {
            // mysql configuration
            String host = plugin.getConfig().getString("database.mysql.host", "localhost");
            int port = plugin.getConfig().getInt("database.mysql.port", 3306);
            String database = plugin.getConfig().getString("database.mysql.database", "hxprefix");
            String username = plugin.getConfig().getString("database.mysql.username", "root");
            String password = plugin.getConfig().getString("database.mysql.password", "");
            
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            
            // mysql optimizations
            config.addDataSourceProperty("useSSL", "false");
            config.addDataSourceProperty("allowPublicKeyRetrieval", "true");
            config.addDataSourceProperty("serverTimezone", "UTC");
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            
        } else {
            // sqlite configuration
            File dbFile = new File(plugin.getDataFolder(), "data.db");
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
        }
        
        // general hikari settings
        config.setMaximumPoolSize(plugin.getConfig().getInt("database.pool-size", 10));
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setConnectionTestQuery("SELECT 1");
        
        dataSource = new HikariDataSource(config);
        
        // test connection
        try (Connection conn = dataSource.getConnection()) {
            Log.debug("database connection test successful");
        }
    }
    
    /**
     * create database tables
     */
    private void createTables() throws SQLException {
        try (Connection conn = getConnection()) {
            // players table
            String playersTable = useMySQL ? 
                """
                CREATE TABLE IF NOT EXISTS %s (
                    uuid VARCHAR(36) PRIMARY KEY,
                    username VARCHAR(16) NOT NULL,
                    nickname VARCHAR(16),
                    name_colour VARCHAR(100),
                    prefix VARCHAR(100),
                    suffix VARCHAR(50),
                    custom_tag_request VARCHAR(100),
                    last_updated BIGINT NOT NULL,
                    last_nickname_change BIGINT DEFAULT 0,
                    INDEX idx_username (username),
                    INDEX idx_updated (last_updated)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """.formatted(PLAYERS_TABLE) :
                """
                CREATE TABLE IF NOT EXISTS %s (
                    uuid TEXT PRIMARY KEY,
                    username TEXT NOT NULL,
                    nickname TEXT,
                    name_colour TEXT,
                    prefix TEXT,
                    suffix TEXT,
                    custom_tag_request TEXT,
                    last_updated INTEGER NOT NULL,
                    last_nickname_change INTEGER DEFAULT 0
                )
                """.formatted(PLAYERS_TABLE);
            
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(playersTable);
            }
            
            // tags table
            String tagsTable = useMySQL ?
                """
                CREATE TABLE IF NOT EXISTS %s (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    player_uuid VARCHAR(36) NOT NULL,
                    player_name VARCHAR(16) NOT NULL,
                    requested_tag VARCHAR(100) NOT NULL,
                    status VARCHAR(20) DEFAULT 'pending',
                    reviewed_by VARCHAR(36),
                    reviewer_name VARCHAR(16),
                    deny_reason TEXT,
                    requested_at BIGINT NOT NULL,
                    reviewed_at BIGINT,
                    INDEX idx_player (player_uuid),
                    INDEX idx_status (status),
                    INDEX idx_requested (requested_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """.formatted(TAGS_TABLE) :
                """
                CREATE TABLE IF NOT EXISTS %s (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid TEXT NOT NULL,
                    player_name TEXT NOT NULL,
                    requested_tag TEXT NOT NULL,
                    status TEXT DEFAULT 'pending',
                    reviewed_by TEXT,
                    reviewer_name TEXT,
                    deny_reason TEXT,
                    requested_at INTEGER NOT NULL,
                    reviewed_at INTEGER
                )
                """.formatted(TAGS_TABLE);
            
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(tagsTable);
            }
            
            // create indices for sqlite
            if (!useMySQL) {
                conn.createStatement().execute(
                    "CREATE INDEX IF NOT EXISTS idx_username ON " + PLAYERS_TABLE + "(username)"
                );
                conn.createStatement().execute(
                    "CREATE INDEX IF NOT EXISTS idx_player ON " + TAGS_TABLE + "(player_uuid)"
                );
                conn.createStatement().execute(
                    "CREATE INDEX IF NOT EXISTS idx_status ON " + TAGS_TABLE + "(status)"
                );
            }
        }
    }
    
    /**
     * run database migrations
     */
    private void runMigrations() {
        Log.debug("Checking for database migrations...");
        
        try (Connection conn = getConnection()) {
            // Check if last_nickname_change column exists
            boolean hasColumn = checkColumnExists(conn, PLAYERS_TABLE, "last_nickname_change");
            
            if (!hasColumn) {
                Log.info("Running migration: Adding last_nickname_change column");
                addNicknameCooldownColumn(conn);
                Log.info("Migration completed successfully");
            }
            
        } catch (SQLException e) {
            Log.error("Failed to run migrations", e);
        }
    }
    
    /**
     * Check if a column exists in a table
     */
    private boolean checkColumnExists(Connection conn, String tableName, String columnName) throws SQLException {
        if (useMySQL) {
            String sql = "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, tableName);
                stmt.setString(2, columnName);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } else {
            // SQLite
            String sql = "PRAGMA table_info(" + tableName + ")";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    if (columnName.equals(rs.getString("name"))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Add last_nickname_change column to existing tables
     */
    private void addNicknameCooldownColumn(Connection conn) throws SQLException {
        String sql = useMySQL ?
            "ALTER TABLE " + PLAYERS_TABLE + " ADD COLUMN last_nickname_change BIGINT DEFAULT 0" :
            "ALTER TABLE " + PLAYERS_TABLE + " ADD COLUMN last_nickname_change INTEGER DEFAULT 0";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }
    
    /**
     * get database connection
     */
    @NotNull
    private Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("database connection not available");
        }
        return dataSource.getConnection();
    }
    
    // player data operations
    
    /**
     * load player data from database
     */
    @Nullable
    public PlayerCustomization loadPlayerData(@NotNull UUID uuid) {
        String sql = "SELECT * FROM " + PLAYERS_TABLE + " WHERE uuid = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, uuid.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new PlayerCustomization(
                        uuid,
                        rs.getString("username"),
                        rs.getString("nickname"),
                        rs.getString("name_colour"),
                        rs.getString("prefix"),
                        rs.getString("suffix"),
                        rs.getString("custom_tag_request"),
                        rs.getLong("last_updated"),
                        rs.getLong("last_nickname_change")
                    );
                }
            }
            
        } catch (SQLException e) {
            Log.error("failed to load player data for " + uuid, e);
        }
        
        return null;
    }
    
    /**
     * save player data to database
     */
    public boolean savePlayerData(@NotNull PlayerCustomization data) {
        String sql = useMySQL ?
            """
            INSERT INTO %s (uuid, username, nickname, name_colour, prefix, suffix, custom_tag_request, last_updated, last_nickname_change)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                username = VALUES(username),
                nickname = VALUES(nickname),
                name_colour = VALUES(name_colour),
                prefix = VALUES(prefix),
                suffix = VALUES(suffix),
                custom_tag_request = VALUES(custom_tag_request),
                last_updated = VALUES(last_updated),
                last_nickname_change = VALUES(last_nickname_change)
            """.formatted(PLAYERS_TABLE) :
            """
            INSERT OR REPLACE INTO %s 
            (uuid, username, nickname, name_colour, prefix, suffix, custom_tag_request, last_updated, last_nickname_change)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.formatted(PLAYERS_TABLE);
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, data.getUuid().toString());
            stmt.setString(2, data.getUsername());
            stmt.setString(3, data.getNickname());
            stmt.setString(4, data.getNameColour());
            stmt.setString(5, data.getPrefix());
            stmt.setString(6, data.getSuffix());
            stmt.setString(7, data.getCustomTagRequest());
            stmt.setLong(8, data.getLastUpdated());
            stmt.setLong(9, data.getLastNicknameChange());
            
            stmt.executeUpdate();
            return true;
            
        } catch (SQLException e) {
            Log.error("failed to save player data for " + data.getUuid(), e);
            return false;
        }
    }

    /**
     * close database connection
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            Log.info("database connection closed");
        }
    }
}