package com.prefix27.customization.integrations;

import com.prefix27.customization.PlayerCustomizationPlugin;
import com.prefix27.customization.database.PlayerData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlaceholderAPIIntegration extends PlaceholderExpansion {
    
    private final PlayerCustomizationPlugin plugin;
    
    public PlaceholderAPIIntegration(PlayerCustomizationPlugin plugin) {
        this.plugin = plugin;
    }
    
    public boolean initialize() {
        try {
            if (register()) {
                plugin.getLogger().info("PlaceholderAPI integration registered successfully!");
                return true;
            } else {
                plugin.getLogger().warning("Failed to register PlaceholderAPI expansion!");
                return false;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("PlaceholderAPI integration failed: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    @NotNull
    public String getIdentifier() {
        return "customization";
    }
    
    @Override
    @NotNull
    public String getAuthor() {
        return "prefix27";
    }
    
    @Override
    @NotNull
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return "";
        }
        
        switch (params.toLowerCase()) {
            case "name":
                return getFormattedName(playerData);
                
            case "prefix":
                return getFormattedPrefix(playerData);
                
            case "nick":
            case "nickname":
                return playerData.hasNickname() ? playerData.getCurrentNickname() : player.getName();
                
            case "full":
                return getFullFormattedName(playerData);
                
            case "rank":
                return playerData.getRank();
                
            case "color":
                return playerData.hasNameColor() ? playerData.getCurrentNameColor() : "none";
                
            case "gradient":
                return playerData.hasNameGradient() ? playerData.getCurrentNameGradient() : "none";
                
            case "has_color":
                return playerData.hasNameColor() ? "true" : "false";
                
            case "has_gradient":
                return playerData.hasNameGradient() ? "true" : "false";
                
            case "has_prefix":
                return playerData.hasPrefix() ? "true" : "false";
                
            case "has_nickname":
                return playerData.hasNickname() ? "true" : "false";
                
            case "display_name":
                return playerData.getDisplayName();
                
            case "username":
                return playerData.getUsername();
                
            default:
                return null;
        }
    }
    
    private String getFormattedName(PlayerData playerData) {
        String name = playerData.getDisplayName();
        
        Component nameComponent;
        if (playerData.hasNameGradient()) {
            nameComponent = plugin.getColorManager().applyGradient(name, playerData.getCurrentNameGradient());
        } else if (playerData.hasNameColor()) {
            if (playerData.getCurrentNameColor().equals("rainbow")) {
                nameComponent = plugin.getColorManager().createRainbowText(name);
            } else {
                nameComponent = plugin.getColorManager().applyColor(name, playerData.getCurrentNameColor());
            }
        } else {
            nameComponent = Component.text(name);
        }
        
        return LegacyComponentSerializer.legacySection().serialize(nameComponent);
    }
    
    private String getFormattedPrefix(PlayerData playerData) {
        if (!playerData.hasPrefix()) {
            return "";
        }
        
        Component prefixComponent = plugin.getPrefixManager().formatPrefix(
            playerData.getCurrentPrefixId(), 
            null, // Use default color for now
            null  // Use default gradient for now
        );
        
        return LegacyComponentSerializer.legacySection().serialize(prefixComponent);
    }
    
    private String getFullFormattedName(PlayerData playerData) {
        String prefix = getFormattedPrefix(playerData);
        String name = getFormattedName(playerData);
        
        if (prefix.isEmpty()) {
            return name;
        } else {
            return prefix + " " + name;
        }
    }
}