package com.prefix27.customization.managers;

import com.prefix27.customization.PlayerCustomizationPlugin;
import com.prefix27.customization.database.PlayerData;
import net.luckperms.api.model.user.User;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class PlayerDataManager {
    
    private final PlayerCustomizationPlugin plugin;
    private final Map<UUID, PlayerData> playerDataCache;
    private final Map<UUID, Long> lastAccessed;
    
    public PlayerDataManager(PlayerCustomizationPlugin plugin) {
        this.plugin = plugin;
        this.playerDataCache = new ConcurrentHashMap<>();
        this.lastAccessed = new ConcurrentHashMap<>();
        
        // Start cache cleanup task
        startCacheCleanupTask();
    }
    
    public void loadPlayerData(UUID uuid) {
        // Always refresh from database for cross-server sync
        plugin.getDatabaseManager().getPlayerData(uuid).thenAccept(data -> {
            if (data == null) {
                // Create new player data
                String rank = getRankFromLuckPerms(uuid);
                String username = plugin.getServer().getPlayer(uuid).getName();
                data = new PlayerData(uuid, username, rank);
                
                // Automatically assign rank-based prefix
                assignDefaultPrefix(data);
                
                plugin.getDatabaseManager().savePlayerData(data);
                plugin.getLogger().info("Created new player data for " + username + " (" + uuid + ") with rank " + rank);
            } else {
                // Check if rank changed and update prefix accordingly
                String currentRank = getRankFromLuckPerms(uuid);
                if (!currentRank.equals(data.getRank())) {
                    data.setRank(currentRank);
                    // Rank changed - update prefix if they don't have a custom one
                    updateRankBasedPrefix(data);
                    plugin.getDatabaseManager().savePlayerData(data);
                    plugin.getLogger().info("Updated rank for " + data.getUsername() + " from " + data.getRank() + " to " + currentRank);
                }
                plugin.getLogger().info("Loaded player data for " + data.getUsername() + " (" + uuid + ") - Color: " + data.getCurrentNameColor() + ", Prefix: " + data.getCurrentPrefixId());
            }
            
            playerDataCache.put(uuid, data);
            lastAccessed.put(uuid, System.currentTimeMillis());
        });
    }
    
    public void refreshPlayerData(UUID uuid) {
        // Force refresh from database - useful for cross-server sync
        playerDataCache.remove(uuid);
        loadPlayerData(uuid);
    }
    
    public void savePlayerData(UUID uuid) {
        PlayerData data = playerDataCache.get(uuid);
        if (data != null) {
            plugin.getDatabaseManager().savePlayerData(data);
        }
    }
    
    public void saveAllData() {
        for (PlayerData data : playerDataCache.values()) {
            plugin.getDatabaseManager().savePlayerData(data);
        }
    }
    
    public void unloadPlayerData(UUID uuid) {
        playerDataCache.remove(uuid);
        lastAccessed.remove(uuid);
    }
    
    public PlayerData getPlayerData(UUID uuid) {
        PlayerData data = playerDataCache.get(uuid);
        if (data != null) {
            lastAccessed.put(uuid, System.currentTimeMillis());
        }
        return data;
    }
    
    public void updatePlayerUsername(UUID uuid, String username) {
        PlayerData data = getPlayerData(uuid);
        if (data != null) {
            data.setUsername(username);
            plugin.getDatabaseManager().updatePlayerUsername(uuid, username);
        }
    }
    
    public void setNameColor(UUID uuid, String color) {
        PlayerData data = getPlayerData(uuid);
        if (data != null) {
            String oldColor = data.getCurrentNameColor();
            data.setCurrentNameColor(color);
            data.setCurrentNameGradient(null); // Clear gradient when setting solid color
            
            plugin.getDatabaseManager().updatePlayerColor(uuid, color);
            plugin.getDatabaseManager().updatePlayerGradient(uuid, null);
            plugin.getDatabaseManager().logAnalytics(uuid, "color_change", oldColor, color);
        }
    }
    
    public void setNameGradient(UUID uuid, String gradient) {
        PlayerData data = getPlayerData(uuid);
        if (data != null) {
            String oldGradient = data.getCurrentNameGradient();
            data.setCurrentNameGradient(gradient);
            data.setCurrentNameColor(null); // Clear solid color when setting gradient
            
            plugin.getDatabaseManager().updatePlayerGradient(uuid, gradient);
            plugin.getDatabaseManager().updatePlayerColor(uuid, null);
            plugin.getDatabaseManager().logAnalytics(uuid, "gradient_change", oldGradient, gradient);
        }
    }
    
    public void setPrefix(UUID uuid, String prefixId) {
        PlayerData data = getPlayerData(uuid);
        if (data != null) {
            String oldPrefix = data.getCurrentPrefixId();
            data.setCurrentPrefixId(prefixId);
            
            plugin.getDatabaseManager().updatePlayerPrefix(uuid, prefixId);
            plugin.getDatabaseManager().logAnalytics(uuid, "prefix_change", oldPrefix, prefixId);
        }
    }
    
    public void setNickname(UUID uuid, String nickname) {
        PlayerData data = getPlayerData(uuid);
        if (data != null) {
            String oldNickname = data.getCurrentNickname();
            data.setCurrentNickname(nickname);
            
            plugin.getDatabaseManager().updatePlayerNickname(uuid, nickname);
            plugin.getDatabaseManager().logAnalytics(uuid, "nick_change", oldNickname, nickname);
        }
    }
    
    public void updatePlayerRank(UUID uuid, String rank) {
        PlayerData data = getPlayerData(uuid);
        if (data != null) {
            data.setRank(rank);
            plugin.getDatabaseManager().updatePlayerRank(uuid, rank);
        }
    }
    
    public void resetPlayerData(UUID uuid) {
        PlayerData data = getPlayerData(uuid);
        if (data != null) {
            data.setCurrentNameColor(null);
            data.setCurrentNameGradient(null);
            data.setCurrentPrefixId(null);
            data.setCurrentNickname(null);
            
            plugin.getDatabaseManager().updatePlayerColor(uuid, null);
            plugin.getDatabaseManager().updatePlayerGradient(uuid, null);
            plugin.getDatabaseManager().updatePlayerPrefix(uuid, null);
            plugin.getDatabaseManager().updatePlayerNickname(uuid, null);
            plugin.getDatabaseManager().logAnalytics(uuid, "reset", "all", "null");
        }
    }
    
    public String getRankFromLuckPerms(UUID uuid) {
        if (plugin.getLuckPermsIntegration() != null) {
            User user = plugin.getLuckPermsIntegration().getUser(uuid);
            if (user != null) {
                String primaryGroup = user.getPrimaryGroup();
                
                // Map LuckPerms groups to our rank system
                switch (primaryGroup.toLowerCase()) {
                    case "player":
                        return "player";
                    case "supporter":
                        return "supporter";
                    case "patron":
                        return "patron";
                    case "devoted":
                        return "devoted";
                    default:
                        return "player"; // Default to player rank instead of "default"
                }
            }
        }
        return "default";
    }
    
    public boolean hasPermission(UUID uuid, String permission) {
        if (plugin.getLuckPermsIntegration() != null) {
            return plugin.getLuckPermsIntegration().hasPermission(uuid, permission);
        }
        
        // Fallback to basic permission check
        org.bukkit.entity.Player player = plugin.getServer().getPlayer(uuid);
        return player != null && player.hasPermission(permission);
    }
    
    public boolean canUseColorFeature(UUID uuid) {
        return hasPermission(uuid, "customization.supporter") || 
               hasPermission(uuid, "customization.patron") || 
               hasPermission(uuid, "customization.devoted");
    }
    
    public boolean canUseGradientFeature(UUID uuid) {
        return hasPermission(uuid, "customization.patron") || 
               hasPermission(uuid, "customization.devoted");
    }
    
    public boolean canUseCustomPrefix(UUID uuid) {
        return hasPermission(uuid, "customization.devoted");
    }
    
    public boolean canUseNickname(UUID uuid) {
        return hasPermission(uuid, "customization.nick");
    }
    
    private void startCacheCleanupTask() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long maxAge = plugin.getConfig().getInt("cache.player_data_expire", 300) * 1000L;
            long currentTime = System.currentTimeMillis();
            
            lastAccessed.entrySet().removeIf(entry -> {
                if (currentTime - entry.getValue() > maxAge) {
                    UUID uuid = entry.getKey();
                    // Save data before removing from cache
                    savePlayerData(uuid);
                    playerDataCache.remove(uuid);
                    return true;
                }
                return false;
            });
            
        }, 20L * 60, 20L * 60); // Run every minute
    }
    
    private void assignDefaultPrefix(PlayerData data) {
        String rank = data.getRank();
        
        // Player rank gets no prefix, other ranks get their rank as prefix with default color
        switch (rank.toLowerCase()) {
            case "player":
                data.setCurrentPrefixId(null); // No prefix for player rank
                break;
            case "supporter":
                // Default to white supporter prefix
                data.setCurrentPrefixId("supporter_white");
                break;
            case "patron":
                // Default to white patron prefix  
                data.setCurrentPrefixId("patron_white");
                break;
            case "devoted":
                // Default to white devoted prefix
                data.setCurrentPrefixId("devoted_white");
                break;
            default:
                data.setCurrentPrefixId(null); // Default to no prefix
                break;
        }
    }
    
    private void updateRankBasedPrefix(PlayerData data) {
        // For now, always update prefix when rank changes
        // Later we could add logic to preserve custom prefixes
        assignDefaultPrefix(data);
        
        // Notify player about the change if they're online
        org.bukkit.entity.Player player = plugin.getServer().getPlayer(data.getUuid());
        if (player != null) {
            if (data.hasPrefix()) {
                player.sendMessage("§aYour rank changed! You now have the §e" + data.getRank() + "§a prefix.");
            } else {
                player.sendMessage("§aYour rank changed to §e" + data.getRank() + "§a.");
            }
        }
    }
    
    public void reload() {
        // Clear cache and reload configuration
        saveAllData();
        playerDataCache.clear();
        lastAccessed.clear();
    }
}