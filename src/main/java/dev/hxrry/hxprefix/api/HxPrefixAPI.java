package dev.hxrry.hxprefix.api;

import dev.hxrry.hxprefix.HxPrefix;
import dev.hxrry.hxprefix.api.events.*;
import dev.hxrry.hxprefix.api.models.PlayerCustomization;
import dev.hxrry.hxprefix.api.models.StyleOption;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Public API for HxPrefix
 * Other plugins should use this instead of accessing internals
 */
public class HxPrefixAPI {
    private final HxPrefix plugin;
    
    public HxPrefixAPI(HxPrefix plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Get a player's full customization data
     * @param uuid Player UUID
     * @return PlayerCustomization object or null if not found
     */
    @Nullable
    public PlayerCustomization getPlayerData(@NotNull UUID uuid) {
        return plugin.getDataCache().getPlayerData(uuid);
    }
    
    /**
     * Get a player's full customization data
     * @param player Player object
     * @return PlayerCustomization object or null if not found
     */
    @Nullable
    public PlayerCustomization getPlayerData(@NotNull Player player) {
        return getPlayerData(player.getUniqueId());
    }
    
    /**
     * Get a player's full customization data asynchronously
     * @param uuid Player UUID
     * @return CompletableFuture with PlayerCustomization
     */
    @NotNull
    public CompletableFuture<PlayerCustomization> getPlayerDataAsync(@NotNull UUID uuid) {
        return CompletableFuture.supplyAsync(() -> getPlayerData(uuid));
    }
    
    // ===== PREFIX METHODS =====
    
    /**
     * Set a player's prefix
     * @param player The player
     * @param prefix The prefix (with formatting codes)
     * @return true if successful
     */
    public boolean setPrefix(@NotNull Player player, @Nullable String prefix) {
        PlayerCustomization data = plugin.getDataCache().getOrCreatePlayerData(player.getUniqueId());
        String oldPrefix = data.getPrefix();
        
        // Fire event
        PrefixChangeEvent event = new PrefixChangeEvent(player, oldPrefix, prefix);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;
        
        // Update data
        data.setPrefix(event.getNewPrefix());
        plugin.getDataCache().savePlayerData(data);
        
        // Update nametag
        if (plugin.getNametagManager() != null) {
            plugin.getNametagManager().updatePlayer(player);
        }
        
        return true;
    }
    
    /**
     * Get a player's prefix
     * @param player The player
     * @return The prefix or null if not set
     */
    @Nullable
    public String getPrefix(@NotNull Player player) {
        PlayerCustomization data = getPlayerData(player);
        return data != null ? data.getPrefix() : null;
    }
    
    // ===== SUFFIX METHODS =====
    
    /**
     * Set a player's suffix
     * @param player The player
     * @param suffix The suffix (with formatting codes)
     * @return true if successful
     */
    public boolean setSuffix(@NotNull Player player, @Nullable String suffix) {
        PlayerCustomization data = plugin.getDataCache().getOrCreatePlayerData(player.getUniqueId());
        String oldSuffix = data.getSuffix();
        
        // Fire event
        SuffixChangeEvent event = new SuffixChangeEvent(player, oldSuffix, suffix);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;
        
        // Update data
        data.setSuffix(event.getNewSuffix());
        plugin.getDataCache().savePlayerData(data);
        
        // Update nametag
        if (plugin.getNametagManager() != null) {
            plugin.getNametagManager().updatePlayer(player);
        }
        
        return true;
    }
    
    /**
     * Get a player's suffix
     * @param player The player
     * @return The suffix or null if not set
     */
    @Nullable
    public String getSuffix(@NotNull Player player) {
        PlayerCustomization data = getPlayerData(player);
        return data != null ? data.getSuffix() : null;
    }
    
    // ===== COLOUR METHODS =====
    
    /**
     * Set a player's name colour
     * @param player The player
     * @param colour The colour (MiniMessage format)
     * @return true if successful
     */
    public boolean setNameColour(@NotNull Player player, @Nullable String colour) {
        PlayerCustomization data = plugin.getDataCache().getOrCreatePlayerData(player.getUniqueId());
        String oldColour = data.getNameColour();
        
        // Fire event
        ColourChangeEvent event = new ColourChangeEvent(player, oldColour, colour);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;
        
        // Update data
        data.setNameColour(event.getNewColour());
        plugin.getDataCache().savePlayerData(data);
        
        // Update nametag
        if (plugin.getNametagManager() != null) {
            plugin.getNametagManager().updatePlayer(player);
        }
        
        return true;
    }
    
    /**
     * Get a player's name colour
     * @param player The player
     * @return The colour or null if not set
     */
    @Nullable
    public String getNameColour(@NotNull Player player) {
        PlayerCustomization data = getPlayerData(player);
        return data != null ? data.getNameColour() : null;
    }
    
    // ===== NICKNAME METHODS =====
    
    /**
     * Set a player's nickname
     * @param player The player
     * @param nickname The nickname (without formatting)
     * @return true if successful
     */
    public boolean setNickname(@NotNull Player player, @Nullable String nickname) {
        PlayerCustomization data = plugin.getDataCache().getOrCreatePlayerData(player.getUniqueId());
        String oldNickname = data.getNickname();
        
        // Fire event
        NicknameChangeEvent event = new NicknameChangeEvent(player, oldNickname, nickname);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;
        
        // Update data
        data.setNickname(event.getNewNickname());
        plugin.getDataCache().savePlayerData(data);
        
        // Update nametag
        if (plugin.getNametagManager() != null) {
            plugin.getNametagManager().updatePlayer(player);
        }
        
        return true;
    }
    
    /**
     * Get a player's nickname
     * @param player The player
     * @return The nickname or null if not set
     */
    @Nullable
    public String getNickname(@NotNull Player player) {
        PlayerCustomization data = getPlayerData(player);
        return data != null ? data.getNickname() : null;
    }
    
    // ===== FORMATTED OUTPUT METHODS =====
    
    /**
     * Get a player's display name (nickname or username)
     * @param player The player
     * @return The display name
     */
    @NotNull
    public String getDisplayName(@NotNull Player player) {
        PlayerCustomization data = getPlayerData(player);
        if (data != null && data.getNickname() != null) {
            return data.getNickname();
        }
        return player.getName();
    }
    
    /**
     * Get a player's formatted name with colour
     * @param player The player
     * @return The formatted name
     */
    @NotNull
    public String getFormattedName(@NotNull Player player) {
        PlayerCustomization data = getPlayerData(player);
        String name = getDisplayName(player);
        
        if (data != null && data.getNameColour() != null) {
            return plugin.getConfigManager().getStyleConfig()
                .formatWithColour(data.getNameColour(), name);
        }
        
        return name;
    }
    
    /**
     * Get a player's full display format (prefix + name + suffix)
     * @param player The player
     * @return The full formatted display
     */
    @NotNull
    public String getFullDisplay(@NotNull Player player) {
        PlayerCustomization data = getPlayerData(player);
        StringBuilder display = new StringBuilder();
        
        if (data != null) {
            if (data.getPrefix() != null) {
                display.append(data.getPrefix()).append(" ");
            }
            
            display.append(getFormattedName(player));
            
            if (data.getSuffix() != null) {
                display.append(" ").append(data.getSuffix());
            }
        } else {
            display.append(player.getName());
        }
        
        return display.toString();
    }
    
    // ===== STYLE OPTIONS METHODS =====
    
    /**
     * Get available colour options for a player
     * @param player The player
     * @return List of available colour options
     */
    @NotNull
    public List<StyleOption> getAvailableColours(@NotNull Player player) {
        String rank = plugin.getLuckPermsHook() != null ? 
            plugin.getLuckPermsHook().getPrimaryGroup(player) : "default";
        return plugin.getConfigManager().getStyleConfig().getAvailableColours(rank);
    }
    
    /**
     * Get available prefix options for a player
     * @param player The player
     * @return List of available prefix options
     */
    @NotNull
    public List<StyleOption> getAvailablePrefixes(@NotNull Player player) {
        String rank = plugin.getLuckPermsHook() != null ? 
            plugin.getLuckPermsHook().getPrimaryGroup(player) : "default";
        return plugin.getConfigManager().getStyleConfig().getAvailablePrefixes(rank);
    }
    
    /**
     * Get available suffix options for a player
     * @param player The player
     * @return List of available suffix options
     */
    @NotNull
    public List<StyleOption> getAvailableSuffixes(@NotNull Player player) {
        String rank = plugin.getLuckPermsHook() != null ? 
            plugin.getLuckPermsHook().getPrimaryGroup(player) : "default";
        return plugin.getConfigManager().getStyleConfig().getAvailableSuffixes(rank);
    }
    
    // ===== UTILITY METHODS =====
    
    /**
     * Check if a player has permission for a feature
     * @param player The player
     * @param feature The feature (colour, prefix, suffix, nickname)
     * @return true if they have permission
     */
    public boolean hasPermission(@NotNull Player player, @NotNull String feature) {
        return plugin.getConfigManager().getPermissionConfig()
            .hasPermission(player, feature);
    }
    
    /**
     * Reload a player's data from database
     * @param player The player
     */
    public void reloadPlayer(@NotNull Player player) {
        plugin.getDataCache().reloadPlayer(player.getUniqueId());
        if (plugin.getNametagManager() != null) {
            plugin.getNametagManager().updatePlayer(player);
        }
    }
}