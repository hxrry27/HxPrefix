package com.yourserver.playercustomisation.config;

import com.yourserver.playercustomisation.PlayerCustomisation;
import com.yourserver.playercustomisation.gui.MenuUtils;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages all configuration files for the plugin
 * Loads colors, gradients, prefixes, suffixes, and rank settings
 */
public class ConfigManager {
    private final PlayerCustomisation plugin;
    
    // Loaded configuration data
    private final Map<String, String> solidColors = new LinkedHashMap<>();
    private final Map<String, List<String>> gradients = new LinkedHashMap<>();
    private final List<String> prefixes = new ArrayList<>();
    private final List<String> suffixes = new ArrayList<>();
    private final Map<String, RankSettings> ranks = new HashMap<>();
    
    // Configuration files
    private YamlConfiguration mainConfig;
    private YamlConfiguration colorsConfig;
    private YamlConfiguration gradientsConfig;
    private YamlConfiguration prefixConfig;
    private YamlConfiguration suffixConfig;
    
    public ConfigManager(PlayerCustomisation plugin) {
        this.plugin = plugin;
        loadAllConfigs();
    }

    public static class PrefixOption {
        public final String name;
        public final String value;
        public final List<String> ranks;
        public final Material material;
        public final boolean glow;
        public final String conditional;
        
        public PrefixOption(String name, String value, List<String> ranks, 
                        Material material, boolean glow, String conditional) {
            this.name = name;
            this.value = value;
            this.ranks = ranks;
            this.material = material;
            this.glow = glow;
            this.conditional = conditional;
        }
    }
    
    /**
     * Loads or reloads all configuration files
     */
    public void loadAllConfigs() {
        // Clear existing data
        solidColors.clear();
        gradients.clear();
        prefixes.clear();
        suffixes.clear();
        ranks.clear();
        
        // Load main config
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        mainConfig = (YamlConfiguration) plugin.getConfig();
        
        // Load additional config files
        loadColorsConfig();
        loadGradientsConfig();
        loadPrefixConfig();
        loadSuffixConfig();
        loadRankSettings();
        
        // Log summary
        plugin.getLogger().info("Configuration loaded:");
        plugin.getLogger().info("- " + solidColors.size() + " solid colors");
        plugin.getLogger().info("- " + gradients.size() + " gradients");
        plugin.getLogger().info("- " + prefixes.size() + " prefixes");
        plugin.getLogger().info("- " + suffixes.size() + " suffixes");
        plugin.getLogger().info("- " + ranks.size() + " ranks configured");
    }
    
    private void loadColorsConfig() {
        File file = new File(plugin.getDataFolder(), "solidcolours.yml");
        if (!file.exists()) {
            saveResource("solidcolours.yml");
        }
        
        colorsConfig = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection colors = colorsConfig.getConfigurationSection("colors");
        
        if (colors != null) {
            for (String key : colors.getKeys(false)) {
                String hex = colors.getString(key);
                if (hex != null) {
                    solidColors.put(key, hex);
                }
            }
        }
    }
    
    private void loadGradientsConfig() {
        File file = new File(plugin.getDataFolder(), "gradients.yml");
        if (!file.exists()) {
            saveResource("gradients.yml");
        }
        
        gradientsConfig = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection grads = gradientsConfig.getConfigurationSection("gradients");
        
        if (grads != null) {
            for (String key : grads.getKeys(false)) {
                List<String> colors = grads.getStringList(key);
                if (!colors.isEmpty()) {
                    gradients.put(key, colors);
                }
            }
        }
    }
    
    private final List<PrefixOption> prefixOptions = new ArrayList<>();

    private void loadPrefixConfig() {
        File file = new File(plugin.getDataFolder(), "prefix.yml");
        if (!file.exists()) {
            saveResource("prefix.yml");
        }
        
        prefixConfig = YamlConfiguration.loadConfiguration(file);
        prefixOptions.clear();
        
        // Load all prefix categories
        loadPrefixCategory("prefixes.supporter-prefixes");
        loadPrefixCategory("prefixes.patron-prefixes");
        loadPrefixCategory("prefixes.devoted-prefixes");
        loadPrefixCategory("prefixes.special-prefixes");
        loadPrefixCategory("prefixes.event-prefixes");
        loadPrefixCategory("prefixes.generic-prefixes");
    }

    private void loadPrefixCategory(String path) {
        ConfigurationSection section = prefixConfig.getConfigurationSection(path);
        if (section == null) return;
        
        for (String key : section.getKeys(false)) {
            ConfigurationSection prefixSection = section.getConfigurationSection(key);
            if (prefixSection == null) continue;
            
            String name = prefixSection.getString("name");
            String value = prefixSection.getString("value");
            List<String> ranks = prefixSection.getStringList("ranks");
            String materialName = prefixSection.getString("material", "NAME_TAG");
            boolean glow = prefixSection.getBoolean("glow", false);
            String conditional = prefixSection.getString("conditional", null);
            
            Material material;
            try {
                material = Material.valueOf(materialName);
            } catch (IllegalArgumentException e) {
                material = Material.NAME_TAG;
            }
            
            prefixOptions.add(new PrefixOption(name, value, ranks, material, glow, conditional));
        }
    }
    
    private void loadSuffixConfig() {
        File file = new File(plugin.getDataFolder(), "suffix.yml");
        if (!file.exists()) {
            saveResource("suffix.yml");
        }
        
        suffixConfig = YamlConfiguration.loadConfiguration(file);
        suffixes.addAll(suffixConfig.getStringList("suffixes"));
    }
    
    private void loadRankSettings() {
        ConfigurationSection ranksSection = mainConfig.getConfigurationSection("ranks");
        if (ranksSection == null) {
            plugin.getLogger().warning("No ranks configured in config.yml!");
            return;
        }
        
        for (String rankName : ranksSection.getKeys(false)) {
            ConfigurationSection rankSection = ranksSection.getConfigurationSection(rankName);
            if (rankSection == null) continue;
            
            RankSettings settings = new RankSettings(rankName);
            
            // Load boolean permissions
            settings.colors = rankSection.getBoolean("colors", false);
            settings.gradients = rankSection.getBoolean("gradients", false);
            settings.rainbow = rankSection.getBoolean("rainbow", false);
            settings.prefix = rankSection.getBoolean("prefix", false);
            settings.suffix = rankSection.getBoolean("suffix", false);
            settings.nickname = rankSection.getBoolean("nickname", false);
            settings.customTags = rankSection.getBoolean("custom-tags", false);
            
            // Load whitelists
            if (rankSection.contains("prefix-whitelist")) {
                settings.prefixWhitelist = new HashSet<>(rankSection.getStringList("prefix-whitelist"));
            }
            if (rankSection.contains("suffix-whitelist")) {
                settings.suffixWhitelist = new HashSet<>(rankSection.getStringList("suffix-whitelist"));
            }
            
            ranks.put(rankName.toLowerCase(), settings);
        }
    }
    
    private void saveResource(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try (InputStream in = plugin.getResource(name)) {
                if (in != null) {
                    Files.copy(in, file.toPath());
                } else {
                    // Create default file if resource doesn't exist
                    createDefaultFile(name);
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Could not save " + name, e);
            }
        }
    }
    
    private void createDefaultFile(String name) {
        File file = new File(plugin.getDataFolder(), name);
        YamlConfiguration config = new YamlConfiguration();
        
        switch (name) {
            case "solidcolours.yml":
                config.set("colors.Red", "#FF0000");
                config.set("colors.Blue", "#0000FF");
                config.set("colors.Green", "#00FF00");
                config.set("colors.Yellow", "#FFFF00");
                config.set("colors.Aqua", "#00FFFF");
                config.set("colors.Pink", "#FF55FF");
                config.set("colors.White", "#FFFFFF");
                break;
                
            case "gradients.yml":
                config.set("gradients.Fire", Arrays.asList("#FF0000", "#FFFF00"));
                config.set("gradients.Ocean", Arrays.asList("#0080FF", "#00FFFF"));
                config.set("gradients.Nature", Arrays.asList("#00FF00", "#FFFF00"));
                break;
                
            case "prefix.yml":
                config.set("prefixes", Arrays.asList("PLAYER", "MEMBER", "VIP", "ELITE", "PREMIUM"));
                break;
                
            case "suffix.yml":
                config.set("suffixes", Arrays.asList("★", "✦", "♦", "✓", "PRO"));
                break;
        }
        
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not create default " + name, e);
        }
    }
    
    // Getter methods for colors and options
    
    public Map<String, String> getSolidColors() {
        return new LinkedHashMap<>(solidColors);
    }
    
    public Map<String, List<String>> getGradients() {
        return new LinkedHashMap<>(gradients);
    }
    
    public List<String> getAllPrefixes() {
        return new ArrayList<>(prefixes);
    }
    
    public List<String> getAllSuffixes() {
        return new ArrayList<>(suffixes);
    }
    
    // Rank permission checks
    
    public boolean canUseColors(String rank) {
        RankSettings settings = ranks.get(rank.toLowerCase());
        return settings != null && settings.colors;
    }
    
    public boolean canUseGradients(String rank) {
        RankSettings settings = ranks.get(rank.toLowerCase());
        return settings != null && settings.gradients;
    }
    
    public boolean canUseRainbow(String rank) {
        RankSettings settings = ranks.get(rank.toLowerCase());
        return settings != null && settings.rainbow;
    }
    
    public boolean canUsePrefix(String rank) {
        RankSettings settings = ranks.get(rank.toLowerCase());
        return settings != null && settings.prefix;
    }
    
    public boolean canUseSuffix(String rank) {
        RankSettings settings = ranks.get(rank.toLowerCase());
        return settings != null && settings.suffix;
    }
    
    public boolean canUseNickname(String rank) {
        RankSettings settings = ranks.get(rank.toLowerCase());
        return settings != null && settings.nickname;
    }
    
    public boolean canUseCustomTags(String rank) {
        RankSettings settings = ranks.get(rank.toLowerCase());
        return settings != null && settings.customTags;
    }
    
    // Get available options for a rank
    
    public List<PrefixOption> getAvailablePrefixOptions(String rank) {
        List<PrefixOption> available = new ArrayList<>();
        
        for (PrefixOption option : prefixOptions) {
            // Check if this rank can use this prefix
            if (option.ranks.contains(rank.toLowerCase())) {
                // Check conditional (for future implementation)
                if (option.conditional == null || checkCondition(option.conditional)) {
                    available.add(option);
                }
            }
        }
        
        return available;
    }
    
    public List<String> getAvailableSuffixes(String rank) {
        RankSettings settings = ranks.get(rank.toLowerCase());
        if (settings == null || !settings.suffix) {
            return Collections.emptyList();
        }
        
        // Check whitelist
        if (settings.suffixWhitelist != null && !settings.suffixWhitelist.isEmpty()) {
            List<String> available = new ArrayList<>();
            for (String suffix : suffixes) {
                if (settings.suffixWhitelist.contains(suffix)) {
                    available.add(suffix);
                }
            }
            return available;
        }
        
        // No whitelist = all suffixes
        return new ArrayList<>(suffixes);
    }
    
    // Message handling
    
    public String getMessage(String key) {
        String path = "messages." + key;
        String message = mainConfig.getString(path);
        
        if (message == null) {
            // Return a default message
            return MenuUtils.colorize("&8[&bCustom&8] &cMissing message: " + key);
        }
        
        // Handle prefix replacement without recursion
        if (message.contains("{prefix}")) {
            // Get prefix directly from config, not through getMessage
            String prefix = mainConfig.getString("messages.prefix", "&8[&bCustom&8] ");
            message = message.replace("{prefix}", prefix);
        }
    
        return MenuUtils.colorize(message);
    }

    private boolean checkCondition(String condition) {
        // For now, always return true
        // Later: check dates, permissions, etc.
        return true;
    }
    
    // Inner class for rank settings
    private static class RankSettings {
        final String name;
        boolean colors;
        boolean gradients;
        boolean rainbow;
        boolean prefix;
        boolean suffix;
        boolean nickname;
        boolean customTags;
        Set<String> prefixWhitelist;
        Set<String> suffixWhitelist;
        
        RankSettings(String name) {
            this.name = name;
        }
    }
}