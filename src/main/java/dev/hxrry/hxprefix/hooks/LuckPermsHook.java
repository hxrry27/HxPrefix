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
 * hook for luckperms integration
 */
public class LuckPermsHook {
    @SuppressWarnings("unused")
    private final HxPrefix plugin;
    private LuckPerms luckPerms;
    
    public LuckPermsHook(@NotNull HxPrefix plugin) {
        this.plugin = plugin;
    }
    
    /**
     * initialize the hook
     */
    public boolean init() {
        try {
            RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager()
                .getRegistration(LuckPerms.class);
            
            if (provider != null) {
                luckPerms = provider.getProvider();
                Log.info("hooked into luckperms v" + getVersion());
                return true;
            }
            
        } catch (Exception e) {
            Log.error("failed to hook into luckperms", e);
        }
        
        Log.warning("luckperms not found - rank features will be limited");
        return false;
    }
    
    /**
     * get a player's primary group
     */
    @NotNull
    public String getPrimaryGroup(@NotNull Player player) {
        return getPrimaryGroup(player.getUniqueId());
    }
    
    /**
     * get a player's primary group by uuid
     */
    @NotNull
    public String getPrimaryGroup(@NotNull UUID uuid) {
        if (luckPerms == null) {
            return "default";
        }
        
        try {
            User user = luckPerms.getUserManager().getUser(uuid);
            if (user == null) {
                // user not loaded, try to load
                user = loadUser(uuid);
                if (user == null) {
                    return "default";
                }
            }
            
            // get primary group
            String primaryGroup = user.getPrimaryGroup();
            
            // validate it exists
            Group group = luckPerms.getGroupManager().getGroup(primaryGroup);
            if (group != null) {
                return primaryGroup;
            }
            
            // fallback to first inherited group
            Collection<Group> inheritedGroups = user.getInheritedGroups(
                QueryOptions.contextual(luckPerms.getContextManager().getStaticContext())
            );
            
            if (!inheritedGroups.isEmpty()) {
                return inheritedGroups.iterator().next().getName();
            }
            
        } catch (Exception e) {
            Log.debug("failed to get primary group for " + uuid + ": " + e.getMessage());
        }
        
        return "default";
    }
    
    /**
     * get all groups for a player
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
            
            // get all inherited groups
            return user.getNodes().stream()
                .filter(node -> node instanceof InheritanceNode)
                .map(node -> ((InheritanceNode) node).getGroupName())
                .collect(java.util.stream.Collectors.toList());
            
        } catch (Exception e) {
            Log.debug("failed to get groups for " + player.getName() + ": " + e.getMessage());
        }
        
        return java.util.List.of("default");
    }
    
    /**
     * check if a player has a specific group
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
            
            // check for group node
            return user.getNodes().stream()
                .filter(node -> node instanceof InheritanceNode)
                .map(node -> ((InheritanceNode) node).getGroupName())
                .anyMatch(group -> group.equalsIgnoreCase(groupName));
            
        } catch (Exception e) {
            Log.debug("failed to check group for " + player.getName() + ": " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * get display name of a group
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
            
            // check for display name meta
            String displayName = group.getCachedData().getMetaData()
                .getMetaValue("display-name");
            
            if (displayName != null) {
                return displayName;
            }
            
            // check for prefix as fallback
            String prefix = group.getCachedData().getMetaData().getPrefix();
            if (prefix != null) {
                // strip colour codes for clean display
                return stripColours(prefix);
            }
            
        } catch (Exception e) {
            Log.debug("failed to get group display name: " + e.getMessage());
        }
        
        // return the group name itself
        return groupName;
    }
    
    /**
     * get group weight (for sorting)
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
            
            // get weight from group
            return group.getWeight().orElse(0);
            
        } catch (Exception e) {
            Log.debug("failed to get group weight: " + e.getMessage());
        }
        
        return 0;
    }
    
    /**
     * reload user data
     */
    public CompletableFuture<Void> reloadUser(@NotNull UUID uuid) {
        if (luckPerms == null) {
            return CompletableFuture.completedFuture(null);
        }
        
        return luckPerms.getUserManager().loadUser(uuid)
            .thenAccept(user -> {
                Log.debug("reloaded luckperms data for " + uuid);
            });
    }
    
    /**
     * load user synchronously
     */
    @Nullable
    private User loadUser(@NotNull UUID uuid) {
        try {
            CompletableFuture<User> future = luckPerms.getUserManager().loadUser(uuid);
            return future.join(); // block until loaded
        } catch (Exception e) {
            Log.debug("failed to load user " + uuid + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * get luckperms version
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
     * check if luckperms is available
     */
    public boolean isAvailable() {
        return luckPerms != null;
    }
    
    /**
     * strip colour codes from text
     */
    private String stripColours(@NotNull String input) {
        // strip minimessage tags
        String stripped = input.replaceAll("<[^>]+>", "");
        // strip legacy codes
        stripped = stripped.replaceAll("&[0-9a-fk-or]", "");
        stripped = stripped.replaceAll("§[0-9a-fk-or]", "");
        return stripped.trim();
    }
}