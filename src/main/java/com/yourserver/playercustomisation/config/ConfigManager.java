package com.yourserver.playercustomisation.config;

import com.yourserver.playercustomisation.PlayerCustomisation;
import com.yourserver.playercustomisation.gui.MenuUtils;
import com.yourserver.playercustomisation.utils.PermissionUtils;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

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
    private YamlConfiguration nameColorsConfig;
    private YamlConfiguration prefixConfig;
    private YamlConfiguration suffixConfig;
    private YamlConfiguration messagesConfig;
    
    // New prefix/color system data structures
    private List<String> prefixTypes = new ArrayList<>();
    private Map<String, List<StyleOption>> styleCategories = new LinkedHashMap<>();
    private Map<String, RankPrefixAccess> rankPrefixAccess = new LinkedHashMap<>();
    private Map<String, List<ColorOption>> colorGroups = new LinkedHashMap<>();
    private Map<String, ColorGroupAccess> rankColorAccess = new LinkedHashMap<>();
    private Map<String, UserColorOverride> userOverrides = new LinkedHashMap<>();
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
        loadNameColorsConfig();
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

    private void loadNameColorsConfig() {
        File file = new File(plugin.getDataFolder(), "namecolors.yml");
        if (!file.exists()) {
            saveResource("namecolors.yml");
        }
        
        nameColorsConfig = YamlConfiguration.loadConfiguration(file);
        colorGroups.clear();
        rankColorAccess.clear();
        userOverrides.clear();
        
        // Get defaults
        String defaultMaterial = nameColorsConfig.getString("defaults.material", "PAPER");
        boolean defaultGlow = nameColorsConfig.getBoolean("defaults.glow", false);
        
        // Load color groups
        ConfigurationSection groupsSection = nameColorsConfig.getConfigurationSection("color-groups");
        if (groupsSection != null) {
            for (String groupName : groupsSection.getKeys(false)) {
                List<ColorOption> colors = loadColorGroup(groupName, groupsSection, defaultMaterial, defaultGlow);
                colorGroups.put(groupName, colors);
                plugin.getLogger().info("Loaded color group '" + groupName + "' with " + colors.size() + " colors");
            }
        }
        
        // Load rank access
        ConfigurationSection rankAccessSection = nameColorsConfig.getConfigurationSection("rank-access");
        if (rankAccessSection != null) {
            for (String rank : rankAccessSection.getKeys(false)) {
                List<String> groups = rankAccessSection.getStringList(rank + ".groups");
                rankColorAccess.put(rank.toLowerCase(), new ColorGroupAccess(groups));
            }
        }
        
        // Load user overrides
        ConfigurationSection userOverridesSection = nameColorsConfig.getConfigurationSection("user-overrides");
        if (userOverridesSection != null) {
            for (String username : userOverridesSection.getKeys(false)) {
                ConfigurationSection userSection = userOverridesSection.getConfigurationSection(username);
                
                List<String> additionalGroups = userSection.getStringList("additional-groups");
                List<String> overrideGroups = userSection.getStringList("override-groups");
                List<ColorOption> customColors = loadCustomColors(userSection.getConfigurationSection("custom-colors"), defaultMaterial, defaultGlow);
                
                userOverrides.put(username.toLowerCase(), 
                    new UserColorOverride(additionalGroups, overrideGroups, customColors));
            }
        }
        
        plugin.getLogger().info("Loaded " + colorGroups.size() + " color groups");
        plugin.getLogger().info("Loaded " + rankColorAccess.size() + " rank access rules");
        plugin.getLogger().info("Loaded " + userOverrides.size() + " user overrides");
    }

    public static class ColorOption {
        public final String name;
        public final String type; // "solid", "gradient", "special"
        public final Object value; // String for solid, List<String> for gradient
        public final Material material;
        public final boolean glow;
        public final String description;
        public final Map<String, Object> animation;
        
        public ColorOption(String name, String type, Object value, Material material, 
                        boolean glow, String description, Map<String, Object> animation) {
            this.name = name;
            this.type = type;
            this.value = value;
            this.material = material;
            this.glow = glow;
            this.description = description;
            this.animation = animation;
        }
    }

    public static class ColorGroupAccess {
        public final List<String> groups;
        public final boolean allGroups;
        
        public ColorGroupAccess(List<String> groups) {
            this.groups = groups;
            this.allGroups = groups.contains("*");
        }
    }

    public static class UserColorOverride {
        public final List<String> additionalGroups;
        public final List<String> overrideGroups;
        public final List<ColorOption> customColors;
        
        public UserColorOverride(List<String> additionalGroups, List<String> overrideGroups, 
                            List<ColorOption> customColors) {
            this.additionalGroups = additionalGroups != null ? additionalGroups : new ArrayList<>();
            this.overrideGroups = overrideGroups;
            this.customColors = customColors != null ? customColors : new ArrayList<>();
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
                        // Generate the combination - NO BRACKETS!
                        String formattedValue = style.format.replace("{PREFIX}", prefixType);
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
            case "namecolors.yml":
                // Basic menu settings
                config.set("menu.title", "&6&lName Color Selection &7({rank})");
                config.set("menu.size", 54);
                
                // Basic solid colors
                config.set("solid-colors.Red.hex", "#FF0000");
                config.set("solid-colors.Red.material", "RED_DYE");
                config.set("solid-colors.Blue.hex", "#0000FF");
                config.set("solid-colors.Blue.material", "BLUE_DYE");
                config.set("solid-colors.Green.hex", "#00FF00");
                config.set("solid-colors.Green.material", "LIME_DYE");
                
                // Basic gradients
                config.set("gradients.Fire.colors", Arrays.asList("#FF0000", "#FFFF00"));
                config.set("gradients.Fire.material", "BLAZE_POWDER");
                config.set("gradients.Ocean.colors", Arrays.asList("#0080FF", "#00FFFF"));
                config.set("gradients.Ocean.material", "PRISMARINE_CRYSTALS");
                break;
                
            case "prefix.yml":
                config.set("prefix-types", Arrays.asList("PLAYER", "MEMBER", "VIP", "ELITE", "PREMIUM"));
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
        return nameColorsConfig;
    }

    public ConfigurationSection getGradientMenuConfig() {
        return nameColorsConfig;
    }

    public String getColorMenuTitle(String rank) {
        String title = nameColorsConfig.getString("menu.title", "&6&lName Color Selection &7({rank})");
        return title.replace("{rank}", rank);
    }

    public int getColorMenuSize() {
        return nameColorsConfig.getInt("menu.size", 54);
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
        return nameColorsConfig.getIntegerList("menu.color-slots");
    }

    public List<Integer> getGradientSlots() {
        return nameColorsConfig.getIntegerList("menu.gradient-slots");
    }

    // Get animation configuration
    public int getAnimationSpeed(String animationType) {
        return nameColorsConfig.getInt("special.rainbow.animation.speed", 5);
    }

    public List<String> getAnimationFrames(String animationType) {
        return nameColorsConfig.getStringList("special.rainbow.animation.frames");
    }
    
    private List<ColorOption> loadColorGroup(String groupName, ConfigurationSection groupsSection, 
                                       String defaultMaterial, boolean defaultGlow) {
    List<ColorOption> colors = new ArrayList<>();
    List<Map<?, ?>> colorList = groupsSection.getMapList(groupName);
    
    for (Map<?, ?> colorMap : colorList) {
        try {
            String name = (String) colorMap.get("name");
            String type = (String) colorMap.get("type");
            Object value = colorMap.get("value");
            
            String materialName = colorMap.containsKey("material") ? 
                (String) colorMap.get("material") : defaultMaterial;
            boolean glow = colorMap.containsKey("glow") ? 
                (Boolean) colorMap.get("glow") : defaultGlow;
            String description = (String) colorMap.get("description");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> animation = (Map<String, Object>) colorMap.get("animation");
            
            Material material;
            try {
                material = Material.valueOf(materialName.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material " + materialName + " for color " + name);
                material = Material.valueOf(defaultMaterial);
            }
            
            colors.add(new ColorOption(name, type, value, material, glow, description, animation));
        } catch (Exception e) {
            plugin.getLogger().warning("Error loading color in group " + groupName + ": " + e.getMessage());
        }
    }
    
    return colors;
}

private List<ColorOption> loadCustomColors(ConfigurationSection section, String defaultMaterial, boolean defaultGlow) {
    List<ColorOption> colors = new ArrayList<>();
    if (section == null) return colors;
    
    for (String key : section.getKeys(false)) {
        ConfigurationSection colorSection = section.getConfigurationSection(key);
        if (colorSection != null) {
            // Load individual color from section
            String name = colorSection.getString("name");
            String type = colorSection.getString("type");
            Object value = colorSection.get("value");
            String materialName = colorSection.getString("material", defaultMaterial);
            boolean glow = colorSection.getBoolean("glow", defaultGlow);
            String description = colorSection.getString("description");
            ConfigurationSection animSection = colorSection.getConfigurationSection("animation");
            
            Material material;
            try {
                material = Material.valueOf(materialName.toUpperCase());
            } catch (IllegalArgumentException e) {
                material = Material.valueOf(defaultMaterial);
            }
            
            Map<String, Object> animation = animSection != null ? animSection.getValues(true) : null;
            
            colors.add(new ColorOption(name, type, value, material, glow, description, animation));
        }
    }
    
    return colors;
}

    // Get available colors for a player
    public List<ColorOption> getAvailableColors(Player player) {
        List<ColorOption> available = new ArrayList<>();
        String username = player.getName().toLowerCase();
        String rank = PermissionUtils.getPlayerRank(player).toLowerCase();
        
        // Check user overrides first
        UserColorOverride userOverride = userOverrides.get(username);
        
        List<String> accessibleGroups = new ArrayList<>();
        
        if (userOverride != null && userOverride.overrideGroups != null) {
            // User has complete override
            accessibleGroups = userOverride.overrideGroups;
        } else {
            // Get rank groups
            ColorGroupAccess rankAccess = rankColorAccess.get(rank);
            if (rankAccess != null) {
                if (rankAccess.allGroups) {
                    // Has access to all groups
                    accessibleGroups.addAll(colorGroups.keySet());
                } else {
                    accessibleGroups.addAll(rankAccess.groups);
                }
            }
            
            // Add any additional groups from user override
            if (userOverride != null) {
                accessibleGroups.addAll(userOverride.additionalGroups);
            }
        }
        
        // Collect colors from accessible groups
        for (String groupName : accessibleGroups) {
            List<ColorOption> groupColors = colorGroups.get(groupName);
            if (groupColors != null) {
                available.addAll(groupColors);
            }
        }
        
        // Add any custom colors for this user
        if (userOverride != null) {
            available.addAll(userOverride.customColors);
        }
        
        // Remove duplicates (by name)
        Map<String, ColorOption> uniqueColors = new LinkedHashMap<>();
        for (ColorOption color : available) {
            uniqueColors.put(color.name, color);
        }
        
        return new ArrayList<>(uniqueColors.values());
    }

    // Helper method to get color value as MiniMessage format
    public static String getColorValue(ColorOption color) {
        switch (color.type) {
            case "solid":
                if (color.value instanceof String) {
                    String hex = (String) color.value;
                    return MenuUtils.hexToMiniMessage(hex);
                }
                break;
                
            case "gradient":
                if (color.value instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> colors = (List<String>) color.value;
                    return MenuUtils.gradientToMiniMessage(colors.toArray(new String[0]));
                }
                break;
                
            case "special":
                if (color.value instanceof String) {
                    return (String) color.value; // Already in MiniMessage format
                }
                break;
        }
        return "<white>"; // Default fallback
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