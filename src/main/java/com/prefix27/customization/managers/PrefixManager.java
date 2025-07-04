package com.prefix27.customization.managers;

import com.prefix27.customization.PlayerCustomizationPlugin;
import com.prefix27.customization.database.PlayerData;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PrefixManager {
    
    private final PlayerCustomizationPlugin plugin;
    private final Map<String, PrefixDefinition> prefixDefinitions;
    private final List<String> forbiddenWords;
    
    public PrefixManager(PlayerCustomizationPlugin plugin) {
        this.plugin = plugin;
        this.prefixDefinitions = new HashMap<>();
        this.forbiddenWords = new ArrayList<>();
        
        loadPrefixDefinitions();
    }
    
    private void loadPrefixDefinitions() {
        ConfigurationSection prefixConfig = plugin.getConfig().getConfigurationSection("prefixes");
        if (prefixConfig != null) {
            for (String rank : prefixConfig.getKeys(false)) {
                ConfigurationSection rankSection = prefixConfig.getConfigurationSection(rank);
                if (rankSection != null) {
                    String baseText = rankSection.getString("base_text", "[" + rank + "]");
                    List<String> colors = rankSection.getStringList("colors");
                    List<String> gradients = rankSection.getStringList("gradients");
                    boolean customAllowed = rankSection.getBoolean("custom_allowed", false);
                    
                    PrefixDefinition definition = new PrefixDefinition(rank, baseText, colors, gradients, customAllowed);
                    prefixDefinitions.put(rank, definition);
                }
            }
        }
        
        // Load event prefixes
        ConfigurationSection eventConfig = plugin.getConfig().getConfigurationSection("event_prefixes");
        if (eventConfig != null) {
            for (String eventName : eventConfig.getKeys(false)) {
                ConfigurationSection eventSection = eventConfig.getConfigurationSection(eventName);
                if (eventSection != null) {
                    String text = eventSection.getString("text", "[" + eventName + "]");
                    List<String> colors = eventSection.getStringList("colors");
                    List<String> availableDates = eventSection.getStringList("available_dates");
                    
                    PrefixDefinition definition = new PrefixDefinition("event_" + eventName, text, colors, new ArrayList<>(), false);
                    definition.setEventPrefix(true);
                    definition.setAvailableDates(availableDates);
                    prefixDefinitions.put("event_" + eventName, definition);
                }
            }
        }
        
        // Load forbidden words
        forbiddenWords.addAll(plugin.getConfig().getStringList("custom_prefixes.forbidden_words"));
    }
    
    public List<String> getAvailablePrefixes(UUID uuid) {
        List<String> availablePrefixes = new ArrayList<>();
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(uuid);
        
        if (playerData == null) {
            return availablePrefixes;
        }
        
        String playerRank = playerData.getRank();
        PrefixDefinition rankDefinition = prefixDefinitions.get(playerRank);
        
        if (rankDefinition != null) {
            // Add rank-based prefix color variants
            List<String> availableColors = getAvailableColors(uuid, playerRank);
            for (String color : availableColors) {
                availablePrefixes.add(playerRank + "_" + color);
            }
            
            // Add rank-based prefix gradient variants for patron+ ranks
            if (playerRank.equals("patron") || playerRank.equals("devoted")) {
                List<String> availableGradients = getAvailableGradients(uuid, playerRank);
                for (String gradient : availableGradients) {
                    availablePrefixes.add(playerRank + "_gradient_" + gradient.replace(":", "_to_"));
                }
            }
            
            // Add event prefixes if they're currently available
            for (Map.Entry<String, PrefixDefinition> entry : prefixDefinitions.entrySet()) {
                if (entry.getValue().isEventPrefix() && isEventPrefixAvailable(entry.getValue())) {
                    String eventKey = entry.getKey();
                    PrefixDefinition eventDef = entry.getValue();
                    
                    // Add color variants for event prefixes
                    for (String color : eventDef.getColors()) {
                        availablePrefixes.add(eventKey + "_" + color);
                    }
                }
            }
            
            // Add custom prefixes for devoted players
            if (rankDefinition.isCustomAllowed()) {
                // Need to implement database lookup for approved custom prefixes
            }
        }
        
        return availablePrefixes;
    }
    
    public List<String> getAvailableColors(UUID uuid, String prefixId) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(uuid);
        if (playerData == null) {
            return new ArrayList<>();
        }
        
        PrefixDefinition definition = prefixDefinitions.get(prefixId);
        if (definition == null) {
            return new ArrayList<>();
        }
        
        List<String> colors = definition.getColors();
        if (colors.contains("all")) {
            return plugin.getColorManager().getBasicColors();
        }
        
        return colors;
    }
    
    public List<String> getAvailableGradients(UUID uuid, String prefixId) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(uuid);
        if (playerData == null) {
            return new ArrayList<>();
        }
        
        String playerRank = playerData.getRank();
        if (!playerRank.equals("patron") && !playerRank.equals("devoted")) {
            return new ArrayList<>();
        }
        
        PrefixDefinition definition = prefixDefinitions.get(prefixId);
        if (definition == null) {
            return new ArrayList<>();
        }
        
        List<String> gradients = definition.getGradients();
        if (gradients.contains("all")) {
            return plugin.getColorManager().getPresetGradients();
        }
        
        return gradients;
    }
    
    public Component formatPrefix(String fullPrefixId, String overrideColor, String overrideGradient) {
        // Parse the full prefix ID (e.g., "supporter_red", "patron_gradient_red_to_blue", "event_pride_rainbow")
        String basePrefixId;
        String embeddedColor = null;
        String embeddedGradient = null;
        
        if (fullPrefixId.contains("_gradient_")) {
            String[] parts = fullPrefixId.split("_gradient_", 2);
            basePrefixId = parts[0];
            if (parts.length > 1) {
                embeddedGradient = parts[1].replace("_to_", ":");
            }
        } else if (fullPrefixId.contains("_")) {
            String[] parts = fullPrefixId.split("_", 2);
            basePrefixId = parts[0];
            if (parts.length > 1 && !parts[1].equals("gradient")) {
                embeddedColor = parts[1];
            }
        } else {
            basePrefixId = fullPrefixId;
        }
        
        PrefixDefinition definition = prefixDefinitions.get(basePrefixId);
        if (definition == null) {
            return Component.text("[Unknown]");
        }
        
        String text = definition.getBaseText().toUpperCase(); // Make all prefixes uppercase by default
        
        // Use override values if provided, otherwise use embedded values
        String useColor = overrideColor != null ? overrideColor : embeddedColor;
        String useGradient = overrideGradient != null ? overrideGradient : embeddedGradient;
        
        if (useGradient != null && !useGradient.isEmpty()) {
            return plugin.getColorManager().applyGradient(text, useGradient);
        } else if (useColor != null && !useColor.isEmpty()) {
            if (useColor.equals("rainbow")) {
                return plugin.getColorManager().createRainbowText(text);
            } else {
                return plugin.getColorManager().applyColor(text, useColor);
            }
        } else {
            // Default to white color for prefixes when no color specified
            return plugin.getColorManager().applyColor(text, "white");
        }
    }
    
    public String previewPrefix(String fullPrefixId, String overrideColor, String overrideGradient, String playerName) {
        Component prefixComponent = formatPrefix(fullPrefixId, overrideColor, overrideGradient);
        Component nameComponent = Component.text(playerName);
        
        Component fullComponent = prefixComponent.append(Component.text(" ")).append(nameComponent).append(Component.text(": Hello world!"));
        return fullComponent.toString(); // Simplified preview - would need proper Adventure text serialization for actual display
    }
    
    public boolean canUsePrefix(UUID uuid, String prefixId) {
        List<String> availablePrefixes = getAvailablePrefixes(uuid);
        return availablePrefixes.contains(prefixId);
    }
    
    public boolean canUseColor(UUID uuid, String prefixId, String color) {
        List<String> availableColors = getAvailableColors(uuid, prefixId);
        return availableColors.contains(color) || availableColors.contains("all");
    }
    
    public boolean canUseGradient(UUID uuid, String prefixId, String gradient) {
        List<String> availableGradients = getAvailableGradients(uuid, prefixId);
        return availableGradients.contains(gradient) || availableGradients.contains("all");
    }
    
    public boolean isValidCustomPrefix(String prefixText) {
        if (prefixText == null || prefixText.trim().isEmpty()) {
            return false;
        }
        
        int maxLength = plugin.getConfig().getInt("custom_prefixes.max_length", 16);
        int minLength = plugin.getConfig().getInt("custom_prefixes.min_length", 1);
        
        if (prefixText.length() > maxLength || prefixText.length() < minLength) {
            return false;
        }
        
        // Check for forbidden words
        String lowerPrefix = prefixText.toLowerCase();
        for (String forbiddenWord : forbiddenWords) {
            if (lowerPrefix.contains(forbiddenWord.toLowerCase())) {
                return false;
            }
        }
        
        return true;
    }
    
    public boolean canRequestCustomPrefix(UUID uuid) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(uuid);
        if (playerData == null) {
            return false;
        }
        
        String playerRank = playerData.getRank();
        PrefixDefinition definition = prefixDefinitions.get(playerRank);
        
        if (definition == null || !definition.isCustomAllowed()) {
            return false;
        }
        
        // Check cooldown
        int cooldownDays = plugin.getConfig().getInt("custom_prefixes.cooldown_days", 30);
        // Will need to check database for last request date
        
        return true;
    }
    
    public void requestCustomPrefix(UUID uuid, String prefixText) {
        // Will implement database insertion for custom_prefix_requests table
        // For now, just log it
        plugin.getLogger().info("Custom prefix request from " + uuid + ": " + prefixText);
    }
    
    private boolean isEventPrefixAvailable(PrefixDefinition definition) {
        if (!definition.isEventPrefix()) {
            return false;
        }
        
        List<String> availableDates = definition.getAvailableDates();
        if (availableDates.isEmpty()) {
            return true; // Available all year
        }
        
        // For testing purposes, make all event prefixes available
        // In production, this would check the current date
        return true;
        
        // String currentMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM")).toLowerCase();
        // return availableDates.contains(currentMonth);
    }
    
    public void reload() {
        prefixDefinitions.clear();
        forbiddenWords.clear();
        loadPrefixDefinitions();
    }
    
    public List<String> getAvailablePrefixesForRank(String rank) {
        List<String> available = new ArrayList<>();
        
        // Always include the rank-based prefix if it exists
        if (prefixDefinitions.containsKey(rank.toLowerCase())) {
            available.add(rank.toLowerCase());
        }
        
        // Add event prefixes that might be available
        // TODO: Add logic for event prefixes based on date/season
        
        return available;
    }
    
    public boolean hasAccessToPrefix(String rank, String prefixId) {
        // Players can access their own rank prefix
        if (prefixId.equals(rank.toLowerCase())) {
            return true;
        }
        
        // TODO: Add logic for event prefixes and special unlockables
        
        return false;
    }
    
    // Inner class for prefix definitions
    public static class PrefixDefinition {
        private final String id;
        private final String baseText;
        private final List<String> colors;
        private final List<String> gradients;
        private final boolean customAllowed;
        private boolean eventPrefix;
        private List<String> availableDates;
        
        public PrefixDefinition(String id, String baseText, List<String> colors, List<String> gradients, boolean customAllowed) {
            this.id = id;
            this.baseText = baseText;
            this.colors = colors;
            this.gradients = gradients;
            this.customAllowed = customAllowed;
            this.eventPrefix = false;
            this.availableDates = new ArrayList<>();
        }
        
        // Getters
        public String getId() { return id; }
        public String getBaseText() { return baseText; }
        public List<String> getColors() { return colors; }
        public List<String> getGradients() { return gradients; }
        public boolean isCustomAllowed() { return customAllowed; }
        public boolean isEventPrefix() { return eventPrefix; }
        public List<String> getAvailableDates() { return availableDates; }
        
        // Setters
        public void setEventPrefix(boolean eventPrefix) { this.eventPrefix = eventPrefix; }
        public void setAvailableDates(List<String> availableDates) { this.availableDates = availableDates; }
    }
}