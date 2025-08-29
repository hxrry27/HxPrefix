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

/**
 * placeholderapi expansion for hxprefix
 * 
 * Default placeholders return MiniMessage format (modern)
 * _legacy suffix returns legacy & format (for VentureChat, etc)
 * _stripped suffix returns plain text with no formatting
 */
public class PlaceholderAPIHook extends PlaceholderExpansion {
    private final HxPrefix plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();
    
    public PlaceholderAPIHook(@NotNull HxPrefix plugin) {
        this.plugin = plugin;
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
        
        // get player data (from cache if available)
        PlayerCustomization data = plugin.getDataCache().getPlayerData(player.getUniqueId());
        
        // handle different placeholders
        return switch (params.toLowerCase()) {
            // ===== MODERN FORMAT (MiniMessage) - DEFAULT =====
            case "prefix" -> {
                if (data != null && data.getPrefix() != null) {
                    yield data.getPrefix(); // Return MiniMessage format
                }
                yield "";
            }
            
            case "suffix" -> {
                if (data != null && data.getSuffix() != null) {
                    yield data.getSuffix(); // Return MiniMessage format
                }
                yield "";
            }
            
            case "colour", "color" -> {
                if (data != null && data.getNameColour() != null) {
                    yield data.getNameColour(); // Return MiniMessage format
                }
                yield "";
            }
            
            // ===== LEGACY FORMAT (for VentureChat, older plugins) =====
            case "prefix_legacy" -> {
                if (data != null && data.getPrefix() != null) {
                    Component component = mm.deserialize(data.getPrefix());
                    yield legacy.serialize(component);
                }
                yield "";
            }
            
            case "suffix_legacy" -> {
                if (data != null && data.getSuffix() != null) {
                    Component component = mm.deserialize(data.getSuffix());
                    yield legacy.serialize(component);
                }
                yield "";
            }
            
            case "colour_legacy", "color_legacy" -> {
                if (data != null && data.getNameColour() != null) {
                    Component component = mm.deserialize(data.getNameColour());
                    yield legacy.serialize(component);
                }
                yield "";
            }
            
            // ===== STRIPPED FORMAT (no colors at all) =====
            case "prefix_stripped" -> {
                if (data != null && data.getPrefix() != null) {
                    Component component = mm.deserialize(data.getPrefix());
                    String legacyText = legacy.serialize(component);
                    yield stripColours(legacyText);
                }
                yield "";
            }
            
            case "suffix_stripped" -> {
                if (data != null && data.getSuffix() != null) {
                    Component component = mm.deserialize(data.getSuffix());
                    String legacyText = legacy.serialize(component);
                    yield stripColours(legacyText);
                }
                yield "";
            }
            
            case "colour_stripped", "color_stripped" -> {
                if (data != null && data.getNameColour() != null) {
                    Component component = mm.deserialize(data.getNameColour());
                    String legacyText = legacy.serialize(component);
                    yield stripColours(legacyText);
                }
                yield "";
            }
            
            // ===== NICKNAME AND NAME PLACEHOLDERS =====
            case "nickname", "nick" -> {
                if (data != null && data.getNickname() != null) {
                    yield data.getNickname();
                }
                yield player.getName();
            }
            
            case "name" -> {
                // returns the display name (nickname or username)
                if (data != null && data.getNickname() != null) {
                    yield data.getNickname();
                }
                yield player.getName();
            }
            
            case "formatted_name", "fname" -> {
                // returns name with colour applied (MiniMessage format)
                String name = player.getName();
                if (data != null && data.getNickname() != null) {
                    name = data.getNickname();
                }
                
                if (data != null && data.getNameColour() != null) {
                    yield plugin.getConfigManager().getStyleConfig()
                        .formatWithColour(data.getNameColour(), name);
                }
                yield name;
            }
            
            case "formatted_name_legacy", "fname_legacy" -> {
                // returns name with colour applied (legacy format)
                String name = player.getName();
                if (data != null && data.getNickname() != null) {
                    name = data.getNickname();
                }
                
                if (data != null && data.getNameColour() != null) {
                    String formatted = plugin.getConfigManager().getStyleConfig()
                        .formatWithColour(data.getNameColour(), name);
                    Component component = mm.deserialize(formatted);
                    yield legacy.serialize(component);
                }
                yield name;
            }
            
            // ===== FULL DISPLAY PLACEHOLDERS =====
            case "display", "full" -> {
                // returns full display: prefix + coloured name + suffix (MiniMessage format)
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
                
                yield display.toString();
            }
            
            case "display_legacy", "full_legacy" -> {
                // returns full display in legacy format
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
                
                yield display.toString();
            }
            
            // ===== STATUS PLACEHOLDERS =====
            case "has_prefix" -> {
                yield String.valueOf(data != null && data.getPrefix() != null);
            }
            
            case "has_suffix" -> {
                yield String.valueOf(data != null && data.getSuffix() != null);
            }
            
            case "has_colour", "has_color" -> {
                yield String.valueOf(data != null && data.getNameColour() != null);
            }
            
            case "has_nickname", "has_nick" -> {
                yield String.valueOf(data != null && data.getNickname() != null);
            }
            
            case "has_customizations" -> {
                yield String.valueOf(data != null && data.hasCustomizations());
            }
            
            case "has_tag_request" -> {
                yield String.valueOf(data != null && data.hasPendingTagRequest());
            }
            
            // ===== RANK PLACEHOLDER =====
            case "rank" -> {
                if (plugin.getLuckPermsHook() != null) {
                    yield plugin.getLuckPermsHook().getPrimaryGroup(player);
                }
                yield "default";
            }
            
            // ===== PERMISSION CHECK PLACEHOLDERS =====
            case "can_use_colours", "can_use_colors" -> {
                yield String.valueOf(plugin.getConfigManager().getPermissionConfig()
                    .hasPermission(player, "colour"));
            }
            
            case "can_use_prefixes" -> {
                yield String.valueOf(plugin.getConfigManager().getPermissionConfig()
                    .hasPermission(player, "prefix"));
            }
            
            case "can_use_suffixes" -> {
                yield String.valueOf(plugin.getConfigManager().getPermissionConfig()
                    .hasPermission(player, "suffix"));
            }
            
            case "can_use_nicknames" -> {
                yield String.valueOf(plugin.getConfigManager().getPermissionConfig()
                    .hasPermission(player, "nickname"));
            }
            
            case "can_use_custom_tags" -> {
                yield String.valueOf(plugin.getConfigManager().getPermissionConfig()
                    .hasPermission(player, "custom-tags"));
            }
            
            // default case
            default -> null;
        };
    }
    
    /**
     * register this expansion
     */
    public boolean register() {
        if (super.register()) {
            Log.info("registered placeholderapi expansion");
            logAvailablePlaceholders();
            return true;
        }
        return false;
    }
    
    /**
     * strip colour codes from text
     */
    private String stripColours(@NotNull String input) {
        // strip legacy codes
        String stripped = input.replaceAll("&[0-9a-fk-or]", "");
        stripped = stripped.replaceAll("§[0-9a-fk-or]", "");
        // strip minimessage tags
        stripped = stripped.replaceAll("<[^>]+>", "");
        return stripped.trim();
    }
    
    /**
     * log available placeholders for reference
     */
    private void logAvailablePlaceholders() {
        Log.info("available placeholders:");
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
}