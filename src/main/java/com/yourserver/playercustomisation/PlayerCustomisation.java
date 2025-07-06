package com.yourserver.playercustomisation;

import com.yourserver.playercustomisation.commands.*;
import com.yourserver.playercustomisation.database.MySQL;
import com.yourserver.playercustomisation.database.PlayerDataManager;
import com.yourserver.playercustomisation.listeners.PlayerJoinListener;
import com.yourserver.playercustomisation.placeholders.CustomisationExpansion;
import com.yourserver.playercustomisation.utils.PermissionUtils;
import com.yourserver.playercustomisation.gui.MenuManager;
import com.yourserver.playercustomisation.config.ConfigManager;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class PlayerCustomisation extends JavaPlugin implements CommandExecutor {
    private MySQL mysql;
    private PlayerDataManager playerDataManager;
    private CustomisationExpansion placeholderExpansion;
    private MenuManager menuManager;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        // Save default config if it doesn't exist
        saveDefaultConfig();
        
        // FORCE LOAD CONFIG FROM DISK
        File configFile = new File(getDataFolder(), "config.yml");
        if (configFile.exists()) {
            getLogger().info("Loading config from: " + configFile.getAbsolutePath());
            FileConfiguration diskConfig = YamlConfiguration.loadConfiguration(configFile);
            
            // Log what we're loading
            getLogger().info("=== MySQL Configuration ===");
            getLogger().info("Host: " + diskConfig.getString("database.mysql.host"));
            getLogger().info("Port: " + diskConfig.getInt("database.mysql.port"));
            getLogger().info("Database: " + diskConfig.getString("database.mysql.database"));
            getLogger().info("Username: " + diskConfig.getString("database.mysql.username"));
            getLogger().info("Password: [HIDDEN]");
            getLogger().info("========================");
            
            // IMPORTANT: Override the in-memory config with disk config
            for (String key : diskConfig.getKeys(true)) {
                getConfig().set(key, diskConfig.get(key));
            }
        } else {
            getLogger().warning("Config file not found at: " + configFile.getAbsolutePath());
            getLogger().warning("Creating default config...");
        }

        // Initialize database with a small delay for Docker
        getServer().getScheduler().runTaskLater(this, () -> {
            initializePlugin();
        }, 40L); // 2 second delay
    }
    
    private void initializePlugin() {
        getLogger().info("Initializing PlayerCustomisation...");
        
        // Double-check our config values
        String host = getConfig().getString("database.mysql.host");
        getLogger().info("Connecting to MySQL at: " + host);
        
        // Initialize database
        mysql = new MySQL(this);
        if (!mysql.connect()) {
            getLogger().severe("Failed to connect to MySQL! Disabling plugin.");
            getLogger().severe("Please check:");
            getLogger().severe("1. MySQL container is running: docker ps | grep customisation-mysql");
            getLogger().severe("2. Config host is correct: " + host);
            getLogger().severe("3. Both containers are on the same network");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize menu manager
        menuManager = new MenuManager(this);

        // Initialize config manager
        configManager = new ConfigManager(this);

        // Initialize managers
        playerDataManager = new PlayerDataManager(this, mysql);

        // Register commands
        registerCommands();

        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        // Register PlaceholderAPI expansion
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            placeholderExpansion = new CustomisationExpansion(this);
            placeholderExpansion.register();
            getLogger().info("PlaceholderAPI integration enabled!");
        }

        getLogger().info("PlayerCustomisation has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        // Unregister PlaceholderAPI expansion
        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
        }

        // Clear caches
        PermissionUtils.clearCache();
        if (playerDataManager != null) {
            playerDataManager.clearCache();
        }

        // Closing menu manager BEFORE closing database
        if (menuManager != null) {
            menuManager.shutdown();
        }

        // Close database connection
        if (mysql != null) {
            mysql.disconnect();
        }

        getLogger().info("PlayerCustomisation has been disabled!");
    }

    private void registerCommands() {
        getCommand("color").setExecutor(new ColorCommand(this));
        getCommand("prefix").setExecutor(new PrefixCommand(this));
        getCommand("suffix").setExecutor(new SuffixCommand(this));
        getCommand("nick").setExecutor(new NickCommand(this));
        getCommand("requesttag").setExecutor(new RequestTagCommand(this));
        getCommand("managetags").setExecutor(new ManageTagsCommand(this));
        getCommand("pcreload").setExecutor(this);
        getCommand("pc-internal").setExecutor(new InternalCommand(this));
    }

    public MenuManager getMenuManager() {
        return menuManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equals("pcreload")) {
            if (!sender.hasPermission("playercustomisation.admin.reload")) {
                sender.sendMessage("§cYou don't have permission to use this command!");
                return true;
            }

            // Force reload from disk
            File configFile = new File(getDataFolder(), "config.yml");
            FileConfiguration diskConfig = YamlConfiguration.loadConfiguration(configFile);
            for (String key : diskConfig.getKeys(true)) {
                getConfig().set(key, diskConfig.get(key));
            }
            
            configManager.loadAllConfigs();
            PermissionUtils.clearCache();
            playerDataManager.clearCache();

            sender.sendMessage("§aPlayerCustomisation configuration reloaded from disk!");
            sender.sendMessage("§7MySQL Host: " + getConfig().getString("database.mysql.host"));
            sender.sendMessage("§7Loaded: " + 
                configManager.getSolidColors().size() + " colors, " +
                configManager.getGradients().size() + " gradients, " +
                configManager.getAllPrefixes().size() + " prefixes, " +
                configManager.getAllSuffixes().size() + " suffixes");
            return true;
        }
        return false;
    }

    // Getters
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public MySQL getMySQL() {
        return mysql;
    }
}