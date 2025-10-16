package dev.hxrry.hxprefix.hooks;

import dev.hxrry.hxcore.utils.Log;

import dev.hxrry.hxprefix.HxPrefix;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;

import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.query.QueryOptions;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Hook for LuckPerms integration
 * 
 * IMPORTANT: Call init() after construction to initialize the LuckPerms API!
 */
public class LuckPermsHook {
    @SuppressWarnings("unused")
    private final HxPrefix plugin;
    private LuckPerms luckPerms;
    
    public LuckPermsHook(@NotNull HxPrefix plugin) {
        this.plugin = plugin;
        // NOTE: init() must be called after construction!
        // We don't call it here to allow proper error handling in setupHooks()
    }
    
    /**
     * Initialize the LuckPerms hook
     * 
     * @return true if successfully hooked into LuckPerms, false otherwise
     */
    public boolean init() {
        try {
            RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager()
                .getRegistration(LuckPerms.class);
            
            if (provider != null) {
                luckPerms = provider.getProvider();
                Log.info("Hooked into LuckPerms v" + getVersion());
                return true;
            }
            
        } catch (Exception e) {
            Log.error("Failed to hook into LuckPerms", e);
        }
        
        Log.warning("LuckPerms not found - rank features will be limited");
        return false;
    }
    
    /**
     * Get a player's primary group
     */
    @NotNull
    public String getPrimaryGroup(@NotNull Player player) {
        return getPrimaryGroup(player.getUniqueId());
    }
    
    /**
     * Get a player's primary group by UUID
     */
    @NotNull
    public String getPrimaryGroup(@NotNull UUID uuid) {
        if (luckPerms == null) {
            return "default";
        }
        
        try {
            User user = luckPerms.getUserManager().getUser(uuid);
            if (user == null) {
                // User not loaded, try to load
                user = loadUser(uuid);
                if (user == null) {
                    return "default";
                }
            }
            
            // Get primary group
            String primaryGroup = user.getPrimaryGroup();
            
            // Validate it exists
            Group group = luckPerms.getGroupManager().getGroup(primaryGroup);
            if (group != null) {
                return primaryGroup;
            }
            
            // Fallback to first inherited group
            Collection<Group> inheritedGroups = user.getInheritedGroups(
                QueryOptions.contextual(luckPerms.getContextManager().getStaticContext())
            );
            
            if (!inheritedGroups.isEmpty()) {
                return inheritedGroups.iterator().next().getName();
            }
            
        } catch (Exception e) {
            Log.debug("Failed to get primary group for " + uuid + ": " + e.getMessage());
        }
        
        return "default";
    }
    
    /**
     * Get all groups for a player
     */
    @NotNull
    public Collection<String> getGroups(@NotNull Player player) {
        if (luckPerms == null) {
            return java.util.List.of("default");
        }
        
        try {
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user == null) {
                return java.util.List.of("default");
            }
            
            // Get all inherited groups
            return user.getNodes().stream()
                .filter(node -> node instanceof InheritanceNode)
                .map(node -> ((InheritanceNode) node).getGroupName())
                .collect(java.util.stream.Collectors.toList());
            
        } catch (Exception e) {
            Log.debug("Failed to get groups for " + player.getName() + ": " + e.getMessage());
        }
        
        return java.util.List.of("default");
    }
    
    /**
     * Check if a player has a specific group
     */
    public boolean hasGroup(@NotNull Player player, @NotNull String groupName) {
        if (luckPerms == null) {
            return false;
        }
        
        try {
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user == null) {
                return false;
            }
            
            // Check for group node
            return user.getNodes().stream()
                .filter(node -> node instanceof InheritanceNode)
                .map(node -> ((InheritanceNode) node).getGroupName())
                .anyMatch(group -> group.equalsIgnoreCase(groupName));
            
        } catch (Exception e) {
            Log.debug("Failed to check group for " + player.getName() + ": " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Get display name of a group
     */
    @Nullable
    public String getGroupDisplayName(@NotNull String groupName) {
        if (luckPerms == null) {
            return null;
        }
        
        try {
            Group group = luckPerms.getGroupManager().getGroup(groupName);
            if (group == null) {
                return null;
            }
            
            // Check for display name meta
            String displayName = group.getCachedData().getMetaData()
                .getMetaValue("display-name");
            
            if (displayName != null) {
                return displayName;
            }
            
            // Check for prefix as fallback
            String prefix = group.getCachedData().getMetaData().getPrefix();
            if (prefix != null) {
                // Strip colour codes for clean display
                return stripColours(prefix);
            }
            
        } catch (Exception e) {
            Log.debug("Failed to get group display name: " + e.getMessage());
        }
        
        // Return the group name itself
        return groupName;
    }
    
    /**
     * Get group weight (for sorting)
     */
    public int getGroupWeight(@NotNull String groupName) {
        if (luckPerms == null) {
            return 0;
        }
        
        try {
            Group group = luckPerms.getGroupManager().getGroup(groupName);
            if (group == null) {
                return 0;
            }
            
            // Get weight from group
            return group.getWeight().orElse(0);
            
        } catch (Exception e) {
            Log.debug("Failed to get group weight: " + e.getMessage());
        }
        
        return 0;
    }
    
    /**
     * Reload user data
     */
    public CompletableFuture<Void> reloadUser(@NotNull UUID uuid) {
        if (luckPerms == null) {
            return CompletableFuture.completedFuture(null);
        }
        
        return luckPerms.getUserManager().loadUser(uuid)
            .thenAccept(user -> {
                Log.debug("Reloaded LuckPerms data for " + uuid);
            });
    }
    
    /**
     * Load user synchronously
     */
    @Nullable
    private User loadUser(@NotNull UUID uuid) {
        try {
            CompletableFuture<User> future = luckPerms.getUserManager().loadUser(uuid);
            return future.join(); // Block until loaded
        } catch (Exception e) {
            Log.debug("Failed to load user " + uuid + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get LuckPerms version
     */
    @SuppressWarnings("deprecation")
    @NotNull
    public String getVersion() {
        if (luckPerms == null) {
            return "unknown";
        }
        
        try {
            var plugin = Bukkit.getPluginManager().getPlugin("LuckPerms");
            if (plugin != null) {
                return plugin.getDescription().getVersion();
            }
        } catch (Exception ignored) {}
        
        return "unknown";
    }
    
    /**
     * Check if LuckPerms is available
     */
    public boolean isAvailable() {
        return luckPerms != null;
    }
    
    /**
     * Strip colour codes from text
     */
    private String stripColours(@NotNull String input) {
        // Strip minimessage tags
        String stripped = input.replaceAll("<[^>]+>", "");
        // Strip legacy codes
        stripped = stripped.replaceAll("&[0-9a-fk-or]", "");
        stripped = stripped.replaceAll("§[0-9a-fk-or]", "");
        return stripped.trim();
    }
}