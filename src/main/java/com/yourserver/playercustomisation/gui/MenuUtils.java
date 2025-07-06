package com.yourserver.playercustomisation.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for menu-related functions
 */
public class MenuUtils {
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacySection();
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    
    /**
     * Colorizes text supporting both legacy codes and hex colors
     * Supports: &c, &#FF0000, and MiniMessage formats
     */
    public static String colorize(String text) {
        if (text == null) return null;
        
        // First handle &#RRGGBB format (Birdflop RGB)
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            String hex = matcher.group(1);
            String replacement = "§x";
            for (char c : hex.toCharArray()) {
                replacement += "§" + c;
            }
            matcher.appendReplacement(buffer, replacement);
        }
        matcher.appendTail(buffer);
        text = buffer.toString();
        
        // Then handle legacy color codes
        text = ChatColor.translateAlternateColorCodes('&', text);
        
        return text;
    }
    
    /**
     * Converts a hex color to MiniMessage format for storage
     * Input: "#FF0000" or "&#FF0000" or "FF0000"
     * Output: "<color:#FF0000>"
     */
    public static String hexToMiniMessage(String hex) {
        // Clean the input
        hex = hex.replace("&#", "").replace("#", "").replace("<", "").replace(">", "");
        
        // Ensure it's 6 characters
        if (hex.length() != 6) {
            return "<color:#FFFFFF>"; // Default to white
        }
        
        return "<color:#" + hex.toUpperCase() + ">";
    }
    
    /**
     * Converts a gradient to MiniMessage format
     * Input: ["#FF0000", "#00FF00"]
     * Output: "<gradient:#FF0000:#00FF00>"
     */
    public static String gradientToMiniMessage(String... colors) {
        StringBuilder gradient = new StringBuilder("<gradient:");
        for (int i = 0; i < colors.length; i++) {
            if (i > 0) gradient.append(":");
            String hex = colors[i].replace("&#", "").replace("#", "");
            gradient.append("#").append(hex.toUpperCase());
        }
        gradient.append(">");
        return gradient.toString();
    }
    
    /**
     * Converts MiniMessage color to Birdflop format for compatibility
     * Input: "<color:#FF0000>" or "#FF0000"
     * Output: "&#FF0000"
     */
    public static String toBirdflop(String color) {
        if (color.startsWith("<color:#") && color.endsWith(">")) {
            // Extract hex from MiniMessage format
            String hex = color.substring(8, color.length() - 1);
            return "&#" + hex;
        } else if (color.startsWith("#")) {
            return "&" + color;
        } else if (color.startsWith("&#")) {
            return color;
        }
        return color;
    }
    
    /**
     * Creates a rainbow MiniMessage tag
     */
    public static String rainbowTag() {
        return "<rainbow>";
    }
    
    /**
     * Gets a material for a color (for menu items)
     */
    public static Material getMaterialForColor(String colorName, boolean isGradient, boolean isRainbow) {
        if (isRainbow) {
            return Material.NETHER_STAR;
        } else if (isGradient) {
            return Material.FIREWORK_STAR;
        }
        
        // Map color names to dye materials
        String lower = colorName.toLowerCase();
        if (lower.contains("red") && !lower.contains("dark")) return Material.RED_DYE;
        if (lower.contains("dark red") || lower.contains("crimson")) return Material.RED_DYE;
        if (lower.contains("blue") && !lower.contains("light")) return Material.BLUE_DYE;
        if (lower.contains("light blue") || lower.contains("aqua")) return Material.LIGHT_BLUE_DYE;
        if (lower.contains("green") && !lower.contains("dark")) return Material.LIME_DYE;
        if (lower.contains("dark green")) return Material.GREEN_DYE;
        if (lower.contains("yellow") || lower.contains("gold")) return Material.YELLOW_DYE;
        if (lower.contains("purple") || lower.contains("violet")) return Material.PURPLE_DYE;
        if (lower.contains("pink") || lower.contains("magenta")) return Material.PINK_DYE;
        if (lower.contains("orange")) return Material.ORANGE_DYE;
        if (lower.contains("white")) return Material.WHITE_DYE;
        if (lower.contains("gray") || lower.contains("grey")) return Material.GRAY_DYE;
        if (lower.contains("black")) return Material.BLACK_DYE;
        if (lower.contains("brown")) return Material.BROWN_DYE;
        if (lower.contains("cyan") || lower.contains("turquoise")) return Material.CYAN_DYE;
        
        // Default
        return Material.PAPER;
    }
    
    /**
     * Formats a preview with color applied
     * @param colorValue The MiniMessage color format
     * @param text The text to preview
     * @return Formatted preview string
     */
    public static String createPreview(String colorValue, String text) {
        String preview = colorValue + text;
        
        // Close the tag based on type
        if (colorValue.startsWith("<gradient:")) {
            preview += "</gradient>";
        } else if (colorValue.equals("<rainbow>")) {
            preview += "</rainbow>";
        } else if (colorValue.startsWith("<color:")) {
            preview += "</color>";
        }
        
        // Convert to legacy format for display
        try {
            Component component = miniMessage.deserialize(preview);
            return legacySerializer.serialize(component);
        } catch (Exception e) {
            // Fallback to basic colorization
            return colorize("&7" + text);
        }
    }
    
    /**
     * Applies a player's color to text (for prefixes/suffixes)
     */
    public static String applyPlayerColor(String color, String text) {
        if (color == null || text == null) return text;
        
        String formatted = color + text;
        
        // Close the tag
        if (color.startsWith("<gradient:")) {
            formatted += "</gradient>";
        } else if (color.equals("<rainbow>")) {
            formatted += "</rainbow>";
        } else if (color.startsWith("<color:")) {
            formatted += "</color>";
        }
        
        // Convert to display format
        try {
            Component component = miniMessage.deserialize(formatted);
            return legacySerializer.serialize(component);
        } catch (Exception e) {
            return text;
        }
    }
}