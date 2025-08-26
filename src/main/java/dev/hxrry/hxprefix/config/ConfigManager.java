package dev.hxrry.hxprefix.config;

import dev.hxrry.hxcore.utils.Log;

import dev.hxrry.hxprefix.HxPrefix;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * manages all configuration files for the plugin
 */
public class ConfigManager {
    private final HxPrefix plugin;
    
    // config instances
    private FileConfiguration mainConfig;
    private MessagesConfig messagesConfig;
    private StyleConfig styleConfig;
    private PermissionConfig permissionConfig;
    
    // config files
    private final File configFile;
    private final File messagesFile;
    private final File stylesFolder;
    
    public ConfigManager(@NotNull HxPrefix plugin) {
        this.plugin = plugin;
        
        // setup file references
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        this.stylesFolder = new File(plugin.getDataFolder(), "styles");
    }
    
    /**
     * load all configuration files
     */
    public void loadAll() {
        // ensure data folder exists
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        // ensure styles folder exists
        if (!stylesFolder.exists()) {
            stylesFolder.mkdirs();
        }
        
        // save defaults if they don't exist
        saveDefaults();
        
        // load main config
        loadMainConfig();
        
        // load other configs
        messagesConfig = new MessagesConfig(plugin, messagesFile);
        messagesConfig.load();
        
        styleConfig = new StyleConfig(plugin, stylesFolder);
        styleConfig.load();
        
        permissionConfig = new PermissionConfig(plugin, mainConfig);
        permissionConfig.load();
        
        Log.info("loaded all configuration files");
    }
    
    /**
     * reload all configuration files
     */
    public void reload() {
        loadAll();
    }
    
    /**
     * save default config files
     */
    private void saveDefaults() {
        // save main config
        if (!configFile.exists()) {
            saveResource("config.yml", false);
        }
        
        // save messages
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
        
        // save style configs
        File coloursFile = new File(stylesFolder, "colours.yml");
        File prefixesFile = new File(stylesFolder, "prefixes.yml");
        File suffixesFile = new File(stylesFolder, "suffixes.yml");
        
        if (!coloursFile.exists()) {
            saveResource("styles/colours.yml", false);
        }
        if (!prefixesFile.exists()) {
            saveResource("styles/prefixes.yml", false);
        }
        if (!suffixesFile.exists()) {
            saveResource("styles/suffixes.yml", false);
        }
    }
    
    /**
     * load the main config
     */
    private void loadMainConfig() {
        mainConfig = YamlConfiguration.loadConfiguration(configFile);
        
        // validate required fields
        validateMainConfig();
    }
    
    /**
     * validate main config has required fields
     */
    private void validateMainConfig() {
        boolean valid = true;
        
        // check database section
        if (!mainConfig.contains("database.type")) {
            Log.warning("missing database.type in config.yml");
            valid = false;
        }
        
        if (mainConfig.getString("database.type", "sqlite").equalsIgnoreCase("mysql")) {
            if (!mainConfig.contains("database.mysql.host")) {
                Log.warning("missing database.mysql.host in config.yml");
                valid = false;
            }
        }
        
        // check feature toggles
        if (!mainConfig.contains("features")) {
            Log.warning("missing features section in config.yml - using defaults");
            setDefaultFeatures();
        }
        
        if (!valid) {
            Log.warning("config validation failed - some features may not work correctly");
        }
    }
    
    /**
     * set default feature toggles
     */
    private void setDefaultFeatures() {
        mainConfig.set("features.colours", true);
        mainConfig.set("features.prefixes", true);
        mainConfig.set("features.suffixes", true);
        mainConfig.set("features.nicknames", true);
        mainConfig.set("features.custom-tags", true);
        mainConfig.set("features.nametags", true);
        
        // save changes
        try {
            mainConfig.save(configFile);
        } catch (IOException e) {
            Log.error("failed to save default features", e);
        }
    }
    
    /**
     * save a resource from the jar
     */
    private void saveResource(@NotNull String resourcePath, boolean replace) {
        if (resourcePath.equals("")) {
            throw new IllegalArgumentException("resource path cannot be empty");
        }
        
        File outFile = new File(plugin.getDataFolder(), resourcePath);
        
        if (!replace && outFile.exists()) {
            return;
        }
        
        // ensure parent directories exist
        File parent = outFile.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }
        
        try (InputStream in = plugin.getResource(resourcePath)) {
            if (in == null) {
                Log.warning("could not find resource: " + resourcePath);
                return;
            }
            
            Files.copy(in, outFile.toPath());
            Log.debug("saved resource: " + resourcePath);
            
        } catch (IOException e) {
            Log.error("failed to save resource: " + resourcePath, e);
        }
    }
    
    // getters for config instances
    
    @NotNull
    public FileConfiguration getMainConfig() {
        return mainConfig;
    }
    
    @NotNull
    public MessagesConfig getMessagesConfig() {
        return messagesConfig;
    }
    
    @NotNull
    public StyleConfig getStyleConfig() {
        return styleConfig;
    }
    
    @NotNull
    public PermissionConfig getPermissionConfig() {
        return permissionConfig;
    }
    
    // convenience methods for common config values
    
    public boolean isFeatureEnabled(@NotNull String feature) {
        return mainConfig.getBoolean("features." + feature, true);
    }
    
    public boolean isNametagsEnabled() {
        return isFeatureEnabled("nametags");
    }
    
    public boolean isColoursEnabled() {
        return isFeatureEnabled("colours");
    }
    
    public boolean isPrefixesEnabled() {
        return isFeatureEnabled("prefixes");
    }
    
    public boolean isSuffixesEnabled() {
        return isFeatureEnabled("suffixes");
    }
    
    public boolean isNicknamesEnabled() {
        return isFeatureEnabled("nicknames");
    }
    
    public boolean isCustomTagsEnabled() {
        return isFeatureEnabled("custom-tags");
    }
    
    public int getCacheTTL() {
        return mainConfig.getInt("cache.ttl-seconds", 300);
    }
    
    public int getCacheMaxSize() {
        return mainConfig.getInt("cache.max-size", 1000);
    }
    
    public String getDatabaseType() {
        return mainConfig.getString("database.type", "sqlite");
    }
    
    public boolean isDebugMode() {
        return mainConfig.getBoolean("debug", false);
    }
}