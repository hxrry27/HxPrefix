package com.yourserver.playercustomisation.config;

import com.yourserver.playercustomisation.PlayerCustomisation;
import com.yourserver.playercustomisation.gui.MenuUtils;

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
    
    // New prefix system data structures
    private List<String> prefixTypes = new ArrayList<>();
    private Map<String, List<StyleOption>> styleCategories = new LinkedHashMap<>();
    private Map<String, RankPrefixAccess> rankPrefixAccess = new LinkedHashMap<>();
    private final List<PrefixOption> prefixOptions = new ArrayList<>();
    private final List<SuffixOption> suffixOptions = new ArrayList<>();
    
    // New data structures for color groups - MUST BE INITIALIZED
    private Map<String, List<ColorOption>> colorGroups = new LinkedHashMap<>();
    private Map<String, ColorGroupAccess> rankColorAccess = new LinkedHashMap<>();
    private Map<String, UserColorOverride> userOverrides = new LinkedHashMap<>();

    public ConfigManager(PlayerCustomisation plugin) {
        this.plugin = plugin;
        loadAllConfigs();
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
        colorGroups.clear();
        rankColorAccess.clear();
        userOverrides.clear();
        
        // Load main config
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        mainConfig = (YamlConfiguration) plugin.getConfig();
        
        // Load additional config files
        loadMessagesConfig();
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
        solidColors.clear();
        gradients.clear();
        
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
                
                // Populate legacy maps for backward compatibility
                for (ColorOption color : colors) {
                    if (color.type.equals("solid")) {
                        solidColors.put(color.name, (String) color.value);
                    } else if (color.type.equals("gradient")) {
                        if (color.value instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<String> gradientColors = (List<String>) color.value;
                            gradients.put(color.name, gradientColors);
                        }
                    }
                }
            }
        }
        
        // Load rank access
        ConfigurationSection rankAccessSection = nameColorsConfig.getConfigurationSection("rank-access");
        if (rankAccessSection != null) {
            for (String rank : rankAccessSection.getKeys(false)) {
                ConfigurationSection rankSection = rankAccessSection.getConfigurationSection(rank);
                List<String> groups = rankSection.getStringList("groups");
                rankColorAccess.put(rank.toLowerCase(), new ColorGroupAccess(groups));
                plugin.getLogger().info("Loaded rank access: " + rank.toLowerCase() + " -> " + groups);
            }
        }
        
        // Load user overrides
        ConfigurationSection userOverridesSection = nameColorsConfig.getConfigurationSection("user-overrides");
        if (userOverridesSection != null) {
            for (String username : userOverridesSection.getKeys(false)) {
                ConfigurationSection userSection = userOverridesSection.getConfigurationSection(username);
                
                List<String> additionalGroups = userSection.getStringList("additional-groups");
                List<String> overrideGroups = userSection.getStringList("override-groups");
                List<ColorOption> customColors = new ArrayList<>();
                
                ConfigurationSection customColorsSection = userSection.getConfigurationSection("custom-colors");
                if (customColorsSection != null) {
                    for (String colorKey : customColorsSection.getKeys(false)) {
                        ConfigurationSection colorSection = customColorsSection.getConfigurationSection(colorKey);
                        ColorOption color = loadSingleColor(colorSection, defaultMaterial, defaultGlow);
                        if (color != null) {
                            customColors.add(color);
                        }
                    }
                }
                
                userOverrides.put(username.toLowerCase(), 
                    new UserColorOverride(additionalGroups, overrideGroups, customColors));
            }
        }
        
        plugin.getLogger().info("Loaded " + colorGroups.size() + " color groups");
        plugin.getLogger().info("Loaded " + rankColorAccess.size() + " rank access rules");
        plugin.getLogger().info("Loaded " + userOverrides.size() + " user overrides");
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
    
    private ColorOption loadSingleColor(ConfigurationSection colorSection, String defaultMaterial, boolean defaultGlow) {
        if (colorSection == null) return null;
        
        try {
            String name = colorSection.getString("name");
            String type = colorSection.getString("type");
            Object value = colorSection.get("value");
            String materialName = colorSection.getString("material", defaultMaterial);
            boolean glow = colorSection.getBoolean("glow", defaultGlow);
            String description = colorSection.getString("description");
            
            Material material;
            try {
                material = Material.valueOf(materialName.toUpperCase());
            } catch (IllegalArgumentException e) {
                material = Material.valueOf(defaultMaterial);
            }
            
            Map<String, Object> animation = null;
            if (colorSection.contains("animation")) {
                animation = colorSection.getConfigurationSection("animation").getValues(false);
            }
            
            return new ColorOption(name, type, value, material, glow, description, animation);
        } catch (Exception e) {
            plugin.getLogger().warning("Error loading single color: " + e.getMessage());
            return null;
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
        
        // 5. Load specific prefixes
        loadSpecificPrefixes(defaultMaterial, defaultGlow);
        
        plugin.getLogger().info("Total prefix options: " + prefixOptions.size());
    }
    
    private List<StyleOption> loadStyleCategory(String categoryName, ConfigurationSection categoriesSection, 
                                              String defaultMaterial, boolean defaultGlow) {
        List<StyleOption> styles = new ArrayList<>();
        List<Map<?, ?>> styleList = categoriesSection.getMapList(categoryName);
        
        for (Map<?, ?> styleMap : styleList) {
            try {
                String name = (String) styleMap.get("name");
                String format = (String) styleMap.get("format");
                
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
        for (Map.Entry<String, RankPrefixAccess> entry : rankPrefixAccess.entrySet()) {
            String rank = entry.getKey();
            RankPrefixAccess access = entry.getValue();
            
            for (String prefixType : access.prefixes) {
                for (String categoryName : access.categories) {
                    List<StyleOption> styles = styleCategories.get(categoryName);
                    if (styles == null) continue;
                    
                    for (StyleOption style : styles) {
                        // Generate WITHOUT brackets
                        String formattedValue = style.format.replace("{PREFIX}", prefixType);
                        String displayName = prefixType + " " + style.name;
                        
                        PrefixOption existing = findExistingOption(displayName);
                        if (existing != null) {
                            if (!existing.ranks.contains(rank)) {
                                existing.ranks.add(rank);
                            }
                        } else {
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
        
        String defaultMaterialName = suffixConfig.getString("defaults.material", "PAPER");
        
        List<Map<?, ?>> suffixList = suffixConfig.getMapList("suffixes");
        
        if (suffixList == null || suffixList.isEmpty()) {
            plugin.getLogger().warning("No suffixes found in suffix.yml");
            return;
        }
        
        for (Map<?, ?> suffixMap : suffixList) {
            try {
                String value = (String) suffixMap.get("value");
                
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
            
            settings.colors = rankSection.getBoolean("colors", false);
            settings.gradients = rankSection.getBoolean("gradients", false);
            settings.rainbow = rankSection.getBoolean("rainbow", false);
            settings.prefix = rankSection.getBoolean("prefix", false);
            settings.suffix = rankSection.getBoolean("suffix", false);
            settings.nickname = rankSection.getBoolean("nickname", false);
            settings.customTags = rankSection.getBoolean("custom-tags", false);
            
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
                config.set("menu.title", "&6&lName Color Selection &7({rank})");
                config.set("menu.size", 54);
                config.set("solid-colors.Red.hex", "#FF0000");
                config.set("solid-colors.Red.material", "RED_DYE");
                config.set("solid-colors.Blue.hex", "#0000FF");
                config.set("solid-colors.Blue.material", "BLUE_DYE");
                break;
                
            case "prefix.yml":
                config.set("prefix-types", Arrays.asList("PLAYER", "MEMBER", "VIP"));
                break;
                
            case "suffix.yml":
                config.set("suffixes", Arrays.asList("★", "✦", "♦"));
                break;
                
            case "messages.yml":
                config.set("prefix", "&8[&bCustom&8] ");
                config.set("permissions.no-permission", "{prefix}&cYou don't have permission!");
                break;
        }
        
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not create default " + name, e);
        }
    }
    
    // Get available colors for a player
    public List<ColorOption> getAvailableColors(Player player) {
        List<ColorOption> available = new ArrayList<>();
        String username = player.getName().toLowerCase();
        String rank = com.yourserver.playercustomisation.utils.PermissionUtils.getPlayerRank(player).toLowerCase();
        
        // Check user overrides first
        UserColorOverride userOverride = userOverrides.get(username);
        
        List<String> accessibleGroups = new ArrayList<>();
        
        if (userOverride != null && userOverride.overrideGroups != null && !userOverride.overrideGroups.isEmpty()) {
            accessibleGroups = userOverride.overrideGroups;
        } else {
            ColorGroupAccess rankAccess = rankColorAccess.get(rank);
            if (rankAccess != null) {
                if (rankAccess.allGroups) {
                    accessibleGroups.addAll(colorGroups.keySet());
                } else {
                    accessibleGroups.addAll(rankAccess.groups);
                }
            }
            
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
        
        // Add custom colors
        if (userOverride != null) {
            available.addAll(userOverride.customColors);
        }
        
        // Remove duplicates
        Map<String, ColorOption> uniqueColors = new LinkedHashMap<>();
        for (ColorOption color : available) {
            uniqueColors.put(color.name, color);
        }
        
        return new ArrayList<>(uniqueColors.values());
    }
    
    // Get available prefix options for a rank
    public List<PrefixOption> getAvailablePrefixOptions(String rank) {
        List<PrefixOption> available = new ArrayList<>();
        
        for (PrefixOption option : prefixOptions) {
            boolean canUse = false;
            for (String allowedRank : option.ranks) {
                if (allowedRank.equalsIgnoreCase(rank)) {
                    canUse = true;
                    break;
                }
            }
            
            if (canUse && (option.conditional == null || checkCondition(option.conditional))) {
                available.add(option);
            }
        }
        
        return available;
    }

    public List<SuffixOption> getAvailableSuffixOptions(String rank) {
        RankSettings settings = ranks.get(rank.toLowerCase());
        if (settings == null || !settings.suffix) {
            return Collections.emptyList();
        }
        
        List<SuffixOption> available = new ArrayList<>();
        
        for (SuffixOption option : suffixOptions) {
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
                    return (String) color.value;
                }
                break;
        }
        return "<white>";
    }
    
    // Getters for legacy compatibility
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
    
    // Message handling - FIXED
    public String getMessage(String key) {
        String path = key;
        
        // First try messages.yml
        if (messagesConfig != null && messagesConfig.contains(path)) {
            return processMessage(messagesConfig.getString(path));
        }
        
        // Fall back to main config with "messages." prefix
        path = "messages." + key;
        if (mainConfig.contains(path)) {
            return processMessage(mainConfig.getString(path));
        }
        
        // Return a default message
        return MenuUtils.colorize("&cMissing message: " + key);
    }

    private String processMessage(String message) {
        if (message == null) return "";
        
        // Handle prefix replacement
        if (message.contains("{prefix}")) {
            String prefix = "";
            if (messagesConfig != null && messagesConfig.contains("prefix")) {
                prefix = messagesConfig.getString("prefix");
            } else if (mainConfig.contains("messages.prefix")) {
                prefix = mainConfig.getString("messages.prefix");
            }
            
            if (prefix != null) {
                message = message.replace("{prefix}", prefix);
            }
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
        // TODO: Implement conditional logic (dates, events, etc.)
        return true;
    }

    // Menu configuration methods
    public ConfigurationSection getColorMenuConfig() {
        return nameColorsConfig;
    }

    public ConfigurationSection getGradientMenuConfig() {
        return nameColorsConfig;
    }

    public ConfigurationSection getPrefixMenuConfig() {
        return prefixConfig;
    }

    public ConfigurationSection getSuffixMenuConfig() {
        return suffixConfig;
    }

    // Menu titles
    public String getColorMenuTitle(String rank) {
        String title = nameColorsConfig.getString("menu.title", "&6&lName Color Selection &7({rank})");
        return title.replace("{rank}", rank);
    }

    public int getColorMenuSize() {
        return nameColorsConfig.getInt("menu.size", 54);
    }

    public String getPrefixMenuTitle(String rank) {
        String title = prefixConfig.getString("menu.title", "&d&lPrefix Selection &7({rank})");
        return title.replace("{rank}", rank);
    }

    public int getPrefixMenuSize() {
        return prefixConfig.getInt("menu.size", 54);
    }

    public String getSuffixMenuTitle(String rank) {
        String title = suffixConfig.getString("menu.title", "&b&lSuffix Selection &7({rank})");
        return title.replace("{rank}", rank);
    }

    public int getSuffixMenuSize() {
        return suffixConfig.getInt("menu.size", 54);
    }

    // Inner classes - ALL OF THEM
    
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
    
    public static class ColorOption {
        public final String name;
        public final String type;
        public final Object value;
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
            this.groups = groups != null ? new ArrayList<>(groups) : new ArrayList<>();
            this.allGroups = this.groups.contains("*");
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