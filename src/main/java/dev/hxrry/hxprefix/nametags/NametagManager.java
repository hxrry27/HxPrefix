package dev.hxrry.hxprefix.nametags;

import dev.hxrry.hxcore.utils.Log;

import dev.hxrry.hxprefix.HxPrefix;
import dev.hxrry.hxprefix.api.models.PlayerCustomization;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TODO:
 * WOULD manage player nametags above their heads
 * 
 * this class remains in the codebase for potential future use if we decide
 * to implement native nametag handling instead of relying on TAB.
 * 
 * @author hxrry
 * @since 0.1.0
 * primarily here because i have a memory like a sieve
 */
public class NametagManager {
    private final HxPrefix plugin;
    
    // player tracking (kept for potential future use)
    private final Map<UUID, NametagData> playerTags = new ConcurrentHashMap<>();
    
    // configuration
    private final boolean enabled;
    private final boolean useTabIntegration;
    
    public NametagManager(@NotNull HxPrefix plugin) {
        this.plugin = plugin;
        
        // check configuration
        this.enabled = plugin.getConfigManager().isNametagsEnabled();
        this.useTabIntegration = plugin.getConfigManager().getMainConfig()
            .getBoolean("nametags.use-tab-integration", true);
        
        if (!enabled) {
            Log.info("Nametag system disabled in configuration");
            return;
        }
        
        if (useTabIntegration) {
            Log.info("Nametag display delegated to TAB plugin");
            validateTabIntegration();
        } else {
            Log.warning("Native nametag implementation not available - using TAB integration");
            // TODO: implement native nametag handling using protocollib
            // rquires proper packet handling in new MC versions
        }
    }
    
    /**
     * initialize nametags for all online players
     * currently a no-op when using TAB integration
     */
    public void initialize() {
        if (!enabled || useTabIntegration) {
            return;
        }
        
        // TODO: implement native initialization
        // would create scoreboard teams for all online players
        
        Log.debug("Nametag manager initialized (placeholder mode)");
    }
    
    /**
     * setup nametag for a player
     * when using TAB, this just caches the player's data
     */
    public void setupPlayer(@NotNull Player player) {
        if (!enabled) {
            return;
        }
        
        // get player data
        PlayerCustomization data = plugin.getDataCache().getOrCreatePlayerData(player.getUniqueId());
        
        // create nametag data
        NametagData tagData = new NametagData(
            player.getUniqueId(),
            player.getName(),
            data.getPrefix(),
            data.getSuffix(),
            data.getNameColour()
        );
        
        playerTags.put(player.getUniqueId(), tagData);
        
        if (useTabIntegration) {
            // TAB will automatically pick up changes via PlaceholderAPI
            Log.debug("Player " + player.getName() + " nametag data cached for TAB");
        } else {
            // TODO: Implement native nametag creation
            // would create a scoreboard team for this player
            // updatePlayerTeam(player, tagData); etc.
        }
    }
    
    /**
     * update a player's nametag
     * when using TAB, this just updates the cache
     */
    public void updatePlayer(@NotNull Player player) {
        if (!enabled) {
            return;
        }
        
        // get fresh player data
        PlayerCustomization data = plugin.getDataCache().getPlayerData(player.getUniqueId());
        if (data == null) {
            return;
        }
        
        // update cached data
        NametagData tagData = playerTags.get(player.getUniqueId());
        if (tagData == null) {
            setupPlayer(player);
            return;
        }
        
        // update values
        tagData.prefix = data.getPrefix();
        tagData.suffix = data.getSuffix();
        tagData.nameColour = data.getNameColour();
        tagData.lastUpdated = System.currentTimeMillis();
        
        if (useTabIntegration) {
            // TAB will automatically pick up changes via PlaceholderAPI
            // might want to force a TAB refresh here
            refreshTabDisplay(player);
        } else {
            // TODO: Implement native nametag update
            // would update the scoreboard team packets
            // sendTeamUpdatePackets(player, tagData);
        }
    }
    
    /**
     * remove a player's nametag
     */
    public void removePlayer(@NotNull Player player) {
        if (!enabled) {
            return;
        }
        
        playerTags.remove(player.getUniqueId());
        
        if (!useTabIntegration) {
            // TODO: Implement native nametag removal
            // would remove the scoreboard team
            // removePlayerTeam(player);
        }
    }
    
    /**
     * force TAB to refresh a player's display
     * this uses TAB's API if available
     */
    private void refreshTabDisplay(@NotNull Player player) {
        // check if TAB API is available
        if (Bukkit.getPluginManager().isPluginEnabled("TAB")) {
            // TODO: Use TAB API to force refresh
            // TabAPI.getInstance().getTabPlayer(player.getUniqueId()).forceRefresh();
            
            // for now, we rely on TAB's automatic placeholder refresh
            Log.debug("TAB refresh triggered for " + player.getName());
        }
    }
    
    /**
     * validate that TAB integration is working
     */
    private void validateTabIntegration() {
        // check if TAB is installed
        if (!Bukkit.getPluginManager().isPluginEnabled("TAB")) {
            Log.warning("TAB plugin not found! Nametag display will not work!");
            Log.warning("Please install TAB or disable nametags in config.yml");
            return;
        }
        
        // check if placeholderAPI is installed
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            Log.warning("PlaceholderAPI not found! TAB integration will not work!");
            Log.warning("Please install PlaceholderAPI for nametag display");
            return;
        }
        
        // check if our placeholders are registered
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (plugin.getAPI() != null) {
                Log.info("TAB integration validated - nametags will display via TAB");
            }
        }, 40L); // check after 2 seconds
    }
    
    /**
     * get cached nametag data for a player
     * useful for debugging or API access
     */
    @NotNull
    public Optional<NametagData> getNametagData(@NotNull UUID uuid) {
        return Optional.ofNullable(playerTags.get(uuid));
    }
    
    /**
     * check if nametags are enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * check if using TAB integration
     */
    public boolean isUsingTabIntegration() {
        return useTabIntegration;
    }
    
    /**
     * cleanup resources
     */
    public void cleanup() {
        if (!enabled) {
            return;
        }
        
        if (!useTabIntegration) {
            // TODO: Clean up native implementation
            // Would remove all teams and cancel tasks
        }
        
        playerTags.clear();
        Log.debug("Nametag manager cleaned up");
    }
    
    /**
     * reload nametag configuration
     * useful when config changes
     */
    public void reload() {
        cleanup();
        initialize();
        
        // re-setup all online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            setupPlayer(player);
        }
        
        Log.info("Nametag manager reloaded");
    }
    
    /**
     * get statistics about nametag usage
     */
    @NotNull
    public String getStatistics() {
        return String.format(
            "Nametags: %s | Mode: %s | Cached: %d players",
            enabled ? "Enabled" : "Disabled",
            useTabIntegration ? "TAB Integration" : "Native",
            playerTags.size()
        );
    }
    
    // =====================================
    // TODO: Native Implementation Methods
    // =====================================
    
    /*
     * if implementing native nametag handling in the future,
     * these methods would handle the actual packet sending:
     * 
     * - createTeam(player, tagData) - Create scoreboard team
     * - updateTeam(player, tagData) - Update team prefix/suffix
     * - removeTeam(player) - Remove scoreboard team
     * - sendTeamPackets() - Send packets via ProtocolLib
     * 
     * this would require:
     * 1. proper ProtocolLib packet handling for MC 1.21.7+
     * 2. team sorting by rank weight
     * 3. handling 16-character limits
     * 4. color code processing
     * 5. packet batching for performance
     * 
     * for now, TAB handles all of this for us.
     */
    
    /**
     * data class for storing nametag information
     */
    public static class NametagData {
        private final UUID uuid;
        private final String username;
        private String prefix;
        private String suffix;
        private String nameColour;
        private long lastUpdated;
        
        public NametagData(@NotNull UUID uuid, @NotNull String username, 
                          String prefix, String suffix, String nameColour) {
            this.uuid = uuid;
            this.username = username;
            this.prefix = prefix;
            this.suffix = suffix;
            this.nameColour = nameColour;
            this.lastUpdated = System.currentTimeMillis();
        }
        
        // getters
        public UUID getUuid() { return uuid; }
        public String getUsername() { return username; }
        public String getPrefix() { return prefix; }
        public String getSuffix() { return suffix; }
        public String getNameColour() { return nameColour; }
        public long getLastUpdated() { return lastUpdated; }
        
        /**
         * build the full display name
         */
        @NotNull
        public String getFullDisplay() {
            StringBuilder display = new StringBuilder();
            
            if (prefix != null) {
                display.append(prefix).append(" ");
            }
            
            if (nameColour != null) {
                display.append(nameColour);
            }
            
            display.append(username);
            
            if (suffix != null) {
                display.append(" ").append(suffix);
            }
            
            return display.toString();
        }
    }
}