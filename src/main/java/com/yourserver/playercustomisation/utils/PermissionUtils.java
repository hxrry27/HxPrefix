package com.yourserver.playercustomisation.utils;

import com.yourserver.playercustomisation.PlayerCustomisation;
import com.yourserver.playercustomisation.config.ConfigManager;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.entity.Player;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class PermissionUtils {
    private static final Map<String, String> rankCache = new ConcurrentHashMap<>();
    private static final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private static PlayerCustomisation plugin;
    private static long CACHE_TTL = 5000; // Default 5 seconds, loaded from config

    // Initialize with plugin instance
    public static void init(PlayerCustomisation instance) {
        plugin = instance;
        CACHE_TTL = plugin.getConfig().getLong("cache.permission-ttl", 5000);
    }

    public static String getPlayerRank(Player player) {
        String key = player.getUniqueId().toString();
        String cached = rankCache.get(key);
        Long timestamp = cacheTimestamps.get(key);
        
        // Check cache first
        if (cached != null && timestamp != null && 
            (System.currentTimeMillis() - timestamp) < CACHE_TTL) {
            return cached;
        }

        try {
            LuckPerms api = LuckPermsProvider.get();
            User user = api.getPlayerAdapter(Player.class).getUser(player);
            String rank = user.getPrimaryGroup();
            
            // Update cache
            rankCache.put(key, rank);
            cacheTimestamps.put(key, System.currentTimeMillis());
            
            return rank;
        } catch (Exception e) {
            // Get fallback rank from config
            return plugin.getConfig().getString("defaults.fallback-rank", "default");
        }
    }

    // Delegate all permission checks to ConfigManager
    public static boolean hasColorAccess(String rank) {
        if (plugin == null) return false;
        return plugin.getConfigManager().canUseColors(rank);
    }

    public static boolean hasPrefixAccess(String rank) {
        if (plugin == null) return false;
        return plugin.getConfigManager().canUsePrefix(rank);
    }

    public static boolean hasNickAccess(String rank) {
        if (plugin == null) return false;
        return plugin.getConfigManager().canUseNickname(rank);
    }

    public static boolean hasCustomTagAccess(String rank) {
        if (plugin == null) return false;
        return plugin.getConfigManager().canUseCustomTags(rank);
    }

    public static boolean hasGradientAccess(String rank) {
        if (plugin == null) return false;
        return plugin.getConfigManager().canUseGradients(rank);
    }

    public static boolean hasSuffixAccess(String rank) {
        if (plugin == null) return false;
        return plugin.getConfigManager().canUseSuffix(rank);
    }

    public static void clearCache() {
        rankCache.clear();
        cacheTimestamps.clear();
    }

    public static void reloadConfig() {
        if (plugin != null) {
            CACHE_TTL = plugin.getConfig().getLong("cache.permission-ttl", 5000);
        }
    }
}