package com.yourserver.playercustomisation.utils;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.entity.Player;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class PermissionUtils {
    private static final Map<String, String> rankCache = new ConcurrentHashMap<>();
    private static final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private static final long CACHE_TTL = 5000; // 5 seconds as specified

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
            return "player"; // Default fallback
        }
    }

    public static boolean hasColorAccess(String rank) {
        return rank.equals("supporter") || rank.equals("patron") || rank.equals("devoted");
    }

    public static boolean hasPrefixAccess(String rank) {
        return rank.equals("supporter") || rank.equals("patron") || rank.equals("devoted");
    }

    public static boolean hasNickAccess(String rank) {
        return rank.equals("supporter") || rank.equals("patron") || rank.equals("devoted");
    }

    public static boolean hasCustomTagAccess(String rank) {
        return rank.equals("devoted");
    }

    public static boolean hasGradientAccess(String rank) {
        return rank.equals("patron") || rank.equals("devoted");
    }

    public static void clearCache() {
        rankCache.clear();
        cacheTimestamps.clear();
    }
}