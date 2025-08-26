package dev.hxrry.hxprefix.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import dev.hxrry.hxcore.utils.Log;

import dev.hxrry.hxprefix.HxPrefix;
import dev.hxrry.hxprefix.api.models.CustomTagRequest;
import dev.hxrry.hxprefix.api.models.PlayerCustomization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
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
                    last_updated INTEGER NOT NULL
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
        // migration system would go here
        // for now, just log
        Log.debug("checking for database migrations...");
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
                        rs.getLong("last_updated")
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
            INSERT INTO %s (uuid, username, nickname, name_colour, prefix, suffix, custom_tag_request, last_updated)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                username = VALUES(username),
                nickname = VALUES(nickname),
                name_colour = VALUES(name_colour),
                prefix = VALUES(prefix),
                suffix = VALUES(suffix),
                custom_tag_request = VALUES(custom_tag_request),
                last_updated = VALUES(last_updated)
            """.formatted(PLAYERS_TABLE) :
            """
            INSERT OR REPLACE INTO %s 
            (uuid, username, nickname, name_colour, prefix, suffix, custom_tag_request, last_updated)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
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
            
            stmt.executeUpdate();
            return true;
            
        } catch (SQLException e) {
            Log.error("failed to save player data for " + data.getUuid(), e);
            return false;
        }
    }
    
    // tag request operations
    
    /**
     * create a new tag request
     */
    public boolean createTagRequest(@NotNull CustomTagRequest request) {
        String sql = """
            INSERT INTO %s (player_uuid, player_name, requested_tag, status, requested_at)
            VALUES (?, ?, ?, ?, ?)
            """.formatted(TAGS_TABLE);
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, request.getPlayerUuid().toString());
            stmt.setString(2, request.getPlayerName());
            stmt.setString(3, request.getRequestedTag());
            stmt.setString(4, request.getStatus().getValue());
            stmt.setLong(5, request.getRequestedAt());
            
            stmt.executeUpdate();
            
            // get generated id
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    request.setId(rs.getInt(1));
                }
            }
            
            return true;
            
        } catch (SQLException e) {
            Log.error("failed to create tag request", e);
            return false;
        }
    }
    
    /**
     * get pending tag requests
     */
    @NotNull
    public List<CustomTagRequest> getPendingTagRequests() {
        List<CustomTagRequest> requests = new ArrayList<>();
        String sql = "SELECT * FROM " + TAGS_TABLE + " WHERE status = 'pending' ORDER BY requested_at ASC";
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                requests.add(mapTagRequest(rs));
            }
            
        } catch (SQLException e) {
            Log.error("failed to load pending tag requests", e);
        }
        
        return requests;
    }
    
    /**
     * get a specific tag request
     */
    @Nullable
    public CustomTagRequest getTagRequest(int id) {
        String sql = "SELECT * FROM " + TAGS_TABLE + " WHERE id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapTagRequest(rs);
                }
            }
            
        } catch (SQLException e) {
            Log.error("failed to load tag request " + id, e);
        }
        
        return null;
    }
    
    /**
     * get pending tag request for a player
     */
    @Nullable
    public CustomTagRequest getPendingTagRequest(@NotNull UUID playerUuid) {
        String sql = "SELECT * FROM " + TAGS_TABLE + " WHERE player_uuid = ? AND status = 'pending'";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, playerUuid.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapTagRequest(rs);
                }
            }
            
        } catch (SQLException e) {
            Log.error("failed to load pending tag request for " + playerUuid, e);
        }
        
        return null;
    }
    
    /**
     * get last denied request for a player
     */
    @Nullable
    public CustomTagRequest getLastDeniedRequest(@NotNull UUID playerUuid) {
        String sql = "SELECT * FROM " + TAGS_TABLE + 
                    " WHERE player_uuid = ? AND status = 'denied'" +
                    " ORDER BY reviewed_at DESC LIMIT 1";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, playerUuid.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapTagRequest(rs);
                }
            }
            
        } catch (SQLException e) {
            Log.error("failed to load denied tag request for " + playerUuid, e);
        }
        
        return null;
    }
    
    /**
     * update a tag request
     */
    public boolean updateTagRequest(@NotNull CustomTagRequest request) {
        String sql = """
            UPDATE %s SET status = ?, reviewed_by = ?, reviewer_name = ?, 
            deny_reason = ?, reviewed_at = ? WHERE id = ?
            """.formatted(TAGS_TABLE);
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, request.getStatus().getValue());
            stmt.setString(2, request.getReviewedBy() != null ? request.getReviewedBy().toString() : null);
            stmt.setString(3, request.getReviewerName());
            stmt.setString(4, request.getDenyReason());
            stmt.setLong(5, request.getReviewedAt());
            stmt.setInt(6, request.getId());
            
            stmt.executeUpdate();
            return true;
            
        } catch (SQLException e) {
            Log.error("failed to update tag request " + request.getId(), e);
            return false;
        }
    }
    
    /**
     * cancel a tag request
     */
    public boolean cancelTagRequest(int id) {
        String sql = "DELETE FROM " + TAGS_TABLE + " WHERE id = ? AND status = 'pending'";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            Log.error("failed to cancel tag request " + id, e);
            return false;
        }
    }
    
    /**
     * map result set to tag request
     */
    private CustomTagRequest mapTagRequest(@NotNull ResultSet rs) throws SQLException {
        String reviewedByStr = rs.getString("reviewed_by");
        UUID reviewedBy = reviewedByStr != null ? UUID.fromString(reviewedByStr) : null;
        
        return new CustomTagRequest(
            rs.getInt("id"),
            UUID.fromString(rs.getString("player_uuid")),
            rs.getString("player_name"),
            rs.getString("requested_tag"),
            CustomTagRequest.Status.fromString(rs.getString("status")),
            reviewedBy,
            rs.getString("reviewer_name"),
            rs.getString("deny_reason"),
            rs.getLong("requested_at"),
            rs.getLong("reviewed_at")
        );
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