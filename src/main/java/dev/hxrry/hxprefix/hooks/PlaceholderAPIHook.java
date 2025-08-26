package dev.hxrry.hxprefix.hooks;

import dev.hxrry.hxcore.utils.Log;

import dev.hxrry.hxprefix.HxPrefix;
import dev.hxrry.hxprefix.api.models.PlayerCustomization;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * placeholderapi expansion for hxprefix
 */
public class PlaceholderAPIHook extends PlaceholderExpansion {
    private final HxPrefix plugin;
    
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
            // basic data placeholders
            case "prefix" -> {
                if (data != null && data.getPrefix() != null) {
                    yield data.getPrefix();
                }
                yield "";
            }
            
            case "suffix" -> {
                if (data != null && data.getSuffix() != null) {
                    yield data.getSuffix();
                }
                yield "";
            }
            
            case "colour", "color" -> {
                if (data != null && data.getNameColour() != null) {
                    yield data.getNameColour();
                }
                yield "";
            }
            
            case "nickname", "nick" -> {
                if (data != null && data.getNickname() != null) {
                    yield data.getNickname();
                }
                yield player.getName();
            }
            
            // formatted name placeholders
            case "name" -> {
                // returns the display name (nickname or username)
                if (data != null && data.getNickname() != null) {
                    yield data.getNickname();
                }
                yield player.getName();
            }
            
            case "formatted_name", "fname" -> {
                // returns name with colour applied
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
            
            case "display", "full" -> {
                // returns full display: prefix + coloured name + suffix
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
            
            // status placeholders
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
            
            // raw values without formatting
            case "prefix_raw" -> {
                if (data != null && data.getPrefix() != null) {
                    yield stripColours(data.getPrefix());
                }
                yield "";
            }
            
            case "suffix_raw" -> {
                if (data != null && data.getSuffix() != null) {
                    yield stripColours(data.getSuffix());
                }
                yield "";
            }
            
            // rank placeholder
            case "rank" -> {
                if (plugin.getLuckPermsHook() != null) {
                    yield plugin.getLuckPermsHook().getPrimaryGroup(player);
                }
                yield "default";
            }
            
            // permission check placeholders
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
        // strip minimessage tags
        String stripped = input.replaceAll("<[^>]+>", "");
        // strip legacy codes
        stripped = stripped.replaceAll("&[0-9a-fk-or]", "");
        stripped = stripped.replaceAll("§[0-9a-fk-or]", "");
        return stripped.trim();
    }
    
    /**
     * log available placeholders for reference
     */
    private void logAvailablePlaceholders() {
        Log.info("available placeholders:");
        Log.info("  %hxprefix_prefix% - player's prefix");
        Log.info("  %hxprefix_suffix% - player's suffix");
        Log.info("  %hxprefix_colour% - player's name colour");
        Log.info("  %hxprefix_nickname% - player's nickname");
        Log.info("  %hxprefix_name% - display name (nick or username)");
        Log.info("  %hxprefix_formatted_name% - coloured display name");
        Log.info("  %hxprefix_display% - full display (prefix + name + suffix)");
        Log.info("  %hxprefix_has_prefix% - true/false");
        Log.info("  %hxprefix_has_suffix% - true/false");
        Log.info("  %hxprefix_has_colour% - true/false");
        Log.info("  %hxprefix_has_nickname% - true/false");
        Log.info("  %hxprefix_rank% - player's rank");
    }
}