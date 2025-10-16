package dev.hxrry.hxprefix.hooks;

import dev.hxrry.hxcore.utils.Log;

import dev.hxrry.hxprefix.HxPrefix;
import dev.hxrry.hxprefix.api.models.PlayerCustomization;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PlaceholderAPI expansion for HxPrefix
 * 
 * Uses strategy pattern for maintainable placeholder handling.
 * Default placeholders return MiniMessage format (modern)
 * _legacy suffix returns legacy & format (for VentureChat, etc)
 * _stripped suffix returns plain text with no formatting
 */
public class PlaceholderAPIHook extends PlaceholderExpansion {
    private final HxPrefix plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();
    
    // Thread-safe handler map
    private final Map<String, PlaceholderHandler> handlers = new ConcurrentHashMap<>();
    
    public PlaceholderAPIHook(@NotNull HxPrefix plugin) {
        this.plugin = plugin;
        registerHandlers();
    }
    
    /**
     * Register all placeholder handlers using strategy pattern
     */
    private void registerHandlers() {
        // ===== MODERN FORMAT (MiniMessage) =====
        handlers.put("prefix", (player, data) -> 
            data != null && data.getPrefix() != null ? data.getPrefix() : "");
        
        handlers.put("suffix", (player, data) -> 
            data != null && data.getSuffix() != null ? data.getSuffix() : "");
        
        handlers.put("colour", (player, data) -> 
            data != null && data.getNameColour() != null ? data.getNameColour() : "");
        handlers.put("color", handlers.get("colour")); // Alias
        
        // ===== LEGACY FORMAT =====
        handlers.put("prefix_legacy", (player, data) -> {
            if (data != null && data.getPrefix() != null) {
                Component component = mm.deserialize(data.getPrefix());
                return legacy.serialize(component);
            }
            return "";
        });
        
        handlers.put("suffix_legacy", (player, data) -> {
            if (data != null && data.getSuffix() != null) {
                Component component = mm.deserialize(data.getSuffix());
                return legacy.serialize(component);
            }
            return "";
        });
        
        handlers.put("colour_legacy", (player, data) -> {
            if (data != null && data.getNameColour() != null) {
                Component component = mm.deserialize(data.getNameColour());
                return legacy.serialize(component);
            }
            return "";
        });
        handlers.put("color_legacy", handlers.get("colour_legacy")); // Alias
        
        // ===== STRIPPED FORMAT =====
        handlers.put("prefix_stripped", (player, data) -> {
            if (data != null && data.getPrefix() != null) {
                Component component = mm.deserialize(data.getPrefix());
                String legacyText = legacy.serialize(component);
                return stripColours(legacyText);
            }
            return "";
        });
        
        handlers.put("suffix_stripped", (player, data) -> {
            if (data != null && data.getSuffix() != null) {
                Component component = mm.deserialize(data.getSuffix());
                String legacyText = legacy.serialize(component);
                return stripColours(legacyText);
            }
            return "";
        });
        
        handlers.put("colour_stripped", (player, data) -> {
            if (data != null && data.getNameColour() != null) {
                Component component = mm.deserialize(data.getNameColour());
                String legacyText = legacy.serialize(component);
                return stripColours(legacyText);
            }
            return "";
        });
        handlers.put("color_stripped", handlers.get("colour_stripped")); // Alias
        
        // ===== NICKNAME AND NAME =====
        handlers.put("nickname", (player, data) -> {
            if (data != null && data.getNickname() != null) {
                return data.getNickname();
            }
            return player.getName();
        });
        handlers.put("nick", handlers.get("nickname")); // Alias
        
        handlers.put("name", (player, data) -> {
            if (data != null && data.getNickname() != null) {
                return data.getNickname();
            }
            return player.getName();
        });
        
        handlers.put("formatted_name", (player, data) -> {
            String name = player.getName();
            if (data != null && data.getNickname() != null) {
                name = data.getNickname();
            }
            
            if (data != null && data.getNameColour() != null) {
                return plugin.getConfigManager().getStyleConfig()
                    .formatWithColour(data.getNameColour(), name);
            }
            return name;
        });
        handlers.put("fname", handlers.get("formatted_name")); // Alias
        
        handlers.put("formatted_name_legacy", (player, data) -> {
            String name = player.getName();
            if (data != null && data.getNickname() != null) {
                name = data.getNickname();
            }
            
            if (data != null && data.getNameColour() != null) {
                String formatted = plugin.getConfigManager().getStyleConfig()
                    .formatWithColour(data.getNameColour(), name);
                Component component = mm.deserialize(formatted);
                return legacy.serialize(component);
            }
            return name;
        });
        handlers.put("fname_legacy", handlers.get("formatted_name_legacy")); // Alias
        
        // ===== FULL DISPLAY =====
        handlers.put("display", (player, data) -> {
            StringBuilder display = new StringBuilder();
            
            if (data != null) {
                if (data.getPrefix() != null) {
                    display.append(data.getPrefix()).append(" ");
                }
                
                String name = data.getNickname() != null ? 
                    data.getNickname() : player.getName();
                
                if (data.getNameColour() != null) {
                    display.append(plugin.getConfigManager().getStyleConfig()
                        .formatWithColour(data.getNameColour(), name));
                } else {
                    display.append(name);
                }
                
                if (data.getSuffix() != null) {
                    display.append(" ").append(data.getSuffix());
                }
            } else {
                display.append(player.getName());
            }
            
            return display.toString();
        });
        handlers.put("full", handlers.get("display")); // Alias
        
        handlers.put("display_legacy", (player, data) -> {
            StringBuilder display = new StringBuilder();
            
            if (data != null) {
                if (data.getPrefix() != null) {
                    Component prefix = mm.deserialize(data.getPrefix());
                    display.append(legacy.serialize(prefix)).append(" ");
                }
                
                String name = data.getNickname() != null ? 
                    data.getNickname() : player.getName();
                
                if (data.getNameColour() != null) {
                    String formatted = plugin.getConfigManager().getStyleConfig()
                        .formatWithColour(data.getNameColour(), name);
                    Component colored = mm.deserialize(formatted);
                    display.append(legacy.serialize(colored));
                } else {
                    display.append(name);
                }
                
                if (data.getSuffix() != null) {
                    Component suffix = mm.deserialize(data.getSuffix());
                    display.append(" ").append(legacy.serialize(suffix));
                }
            } else {
                display.append(player.getName());
            }
            
            return display.toString();
        });
        handlers.put("full_legacy", handlers.get("display_legacy")); // Alias
        
        // ===== STATUS PLACEHOLDERS =====
        handlers.put("has_prefix", (player, data) -> 
            String.valueOf(data != null && data.getPrefix() != null));
        
        handlers.put("has_suffix", (player, data) -> 
            String.valueOf(data != null && data.getSuffix() != null));
        
        handlers.put("has_colour", (player, data) -> 
            String.valueOf(data != null && data.getNameColour() != null));
        handlers.put("has_color", handlers.get("has_colour")); // Alias
        
        handlers.put("has_nickname", (player, data) -> 
            String.valueOf(data != null && data.getNickname() != null));
        handlers.put("has_nick", handlers.get("has_nickname")); // Alias
        
        handlers.put("has_customizations", (player, data) -> 
            String.valueOf(data != null && data.hasCustomizations()));
        
        handlers.put("has_tag_request", (player, data) -> 
            String.valueOf(data != null && data.hasPendingTagRequest()));
        
        // ===== RANK PLACEHOLDER =====
        handlers.put("rank", (player, data) -> {
            if (plugin.getLuckPermsHook() != null) {
                return plugin.getLuckPermsHook().getPrimaryGroup(player);
            }
            return "default";
        });
        
        // ===== PERMISSION CHECK PLACEHOLDERS =====
        handlers.put("can_use_colours", (player, data) -> 
            String.valueOf(plugin.getConfigManager().getPermissionConfig()
                .hasPermission(player, "colour")));
        handlers.put("can_use_colors", handlers.get("can_use_colours")); // Alias
        
        handlers.put("can_use_prefixes", (player, data) -> 
            String.valueOf(plugin.getConfigManager().getPermissionConfig()
                .hasPermission(player, "prefix")));
        
        handlers.put("can_use_suffixes", (player, data) -> 
            String.valueOf(plugin.getConfigManager().getPermissionConfig()
                .hasPermission(player, "suffix")));
        
        handlers.put("can_use_nicknames", (player, data) -> 
            String.valueOf(plugin.getConfigManager().getPermissionConfig()
                .hasPermission(player, "nickname")));
        
        handlers.put("can_use_custom_tags", (player, data) -> 
            String.valueOf(plugin.getConfigManager().getPermissionConfig()
                .hasPermission(player, "custom-tags")));
    }
    
    @Override
    @NotNull
    public String getIdentifier() {
        return "hxprefix";
    }
    
    @Override
    @NotNull
    public String getAuthor() {
        return "hxrry";
    }
    
    @SuppressWarnings("deprecation")
    @Override
    @NotNull
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public boolean canRegister() {
        return true;
    }
    
    @Override
    @Nullable
    public String onPlaceholderRequest(@Nullable Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        
        // Get player data from cache
        PlayerCustomization data = plugin.getDataCache().getPlayerData(player.getUniqueId());
        
        // Find and execute handler
        PlaceholderHandler handler = handlers.get(params.toLowerCase());
        return handler != null ? handler.handle(player, data) : null;
    }
    
    /**
     * Register this expansion
     */
    public boolean register() {
        if (super.register()) {
            Log.info("Registered PlaceholderAPI expansion with " + handlers.size() + " placeholders");
            logAvailablePlaceholders();
            return true;
        }
        return false;
    }
    
    /**
     * Strip colour codes from text
     */
    private String stripColours(@NotNull String input) {
        // Strip legacy codes
        String stripped = input.replaceAll("&[0-9a-fk-or]", "");
        stripped = stripped.replaceAll("§[0-9a-fk-or]", "");
        // Strip minimessage tags
        stripped = stripped.replaceAll("<[^>]+>", "");
        return stripped.trim();
    }
    
    /**
     * Log available placeholders for reference
     */
    private void logAvailablePlaceholders() {
        Log.info("Available placeholders:");
        Log.info("  === MODERN FORMAT (MiniMessage) ===");
        Log.info("  %hxprefix_prefix% - player's prefix");
        Log.info("  %hxprefix_suffix% - player's suffix");
        Log.info("  %hxprefix_colour% - player's name colour");
        Log.info("  %hxprefix_formatted_name% - coloured display name");
        Log.info("  %hxprefix_display% - full display (prefix + name + suffix)");
        Log.info("");
        Log.info("  === LEGACY FORMAT (for VentureChat, etc) ===");
        Log.info("  %hxprefix_prefix_legacy% - prefix with & codes");
        Log.info("  %hxprefix_suffix_legacy% - suffix with & codes");
        Log.info("  %hxprefix_colour_legacy% - colour with & codes");
        Log.info("  %hxprefix_formatted_name_legacy% - name with & codes");
        Log.info("  %hxprefix_display_legacy% - full display with & codes");
        Log.info("");
        Log.info("  === STRIPPED FORMAT (no colors) ===");
        Log.info("  %hxprefix_prefix_stripped% - prefix without colors");
        Log.info("  %hxprefix_suffix_stripped% - suffix without colors");
        Log.info("  %hxprefix_colour_stripped% - colour code stripped");
        Log.info("");
        Log.info("  === OTHER PLACEHOLDERS ===");
        Log.info("  %hxprefix_nickname% - player's nickname");
        Log.info("  %hxprefix_name% - display name (nick or username)");
        Log.info("  %hxprefix_rank% - player's rank");
        Log.info("  %hxprefix_has_prefix% - true/false");
        Log.info("  %hxprefix_has_suffix% - true/false");
        Log.info("  %hxprefix_has_colour% - true/false");
        Log.info("  %hxprefix_has_nickname% - true/false");
    }
    
    /**
     * Functional interface for placeholder handlers
     */
    @FunctionalInterface
    private interface PlaceholderHandler {
        /**
         * Handle a placeholder request
         * 
         * @param player The player
         * @param data The player's customization data (can be null)
         * @return The placeholder value
         */
        String handle(Player player, @Nullable PlayerCustomization data);
    }
}