package com.prefix27.customization.integrations;

import com.prefix27.customization.PlayerCustomizationPlugin;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class LuckPermsIntegration {
    
    private final PlayerCustomizationPlugin plugin;
    private LuckPerms luckPerms;
    
    public LuckPermsIntegration(PlayerCustomizationPlugin plugin) {
        this.plugin = plugin;
    }
    
    public boolean initialize() {
        try {
            luckPerms = LuckPermsProvider.get();
            plugin.getLogger().info("LuckPerms integration initialized successfully!");
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize LuckPerms integration", e);
            return false;
        }
    }
    
    public User getUser(UUID uuid) {
        if (luckPerms == null) {
            return null;
        }
        
        try {
            return luckPerms.getUserManager().getUser(uuid);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to get user from LuckPerms", e);
            return null;
        }
    }
    
    public CompletableFuture<User> loadUser(UUID uuid) {
        if (luckPerms == null) {
            return CompletableFuture.completedFuture(null);
        }
        
        return luckPerms.getUserManager().loadUser(uuid);
    }
    
    public String getPrimaryGroup(UUID uuid) {
        User user = getUser(uuid);
        if (user != null) {
            return user.getPrimaryGroup();
        }
        return "default";
    }
    
    public boolean hasPermission(UUID uuid, String permission) {
        User user = getUser(uuid);
        if (user != null) {
            QueryOptions queryOptions = QueryOptions.defaultContextualOptions();
            return user.getCachedData().getPermissionData(queryOptions).checkPermission(permission).asBoolean();
        }
        return false;
    }
    
    public boolean isInGroup(UUID uuid, String group) {
        User user = getUser(uuid);
        if (user != null) {
            return user.getInheritedGroups(QueryOptions.defaultContextualOptions()).stream()
                    .anyMatch(g -> g.getName().equalsIgnoreCase(group));
        }
        return false;
    }
    
    public String getRankForCustomization(UUID uuid) {
        String primaryGroup = getPrimaryGroup(uuid).toLowerCase();
        
        // Map LuckPerms groups to our customization ranks
        switch (primaryGroup) {
            case "supporter":
                return "supporter";
            case "patron":
                return "patron";
            case "devoted":
                return "devoted";
            case "admin":
            case "moderator":
            case "staff":
                return "devoted"; // Staff get devoted permissions
            default:
                return "default";
        }
    }
    
    public boolean canUseColorFeatures(UUID uuid) {
        return hasPermission(uuid, "customization.supporter") ||
               hasPermission(uuid, "customization.patron") ||
               hasPermission(uuid, "customization.devoted") ||
               isInGroup(uuid, "supporter") ||
               isInGroup(uuid, "patron") ||
               isInGroup(uuid, "devoted");
    }
    
    public boolean canUseGradientFeatures(UUID uuid) {
        return hasPermission(uuid, "customization.patron") ||
               hasPermission(uuid, "customization.devoted") ||
               isInGroup(uuid, "patron") ||
               isInGroup(uuid, "devoted");
    }
    
    public boolean canUseCustomPrefixes(UUID uuid) {
        return hasPermission(uuid, "customization.devoted") ||
               isInGroup(uuid, "devoted");
    }
    
    public boolean canUseNicknames(UUID uuid) {
        return hasPermission(uuid, "customization.nick") ||
               hasPermission(uuid, "customization.supporter") ||
               hasPermission(uuid, "customization.patron") ||
               hasPermission(uuid, "customization.devoted");
    }
    
    public boolean isStaff(UUID uuid) {
        return hasPermission(uuid, "customization.admin") ||
               isInGroup(uuid, "admin") ||
               isInGroup(uuid, "moderator") ||
               isInGroup(uuid, "staff");
    }
    
    public void refreshUser(UUID uuid) {
        if (luckPerms != null) {
            luckPerms.getUserManager().cleanupUser(luckPerms.getUserManager().getUser(uuid));
        }
    }
}