package dev.hxrry.hxprefix.config;

import dev.hxrry.hxcore.utils.Log;

import dev.hxrry.hxprefix.HxPrefix;
import dev.hxrry.hxprefix.api.models.StyleOption;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * handles style configurations (colours, prefixes, suffixes)
 * redone thread-safe implementation using ConcurrentHashMap
 */
public class StyleConfig {
    @SuppressWarnings("unused")
    private final HxPrefix plugin;
    private final File stylesFolder;

    private final Map<String, StyleOption> colours = new ConcurrentHashMap<>();
    private final Map<String, StyleOption> prefixes = new ConcurrentHashMap<>();
    private final Map<String, StyleOption> suffixes = new ConcurrentHashMap<>();
    
    private final Map<String, List<String>> colourAccess = new ConcurrentHashMap<>();
    private final Map<String, List<String>> prefixAccess = new ConcurrentHashMap<>();
    private final Map<String, List<String>> suffixAccess = new ConcurrentHashMap<>();
    
    // lock for reload operations to prevent concurrent modifications
    private final Object reloadLock = new Object();
    
    public StyleConfig(@NotNull HxPrefix plugin, @NotNull File stylesFolder) {
        this.plugin = plugin;
        this.stylesFolder = stylesFolder;
    }
    
    /**
     * load all style configurations
     */
    public void load() {
        synchronized (reloadLock) {
            // create temporary maps to load into
            Map<String, StyleOption> tempColours = new ConcurrentHashMap<>();
            Map<String, StyleOption> tempPrefixes = new ConcurrentHashMap<>();
            Map<String, StyleOption> tempSuffixes = new ConcurrentHashMap<>();
            Map<String, List<String>> tempColourAccess = new ConcurrentHashMap<>();
            Map<String, List<String>> tempPrefixAccess = new ConcurrentHashMap<>();
            Map<String, List<String>> tempSuffixAccess = new ConcurrentHashMap<>();
            
            // load into temporary maps
            loadColours(tempColours, tempColourAccess);
            loadPrefixes(tempPrefixes, tempPrefixAccess);
            loadSuffixes(tempSuffixes, tempSuffixAccess);
            
            // atomically replace all maps at once
            colours.clear();
            colours.putAll(tempColours);
            
            prefixes.clear();
            prefixes.putAll(tempPrefixes);
            
            suffixes.clear();
            suffixes.putAll(tempSuffixes);
            
            colourAccess.clear();
            colourAccess.putAll(tempColourAccess);
            
            prefixAccess.clear();
            prefixAccess.putAll(tempPrefixAccess);
            
            suffixAccess.clear();
            suffixAccess.putAll(tempSuffixAccess);
            
            Log.info("loaded " + colours.size() + " colours, " + 
                    prefixes.size() + " prefixes, " + 
                    suffixes.size() + " suffixes");
        }
    }
    
    /**
     * load colour configurations into provided maps
     */
    private void loadColours(Map<String, StyleOption> targetColours, 
                            Map<String, List<String>> targetAccess) {
        File file = new File(stylesFolder, "colours.yml");
        if (!file.exists()) {
            Log.warning("colours.yml not found");
            return;
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        // load colour groups
        ConfigurationSection groupsSection = config.getConfigurationSection("colour-groups");
        if (groupsSection != null) {
            for (String groupName : groupsSection.getKeys(false)) {
                loadColourGroup(groupName, groupsSection.getConfigurationSection(groupName), targetColours);
            }
        }
        
        // load rank access
        ConfigurationSection accessSection = config.getConfigurationSection("rank-access");
        if (accessSection != null) {
            for (String rank : accessSection.getKeys(false)) {
                List<String> groups = accessSection.getStringList(rank + ".groups");
                List<String> colourIds = new ArrayList<>();
                
                // expand groups to colour ids
                for (String group : groups) {
                    if (group.equals("*")) {
                        // all colours
                        colourIds.addAll(targetColours.keySet());
                    } else {
                        // specific group
                        colourIds.addAll(getColourIdsByGroup(group, targetColours));
                    }
                }
                
                targetAccess.put(rank.toLowerCase(), Collections.unmodifiableList(colourIds));
            }
        }
    }
    
    /**
     * load a colour group into provided map
     */
    private void loadColourGroup(@NotNull String groupName, @Nullable ConfigurationSection section,
                                 Map<String, StyleOption> targetColours) {
        if (section == null) return;
        
        for (String colourKey : section.getKeys(false)) {
            ConfigurationSection colourSection = section.getConfigurationSection(colourKey);
            if (colourSection == null) continue;
            
            String id = groupName + "." + colourKey;
            String displayName = colourSection.getString("name", colourKey);
            String value = colourSection.getString("value", "");
            String typeStr = colourSection.getString("type", "solid");
            
            StyleOption.Type type = switch (typeStr) {
                case "gradient" -> StyleOption.Type.COLOUR_GRADIENT;
                case "special" -> StyleOption.Type.COLOUR_SPECIAL;
                default -> StyleOption.Type.COLOUR_SOLID;
            };
            
            // get material
            String materialName = colourSection.getString("material", "PAPER");
            Material material;
            try {
                material = Material.valueOf(materialName.toUpperCase());
            } catch (IllegalArgumentException e) {
                Log.warning("invalid material " + materialName + " for colour " + id);
                material = Material.PAPER;
            }
            
            // get other properties
            boolean glow = colourSection.getBoolean("glow", false);
            List<String> description = colourSection.getStringList("description");
            
            // get metadata
            Map<String, Object> metadata = new HashMap<>();
            if (colourSection.contains("animation")) {
                metadata.put("animation", colourSection.getConfigurationSection("animation").getValues(true));
            }
            if (colourSection.contains("seasonal")) {
                metadata.put("seasonal", colourSection.getString("seasonal"));
            }
            
            // create the option
            StyleOption option = new StyleOption(
                id, displayName, type, value, material, glow,
                description, new ArrayList<>(), metadata
            );
            
            targetColours.put(id, option);
        }
    }
    
    /**
     * load prefix configurations into provided maps
     */
    private void loadPrefixes(Map<String, StyleOption> targetPrefixes,
                             Map<String, List<String>> targetAccess) {
        File file = new File(stylesFolder, "prefixes.yml");
        if (!file.exists()) {
            Log.warning("prefixes.yml not found");
            return;
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        // load prefix options
        ConfigurationSection optionsSection = config.getConfigurationSection("prefix-options");
        if (optionsSection != null) {
            for (String prefixKey : optionsSection.getKeys(false)) {
                ConfigurationSection prefixSection = optionsSection.getConfigurationSection(prefixKey);
                if (prefixSection == null) continue;
                
                String id = prefixKey;
                String displayName = prefixSection.getString("name", prefixKey);
                String value = prefixSection.getString("value", "");
                
                // get material
                String materialName = prefixSection.getString("material", "NAME_TAG");
                Material material;
                try {
                    material = Material.valueOf(materialName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    Log.warning("invalid material " + materialName + " for prefix " + id);
                    material = Material.NAME_TAG;
                }
                
                // get properties
                boolean glow = prefixSection.getBoolean("glow", false);
                List<String> ranks = prefixSection.getStringList("ranks");
                
                // get metadata
                Map<String, Object> metadata = new HashMap<>();
                if (prefixSection.contains("seasonal")) {
                    metadata.put("seasonal", prefixSection.getString("seasonal"));
                }
                
                StyleOption option = new StyleOption(
                    id, displayName, StyleOption.Type.PREFIX, value,
                    material, glow, null, ranks, metadata
                );
                
                targetPrefixes.put(id, option);
            }
        }
        
        // load rank access
        ConfigurationSection accessSection = config.getConfigurationSection("rank-access");
        if (accessSection != null) {
            for (String rank : accessSection.getKeys(false)) {
                List<String> prefixIds = accessSection.getStringList(rank);
                targetAccess.put(rank.toLowerCase(), Collections.unmodifiableList(prefixIds));
            }
        }
    }
    
    /**
     * load suffix configurations into provided maps
     */
    private void loadSuffixes(Map<String, StyleOption> targetSuffixes,
                             Map<String, List<String>> targetAccess) {
        File file = new File(stylesFolder, "suffixes.yml");
        if (!file.exists()) {
            Log.warning("suffixes.yml not found");
            return;
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        // load suffix options
        ConfigurationSection optionsSection = config.getConfigurationSection("suffix-options");
        if (optionsSection != null) {
            for (String suffixKey : optionsSection.getKeys(false)) {
                ConfigurationSection suffixSection = optionsSection.getConfigurationSection(suffixKey);
                if (suffixSection == null) continue;
                
                String id = suffixKey;
                String displayName = suffixSection.getString("name", suffixKey);
                String value = suffixSection.getString("value", "");
                
                // get material
                String materialName = suffixSection.getString("material", "PAPER");
                Material material;
                try {
                    material = Material.valueOf(materialName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    Log.warning("invalid material " + materialName + " for suffix " + id);
                    material = Material.PAPER;
                }
                
                // get properties
                boolean glow = suffixSection.getBoolean("glow", false);
                List<String> ranks = suffixSection.getStringList("ranks");
                
                // add colour to value if specified
                String colour = suffixSection.getString("colour");
                if (colour != null) {
                    value = colour + value;
                }
                
                StyleOption option = new StyleOption(
                    id, displayName, StyleOption.Type.SUFFIX, value,
                    material, glow, null, ranks, null
                );
                
                targetSuffixes.put(id, option);
            }
        }
        
        // load rank access
        ConfigurationSection accessSection = config.getConfigurationSection("rank-access");
        if (accessSection != null) {
            for (String rank : accessSection.getKeys(false)) {
                List<String> suffixIds = accessSection.getStringList(rank);
                targetAccess.put(rank.toLowerCase(), Collections.unmodifiableList(suffixIds));
            }
        }
    }
    
    /**
     * get colour ids by group name from provided map
     */
    private List<String> getColourIdsByGroup(@NotNull String groupName, 
                                             Map<String, StyleOption> sourceColours) {
        return sourceColours.keySet().stream()
            .filter(id -> id.startsWith(groupName + "."))
            .collect(Collectors.toList());
    }
    
    /**
     * get available colours for a rank
     */
    @NotNull
    public List<StyleOption> getAvailableColours(@NotNull String rank) {
        List<String> colourIds = colourAccess.get(rank.toLowerCase());
        if (colourIds == null) {
            return Collections.emptyList();
        }
        
        return colourIds.stream()
            .map(colours::get)
            .filter(Objects::nonNull)
            .map(opt -> opt.withRanks(List.of(rank)))
            .collect(Collectors.toList());
    }
    
    /**
     * get available prefixes for a rank
     */
    @NotNull
    public List<StyleOption> getAvailablePrefixes(@NotNull String rank) {
        // check by rank list in each prefix
        return prefixes.values().stream()
            .filter(opt -> opt.isAllowedForRank(rank))
            .collect(Collectors.toList());
    }
    
    /**
     * get available suffixes for a rank
     */
    @NotNull
    public List<StyleOption> getAvailableSuffixes(@NotNull String rank) {
        // check by rank list in each suffix
        return suffixes.values().stream()
            .filter(opt -> opt.isAllowedForRank(rank))
            .collect(Collectors.toList());
    }
    
    /**
     * get a specific colour by id
     */
    @Nullable
    public StyleOption getColour(@NotNull String id) {
        return colours.get(id);
    }
    
    /**
     * get a specific prefix by id
     */
    @Nullable
    public StyleOption getPrefix(@NotNull String id) {
        return prefixes.get(id);
    }
    
    /**
     * get a specific suffix by id
     */
    @Nullable
    public StyleOption getSuffix(@NotNull String id) {
        return suffixes.get(id);
    }
    
    /**
     * format text with a colour value
     */
    @NotNull
    public String formatWithColour(@NotNull String colourValue, @NotNull String text) {
        // handle different colour formats
        if (colourValue.startsWith("<") && colourValue.endsWith(">")) {
            // minimessage format
            String closeTag = colourValue.replace("<", "</");
            return colourValue + text + closeTag;
        } else if (colourValue.startsWith("#")) {
            // hex colour
            return "<color:" + colourValue + ">" + text + "</color>";
        } else {
            // legacy code or plain
            return colourValue + text;
        }
    }
    
    /**
     * get total colour count
     */
    public int getColourCount() {
        return colours.size();
    }
    
    /**
     * get total prefix count
     */
    public int getPrefixCount() {
        return prefixes.size();
    }
    
    /**
     * get total suffix count
     */
    public int getSuffixCount() {
        return suffixes.size();
    }
    
    /**
     * reload styles
     */
    public void reload() {
        load();
    }
}