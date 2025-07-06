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
    private YamlConfiguration messagesConfig;
    
    // New prefix system data structures
    private List<String> prefixTypes = new ArrayList<>();
    private Map<String, List<StyleOption>> styleCategories = new LinkedHashMap<>();
    private Map<String, RankPrefixAccess> rankPrefixAccess = new LinkedHashMap<>();
    private final List<PrefixOption> prefixOptions = new ArrayList<>();
    private final List<SuffixOption> suffixOptions = new ArrayList<>();

    public ConfigManager(PlayerCustomisation plugin) {
        this.plugin = plugin;
        loadAllConfigs();
    }

    // Inner classes for the new system
    public static class StyleOption {
        public final String name;
        public final String format;
        public final Material material;
        public final boolean glow;
        public final String conditional;
        
        public StyleOption(String name, String format, Material material, boolean glow, String conditional) {
            this.name = name;
            this.format = format;
            this.material = material;
            this.glow = glow;
            this.conditional = conditional;
        }
    }
    
    public static class RankPrefixAccess {
        public final List<String> prefixes;
        public final List<String> categories;
        public final boolean customTags;
        
        public RankPrefixAccess(List<String> prefixes, List<String> categories, boolean customTags) {
            this.prefixes = prefixes;
            this.categories = categories;
            this.customTags = customTags;
        }
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

    public static class SuffixOption {
        public final String value;
        public final Material material;
        public final String color;
        public final boolean glow;
        public final List<String> ranks;
        
        public SuffixOption(String value, Material material, String color, 
                        boolean glow, List<String> ranks) {
            this.value = value;
            this.material = material;
            this.color = color;
            this.glow = glow;
            this.ranks = ranks;
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
        loadMessagesConfig();
        
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
        plugin.getLogger().info("- " + prefixOptions.size() + " prefix options");
        plugin.getLogger().info("- " + suffixOptions.size() + " suffix options");
        plugin.getLogger().info("- " + ranks.size() + " ranks configured");
    }
    
    private void loadMessagesConfig() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            saveResource("messages.yml");
        }
        
        messagesConfig = YamlConfiguration.loadConfiguration(file);
        plugin.getLogger().info("- " + countMessages(messagesConfig) + " messages");
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

    private void loadPrefixConfig() {
        File file = new File(plugin.getDataFolder(), "prefix.yml");
        if (!file.exists()) {
            saveResource("prefix.yml");
        }
        
        prefixConfig = YamlConfiguration.loadConfiguration(file);
        prefixOptions.clear();
        prefixTypes.clear();
        styleCategories.clear();
        rankPrefixAccess.clear();
        
        // Get defaults
        String defaultMaterial = prefixConfig.getString("defaults.material", "NAME_TAG");
        boolean defaultGlow = prefixConfig.getBoolean("defaults.glow", false);
        
        // 1. Load prefix types (base words)
        prefixTypes = prefixConfig.getStringList("prefix-types");
        plugin.getLogger().info("Loaded " + prefixTypes.size() + " prefix types");
        
        // 2. Load style categories
        ConfigurationSection styleCategoriesSection = prefixConfig.getConfigurationSection("style-categories");
        if (styleCategoriesSection != null) {
            for (String categoryName : styleCategoriesSection.getKeys(false)) {
                List<StyleOption> styles = loadStyleCategory(categoryName, styleCategoriesSection, defaultMaterial, defaultGlow);
                styleCategories.put(categoryName, styles);
                plugin.getLogger().info("Loaded category '" + categoryName + "' with " + styles.size() + " styles");
            }
        }
        
        // 3. Load rank access rules
        ConfigurationSection rankAccessSection = prefixConfig.getConfigurationSection("rank-access");
        if (rankAccessSection != null) {
            for (String rank : rankAccessSection.getKeys(false)) {
                ConfigurationSection rankSection = rankAccessSection.getConfigurationSection(rank);
                List<String> prefixes = rankSection.getStringList("prefixes");
                List<String> categories = rankSection.getStringList("categories");
                boolean customTags = rankSection.getBoolean("custom-tags", false);
                
                rankPrefixAccess.put(rank.toLowerCase(), new RankPrefixAccess(prefixes, categories, customTags));
            }
        }
        
        // 4. Generate auto combinations
        generateAutoCombinations();
        
        // 5. Load specific prefixes (these are added on top)
        loadSpecificPrefixes(defaultMaterial, defaultGlow);
        
        plugin.getLogger().info("Total prefix options: " + prefixOptions.size());
    }
    
    private List<StyleOption> loadStyleCategory(String categoryName, ConfigurationSection categoriesSection, String defaultMaterial, boolean defaultGlow) {
        List<StyleOption> styles = new ArrayList<>();
        
        // Get the list of styles in this category
        List<Map<?, ?>> styleList = categoriesSection.getMapList(categoryName);
        
        for (Map<?, ?> styleMap : styleList) {
            try {
                String name = (String) styleMap.get("name");
                String format = (String) styleMap.get("format");
                
                // Handle type-safe default values
                String materialName = styleMap.containsKey("material") ? 
                    (String) styleMap.get("material") : defaultMaterial;
                    
                boolean glow = styleMap.containsKey("glow") ? 
                    (Boolean) styleMap.get("glow") : defaultGlow;
                    
                String conditional = (String) styleMap.get("conditional");
                
                Material material;
                try {
                    material = Material.valueOf(materialName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material " + materialName + " for style " + name);
                    material = Material.valueOf(defaultMaterial);
                }
                
                styles.add(new StyleOption(name, format, material, glow, conditional));
            } catch (Exception e) {
                plugin.getLogger().warning("Error loading style in category " + categoryName + ": " + e.getMessage());
            }
        }
        
        return styles;
    }

    private void generateAutoCombinations() {
        // For each rank
        for (Map.Entry<String, RankPrefixAccess> entry : rankPrefixAccess.entrySet()) {
            String rank = entry.getKey();
            RankPrefixAccess access = entry.getValue();
            
            // For each prefix this rank can use
            for (String prefixType : access.prefixes) {
                // For each category this rank can access
                for (String categoryName : access.categories) {
                    List<StyleOption> styles = styleCategories.get(categoryName);
                    if (styles == null) continue;
                    
                    // For each style in this category
                    for (StyleOption style : styles) {
                        // Generate the combination
                        String formattedValue = style.format.replace("{PREFIX}", "[" + prefixType + "]");
                        String displayName = prefixType + " " + style.name;
                        
                        // Check if this combination already exists
                        PrefixOption existing = findExistingOption(displayName);
                        if (existing != null) {
                            // Add this rank to the existing option
                            if (!existing.ranks.contains(rank)) {
                                existing.ranks.add(rank);
                            }
                        } else {
                            // Create new option
                            List<String> ranks = new ArrayList<>();
                            ranks.add(rank);
                            
                            PrefixOption option = new PrefixOption(
                                displayName, 
                                formattedValue, 
                                ranks, 
                                style.material, 
                                style.glow, 
                                style.conditional
                            );
                            prefixOptions.add(option);
                        }
                    }
                }
            }
        }
        
        plugin.getLogger().info("Generated " + prefixOptions.size() + " prefix combinations");
    }
    
    private PrefixOption findExistingOption(String displayName) {
        for (PrefixOption option : prefixOptions) {
            if (option.name.equals(displayName)) {
                return option;
            }
        }
        return null;
    }
    
    private void loadSpecificPrefixes(String defaultMaterial, boolean defaultGlow) {
        List<Map<?, ?>> specificList = prefixConfig.getMapList("specific-prefixes");
        
        if (specificList == null || specificList.isEmpty()) {
            plugin.getLogger().info("No specific prefixes configured");
            return;
        }
        
        int count = 0;
        for (Map<?, ?> prefixMap : specificList) {
            try {
                String name = (String) prefixMap.get("name");
                String value = (String) prefixMap.get("value");
                @SuppressWarnings("unchecked")
                List<String> ranks = (List<String>) prefixMap.get("ranks");
                
                // Handle type-safe default values
                String materialName = prefixMap.containsKey("material") ? 
                    (String) prefixMap.get("material") : defaultMaterial;
                    
                boolean glow = prefixMap.containsKey("glow") ? 
                    (Boolean) prefixMap.get("glow") : defaultGlow;
                    
                String conditional = (String) prefixMap.get("conditional");
                
                Material material;
                try {
                    material = Material.valueOf(materialName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material " + materialName + " for prefix " + name);
                    material = Material.valueOf(defaultMaterial);
                }
                
                // Add to the list
                prefixOptions.add(new PrefixOption(name, value, ranks, material, glow, conditional));
                count++;
            } catch (Exception e) {
                plugin.getLogger().warning("Error loading specific prefix: " + e.getMessage());
            }
        }
        
        plugin.getLogger().info("Loaded " + count + " specific prefixes");
    }
    
    private void loadSuffixConfig() {
        File file = new File(plugin.getDataFolder(), "suffix.yml");
        if (!file.exists()) {
            saveResource("suffix.yml");
        }
        
        suffixConfig = YamlConfiguration.loadConfiguration(file);
        suffixOptions.clear();
        
        // Get default material from config
        String defaultMaterialName = suffixConfig.getString("defaults.material", "PAPER");
        
        if (defaultMaterialName == null) {
            plugin.getLogger().warning("No default material specified in suffix.yml");
            return;
        }
        
        // Load suffix options - they are a LIST
        List<Map<?, ?>> suffixList = suffixConfig.getMapList("suffixes");
        
        if (suffixList == null || suffixList.isEmpty()) {
            plugin.getLogger().warning("No suffixes found in suffix.yml");
            return;
        }
        
        for (Map<?, ?> suffixMap : suffixList) {
            try {
                String value = (String) suffixMap.get("value");
                
                // Handle type-safe default values
                String materialName = suffixMap.containsKey("material") ? 
                    (String) suffixMap.get("material") : defaultMaterialName;
                    
                String color = suffixMap.containsKey("color") ? 
                    (String) suffixMap.get("color") : "&f&l";
                    
                boolean glow = suffixMap.containsKey("glow") ? 
                    (Boolean) suffixMap.get("glow") : false;
                
                @SuppressWarnings("unchecked")
                List<String> ranks = (List<String>) suffixMap.get("ranks");
                
                if (value == null || ranks == null) {
                    plugin.getLogger().warning("Invalid suffix entry - missing required fields");
                    continue;
                }
                
                Material material;
                try {
                    material = Material.valueOf(materialName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material " + materialName + " for suffix " + value);
                    material = Material.valueOf(defaultMaterialName);
                }
                
                suffixOptions.add(new SuffixOption(value, material, color, glow, ranks));
            } catch (Exception e) {
                plugin.getLogger().warning("Error loading suffix: " + e.getMessage());
            }
        }
        
        plugin.getLogger().info("Loaded " + suffixOptions.size() + " suffix options");
    }

    // Get available options for a rank
    public List<PrefixOption> getAvailablePrefixOptions(String rank) {
        List<PrefixOption> available = new ArrayList<>();
        
        plugin.getLogger().info("Getting available prefixes for rank: " + rank);
        
        for (PrefixOption option : prefixOptions) {
            // Check if this rank can use this prefix (case-insensitive)
            boolean canUse = false;
            for (String allowedRank : option.ranks) {
                if (allowedRank.equalsIgnoreCase(rank)) {
                    canUse = true;
                    break;
                }
            }
            
            if (canUse) {
                // Check conditional (for future implementation)
                if (option.conditional == null || checkCondition(option.conditional)) {
                    available.add(option);
                }
            }
        }
        
        plugin.getLogger().info("Found " + available.size() + " available prefixes for rank " + rank);
        return available;
    }

    public List<SuffixOption> getAvailableSuffixOptions(String rank) {
        RankSettings settings = ranks.get(rank.toLowerCase());
        if (settings == null || !settings.suffix) {
            plugin.getLogger().info("Rank " + rank + " has no suffix access");
            return Collections.emptyList();
        }
        
        List<SuffixOption> available = new ArrayList<>();
        
        for (SuffixOption option : suffixOptions) {
            // Check if this rank can use this suffix (case-insensitive)
            boolean canUse = false;
            for (String allowedRank : option.ranks) {
                if (allowedRank.equalsIgnoreCase(rank)) {
                    canUse = true;
                    break;
                }
            }
            
            if (canUse) {
                available.add(option);
            }
        }
        
        return available;
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
                
            case "messages.yml":
                config.set("prefix", "&8[&bCustom&8] ");
                config.set("permissions.no-permission", "{prefix}&cYou don't have permission!");
                config.set("color.changed", "{prefix}&aYour name color has been updated!");
                config.set("color.reset", "{prefix}&aYour name color has been reset!");
                config.set("prefix.changed", "{prefix}&aYour prefix has been set to: {value}");
                config.set("prefix.removed", "{prefix}&aYour prefix has been removed!");
                config.set("suffix.changed", "{prefix}&aYour suffix has been set to: {value}");
                config.set("suffix.removed", "{prefix}&aYour suffix has been removed!");
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
    
    // Message handling
    
    public String getMessage(String key) {
        String path = key;
        
        // First try messages.yml
        if (messagesConfig != null && messagesConfig.contains(path)) {
            String message = messagesConfig.getString(path);
            return processMessage(message);
        }
        
        // Fall back to checking with "messages." prefix in main config
        path = "messages." + key;
        String message = mainConfig.getString(path);
        
        if (message == null) {
            // Return a default message
            return MenuUtils.colorize("&cMissing message: " + key);
        }
        
        return processMessage(message);
    }

    private String processMessage(String message) {
        if (message == null) return "";
        
        // Handle prefix replacement
        if (message.contains("{prefix}")) {
            String prefix = messagesConfig != null ? 
                messagesConfig.getString("prefix", "&8[&bCustom&8] ") :
                mainConfig.getString("messages.prefix", "&8[&bCustom&8] ");
            message = message.replace("{prefix}", prefix);
        }
        
        return MenuUtils.colorize(message);
    }

    private int countMessages(YamlConfiguration config) {
        int count = 0;
        for (String key : config.getKeys(true)) {
            if (!config.isConfigurationSection(key)) {
                count++;
            }
        }
        return count;
    }

    private boolean checkCondition(String condition) {
        // For now, always return true
        // Later: check dates, permissions, etc.
        return true;
    }

    // Additional menu config methods
    public ConfigurationSection getColorMenuConfig() {
        return colorsConfig;
    }

    public ConfigurationSection getGradientMenuConfig() {
        return gradientsConfig;
    }

    public String getColorMenuTitle(String rank) {
        String title = colorsConfig.getString("menu.title", "&6&lName Color Selection &7({rank})");
        return title.replace("{rank}", rank);
    }

    public int getColorMenuSize() {
        return colorsConfig.getInt("menu.size", 54);
    }

    // Prefix menu configuration
    public ConfigurationSection getPrefixMenuConfig() {
        return prefixConfig;
    }

    public String getPrefixMenuTitle(String rank) {
        String title = prefixConfig.getString("menu.title", "&d&lPrefix Selection &7({rank})");
        return title.replace("{rank}", rank);
    }

    public int getPrefixMenuSize() {
        return prefixConfig.getInt("menu.size", 54);
    }

    // Suffix menu configuration
    public ConfigurationSection getSuffixMenuConfig() {
        return suffixConfig;
    }

    public String getSuffixMenuTitle(String rank) {
        String title = suffixConfig.getString("menu.title", "&b&lSuffix Selection &7({rank})");
        return title.replace("{rank}", rank);
    }

    public int getSuffixMenuSize() {
        return suffixConfig.getInt("menu.size", 54);
    }

    // Get color slots configuration
    public List<Integer> getColorSlots() {
        return colorsConfig.getIntegerList("menu.color-slots");
    }

    public List<Integer> getGradientSlots() {
        return gradientsConfig.getIntegerList("menu.gradient-slots");
    }

    // Get animation configuration
    public int getAnimationSpeed(String animationType) {
        return gradientsConfig.getInt("menu.animation." + animationType + ".speed", 5);
    }

    public List<String> getAnimationFrames(String animationType) {
        return gradientsConfig.getStringList("menu.animation." + animationType + ".frames");
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