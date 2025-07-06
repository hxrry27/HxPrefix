package com.yourserver.playercustomisation.database;

import com.yourserver.playercustomisation.PlayerCustomisation;
import com.yourserver.playercustomisation.models.PlayerData;
import com.yourserver.playercustomisation.models.TagRequest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class PlayerDataManager {
    private final PlayerCustomisation plugin;
    private final MySQL mysql;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cacheTimestamps = new ConcurrentHashMap<>();
    
    // 30 second cache TTL as specified
    private static final long CACHE_TTL = 30000;

    public PlayerDataManager(PlayerCustomisation plugin, MySQL mysql) {
        this.plugin = plugin;
        this.mysql = mysql;
    }

    public CompletableFuture<PlayerData> getPlayerData(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            // Check cache first
            PlayerData cached = cache.get(uuid);
            Long timestamp = cacheTimestamps.get(uuid);
            
            if (cached != null && timestamp != null && 
                (System.currentTimeMillis() - timestamp) < CACHE_TTL) {
                return cached;
            }

            // Load from database
            try (Connection connection = mysql.getConnection()) {
                String sql = "SELECT * FROM player_data WHERE uuid = ?";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, uuid.toString());
                    try (ResultSet rs = stmt.executeQuery()) {
                        PlayerData data;
                        if (rs.next()) {
                            data = new PlayerData(uuid, rs.getString("username"));
                            data.setNickname(rs.getString("nickname"));
                            data.setNameColor(rs.getString("name_color"));
                            data.setPrefixStyle(rs.getString("prefix_style"));
                            data.setCustomPrefix(rs.getString("custom_prefix"));
                            data.setSuffix(rs.getString("suffix"));
                        } else {
                            // Player doesn't exist in database yet
                            return null;
                        }
                        
                        // Update cache
                        cache.put(uuid, data);
                        cacheTimestamps.put(uuid, System.currentTimeMillis());
                        
                        return data;
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load player data for " + uuid, e);
                return cached; // Return cached data if database fails
            }
        });
    }

    public CompletableFuture<Void> savePlayerData(PlayerData data) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = mysql.getConnection()) {
                String sql = """
                    INSERT INTO player_data (uuid, username, nickname, name_color, prefix_style, custom_prefix, suffix)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                    username = VALUES(username),
                    nickname = VALUES(nickname),
                    name_color = VALUES(name_color),
                    prefix_style = VALUES(prefix_style),
                    custom_prefix = VALUES(custom_prefix),
                    suffix = VALUES(suffix)
                    """;
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, data.getUuid().toString());
                    stmt.setString(2, data.getUsername());
                    stmt.setString(3, data.getNickname());
                    stmt.setString(4, data.getNameColor());
                    stmt.setString(5, data.getPrefixStyle());
                    stmt.setString(6, data.getCustomPrefix());
                    stmt.setString(7, data.getSuffix());
                    stmt.executeUpdate();
                }
                
                // Update cache
                cache.put(data.getUuid(), data);
                cacheTimestamps.put(data.getUuid(), System.currentTimeMillis());
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save player data for " + data.getUuid(), e);
            }
        });
    }

    public CompletableFuture<Void> createTagRequest(TagRequest request) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = mysql.getConnection()) {
                String sql = "INSERT INTO custom_tag_requests (uuid, username, requested_tag) VALUES (?, ?, ?)";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, request.getUuid().toString());
                    stmt.setString(2, request.getUsername());
                    stmt.setString(3, request.getRequestedTag());
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create tag request", e);
            }
        });
    }

    public CompletableFuture<List<TagRequest>> getPendingTagRequests() {
        return CompletableFuture.supplyAsync(() -> {
            List<TagRequest> requests = new ArrayList<>();
            try (Connection connection = mysql.getConnection()) {
                String sql = "SELECT * FROM custom_tag_requests WHERE status = 'pending' ORDER BY requested_at";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            TagRequest request = new TagRequest(
                                UUID.fromString(rs.getString("uuid")),
                                rs.getString("username"),
                                rs.getString("requested_tag")
                            );
                            request.setId(rs.getInt("id"));
                            request.setStatus(rs.getString("status"));
                            requests.add(request);
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load pending tag requests", e);
            }
            return requests;
        });
    }

    public CompletableFuture<Void> updateTagRequest(int requestId, String status, UUID reviewedBy) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = mysql.getConnection()) {
                String sql = "UPDATE custom_tag_requests SET status = ?, reviewed_by = ?, reviewed_at = NOW() WHERE id = ?";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, status);
                    stmt.setString(2, reviewedBy != null ? reviewedBy.toString() : null);
                    stmt.setInt(3, requestId);
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to update tag request", e);
            }
        });
    }

    public void clearCache() {
        cache.clear();
        cacheTimestamps.clear();
    }

    public void removeFromCache(UUID uuid) {
        cache.remove(uuid);
        cacheTimestamps.remove(uuid);
    }
}