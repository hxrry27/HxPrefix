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
            plugin.getLogger().warning("PlaceholderAPI called for " + player.getName() + " but no cached data! Placeholder: " + params);
            return "";
        }
        
        // Debug logging for cross-server testing
        if (params.equals("prefix") || params.equals("full")) {
            plugin.getLogger().info("PlaceholderAPI " + params + " for " + player.getName() + ": prefix=" + playerData.getCurrentPrefixId() + ", color=" + playerData.getCurrentNameColor());
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
        
        // Simple name formatting - ONLY use name color/gradient settings
        if (playerData.hasNameGradient()) {
            // Apply gradient to name only
            String gradient = playerData.getCurrentNameGradient();
            return applySimpleGradient(name, gradient);
        } else if (playerData.hasNameColor()) {
            // Apply solid color to name only
            String color = playerData.getCurrentNameColor();
            return applySimpleColor(name, color);
        } else {
            // Default white name
            return "§f" + name;
        }
    }
    
    private String getFormattedPrefix(PlayerData playerData) {
        if (!playerData.hasPrefix()) {
            return "";
        }
        
        // Simple prefix formatting - ONLY use prefix settings
        String prefixId = playerData.getCurrentPrefixId();
        return formatSimplePrefix(prefixId);
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
    
    // Simple color and gradient formatting methods
    private String applySimpleColor(String text, String color) {
        switch (color.toLowerCase()) {
            case "red": return "§c" + text;
            case "blue": return "§9" + text;
            case "green": return "§a" + text;
            case "yellow": return "§e" + text;
            case "purple": return "§5" + text;
            case "orange": return "§6" + text;
            case "pink": return "§d" + text;
            case "cyan": return "§b" + text;
            case "white": return "§f" + text;
            case "gray": return "§7" + text;
            case "black": return "§0" + text;
            default: return "§f" + text;
        }
    }
    
    private String applySimpleGradient(String text, String gradient) {
        // For now, just use the first color of the gradient
        // This can be enhanced later with actual gradient rendering
        String[] colors = gradient.split(":");
        if (colors.length > 0) {
            return applySimpleColor(text, colors[0]);
        }
        return "§f" + text;
    }
    
    private String formatSimplePrefix(String prefixId) {
        if (prefixId == null || prefixId.isEmpty()) {
            return "";
        }
        
        // Parse prefixId like "supporter_red" or "patron_gradient_red_to_blue"
        String prefixText;
        String color = "white"; // default
        
        if (prefixId.contains("_gradient_")) {
            // Handle gradient prefixes
            String[] parts = prefixId.split("_gradient_");
            String rank = parts[0];
            prefixText = getPrefixTextForRank(rank);
            
            if (parts.length > 1) {
                // Use first color of gradient
                String gradientPart = parts[1].replace("_to_", ":");
                String[] gradientColors = gradientPart.split(":");
                if (gradientColors.length > 0) {
                    color = gradientColors[0];
                }
            }
        } else if (prefixId.contains("_")) {
            // Handle solid color prefixes
            String[] parts = prefixId.split("_", 2);
            String rank = parts[0];
            prefixText = getPrefixTextForRank(rank);
            
            if (parts.length > 1) {
                color = parts[1];
            }
        } else {
            // Simple rank prefix
            prefixText = getPrefixTextForRank(prefixId);
        }
        
        // Apply bold formatting to prefix and color
        return applySimpleColor("§l" + prefixText, color);
    }
    
    private String getPrefixTextForRank(String rank) {
        switch (rank.toLowerCase()) {
            case "supporter": return "[SUPPORTER]";
            case "patron": return "[PATRON]";
            case "devoted": return "[DEVOTED]";
            default: return "[" + rank.toUpperCase() + "]";
        }
    }
}