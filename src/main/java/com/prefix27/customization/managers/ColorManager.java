package com.prefix27.customization.managers;

import com.prefix27.customization.PlayerCustomizationPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ColorManager {
    
    private final PlayerCustomizationPlugin plugin;
    private final Map<String, TextColor> colorMap;
    private final Pattern hexPattern;
    
    public ColorManager(PlayerCustomizationPlugin plugin) {
        this.plugin = plugin;
        this.colorMap = new HashMap<>();
        this.hexPattern = Pattern.compile("^#[0-9A-Fa-f]{6}$");
        
        initializeColors();
    }
    
    private void initializeColors() {
        // Set up our color name mappings
        colorMap.put("red", NamedTextColor.RED);
        colorMap.put("blue", NamedTextColor.BLUE);
        colorMap.put("green", NamedTextColor.GREEN);
        colorMap.put("yellow", NamedTextColor.YELLOW);
        colorMap.put("purple", NamedTextColor.DARK_PURPLE);
        colorMap.put("orange", NamedTextColor.GOLD);
        colorMap.put("pink", NamedTextColor.LIGHT_PURPLE);
        colorMap.put("cyan", NamedTextColor.AQUA);
        colorMap.put("white", NamedTextColor.WHITE);
        colorMap.put("gray", NamedTextColor.GRAY);
        colorMap.put("black", NamedTextColor.BLACK);
        colorMap.put("lime", NamedTextColor.GREEN);
        colorMap.put("gold", NamedTextColor.GOLD);
        colorMap.put("aqua", NamedTextColor.AQUA);
        colorMap.put("dark_red", NamedTextColor.DARK_RED);
        colorMap.put("dark_blue", NamedTextColor.DARK_BLUE);
        colorMap.put("dark_green", NamedTextColor.DARK_GREEN);
        colorMap.put("dark_purple", NamedTextColor.DARK_PURPLE);
        colorMap.put("dark_aqua", NamedTextColor.DARK_AQUA);
        colorMap.put("dark_gray", NamedTextColor.DARK_GRAY);
    }
    
    public List<String> getBasicColors() {
        return plugin.getConfig().getStringList("colors.basic");
    }
    
    public List<String> getPresetGradients() {
        return plugin.getConfig().getStringList("colors.gradients.preset");
    }
    
    public TextColor getColor(String colorName) {
        if (colorName == null) return null;
        
        // Check if it's a hex color
        if (hexPattern.matcher(colorName).matches()) {
            return TextColor.fromHexString(colorName);
        }
        
        // Check named colors
        return colorMap.get(colorName.toLowerCase());
    }
    
    public Component applyColor(String text, String colorName) {
        TextColor color = getColor(colorName);
        if (color != null) {
            return Component.text(text).color(color);
        }
        return Component.text(text);
    }
    
    public Component applyGradient(String text, String gradientString) {
        if (gradientString == null || !gradientString.contains(":")) {
            return Component.text(text);
        }
        
        String[] colors = gradientString.split(":");
        if (colors.length != 2) {
            return Component.text(text);
        }
        
        TextColor startColor = getColor(colors[0]);
        TextColor endColor = getColor(colors[1]);
        
        if (startColor == null || endColor == null) {
            return Component.text(text);
        }
        
        return createGradientText(text, startColor, endColor);
    }
    
    private Component createGradientText(String text, TextColor startColor, TextColor endColor) {
        if (text.length() <= 1) {
            return Component.text(text).color(startColor);
        }
        
        var builder = Component.text();
        
        for (int i = 0; i < text.length(); i++) {
            float ratio = (float) i / (text.length() - 1);
            TextColor interpolatedColor = interpolateColor(startColor, endColor, ratio);
            builder.append(Component.text(text.charAt(i)).color(interpolatedColor));
        }
        
        return builder.build();
    }
    
    private TextColor interpolateColor(TextColor start, TextColor end, float ratio) {
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
    
    public Component createRainbowText(String text) {
        var builder = Component.text();
        TextColor[] rainbowColors = {
            NamedTextColor.RED,
            NamedTextColor.GOLD,
            NamedTextColor.YELLOW,
            NamedTextColor.GREEN,
            NamedTextColor.BLUE,
            NamedTextColor.DARK_PURPLE,
            NamedTextColor.LIGHT_PURPLE
        };
        
        for (int i = 0; i < text.length(); i++) {
            TextColor color = rainbowColors[i % rainbowColors.length];
            builder.append(Component.text(text.charAt(i)).color(color));
        }
        
        return builder.build();
    }
    
    public boolean isValidColor(String colorName) {
        return getColor(colorName) != null;
    }
    
    public boolean isValidGradient(String gradientString) {
        if (gradientString == null || !gradientString.contains(":")) {
            return false;
        }
        
        String[] colors = gradientString.split(":");
        if (colors.length != 2) {
            return false;
        }
        
        return isValidColor(colors[0]) && isValidColor(colors[1]);
    }
    
    public String getColorName(TextColor color) {
        for (Map.Entry<String, TextColor> entry : colorMap.entrySet()) {
            if (entry.getValue().equals(color)) {
                return entry.getKey();
            }
        }
        
        // Return hex if it's not a named color
        return color.asHexString();
    }
    
    public Component formatPlayerName(String name, String color, String gradient) {
        if (gradient != null && !gradient.isEmpty()) {
            return applyGradient(name, gradient);
        } else if (color != null && !color.isEmpty()) {
            if (color.equals("rainbow")) {
                return createRainbowText(name);
            } else {
                return applyColor(name, color);
            }
        } else {
            return Component.text(name);
        }
    }
    
    public String previewColor(String playerName, String color) {
        if (color == null || color.isEmpty()) {
            return playerName;
        }
        
        Component component = applyColor(playerName, color);
        return component.toString(); // This is a simplified preview
    }
    
    public String previewGradient(String playerName, String gradient) {
        if (gradient == null || gradient.isEmpty()) {
            return playerName;
        }
        
        Component component = applyGradient(playerName, gradient);
        return component.toString(); // This is a simplified preview
    }
    
    public void reload() {
        // Reload color configuration if needed
        colorMap.clear();
        initializeColors();
    }
}