package com.prefix27.customization.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.regex.Pattern;

public class ColorUtils {
    
    private static final Pattern HEX_PATTERN = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("§[0-9a-fk-or]");
    
    public static boolean isValidHexColor(String hex) {
        return hex != null && HEX_PATTERN.matcher(hex).matches();
    }
    
    public static TextColor parseColor(String colorString) {
        if (colorString == null || colorString.isEmpty()) {
            return null;
        }
        
        // Check if it's a hex color
        if (isValidHexColor(colorString)) {
            return TextColor.fromHexString(colorString);
        }
        
        // Check if it's a named color
        try {
            return NamedTextColor.NAMES.value(colorString.toLowerCase());
        } catch (Exception e) {
            return null;
        }
    }
    
    public static String stripLegacyColors(String text) {
        if (text == null) {
            return null;
        }
        return LEGACY_COLOR_PATTERN.matcher(text).replaceAll("");
    }
    
    public static String componentToLegacy(Component component) {
        return LegacyComponentSerializer.legacySection().serialize(component);
    }
    
    public static Component legacyToComponent(String legacy) {
        return LegacyComponentSerializer.legacySection().deserialize(legacy);
    }
    
    public static String rgbToHex(int r, int g, int b) {
        return String.format("#%02X%02X%02X", r, g, b);
    }
    
    public static int[] hexToRgb(String hex) {
        if (!isValidHexColor(hex)) {
            return new int[]{255, 255, 255}; // Default to white
        }
        
        hex = hex.substring(1); // Remove #
        
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        
        return new int[]{r, g, b};
    }
    
    public static TextColor interpolateColors(TextColor start, TextColor end, float ratio) {
        if (ratio <= 0) return start;
        if (ratio >= 1) return end;
        
        int startR = start.red();
        int startG = start.green();
        int startB = start.blue();
        
        int endR = end.red();
        int endG = end.green();
        int endB = end.blue();
        
        int r = (int) (startR + (endR - startR) * ratio);
        int g = (int) (startG + (endG - startG) * ratio);
        int b = (int) (startB + (endB - startB) * ratio);
        
        return TextColor.color(r, g, b);
    }
    
    public static Component createGradientText(String text, TextColor startColor, TextColor endColor) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        
        if (text.length() == 1) {
            return Component.text(text).color(startColor);
        }
        
        Component.Builder builder = Component.text();
        
        for (int i = 0; i < text.length(); i++) {
            float ratio = (float) i / (text.length() - 1);
            TextColor color = interpolateColors(startColor, endColor, ratio);
            builder.append(Component.text(text.charAt(i)).color(color));
        }
        
        return builder.build();
    }
    
    public static boolean isColorSimilar(TextColor color1, TextColor color2, int threshold) {
        if (color1 == null || color2 == null) {
            return false;
        }
        
        int r1 = color1.red(), g1 = color1.green(), b1 = color1.blue();
        int r2 = color2.red(), g2 = color2.green(), b2 = color2.blue();
        
        int deltaR = Math.abs(r1 - r2);
        int deltaG = Math.abs(g1 - g2);
        int deltaB = Math.abs(b1 - b2);
        
        return (deltaR + deltaG + deltaB) <= threshold;
    }
}