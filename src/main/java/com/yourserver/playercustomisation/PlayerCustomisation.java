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
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerCustomisation extends JavaPlugin implements CommandExecutor {
    private MySQL mysql;
    private PlayerDataManager playerDataManager;
    private CustomisationExpansion placeholderExpansion;
    private MenuManager menuManager;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();

        // Initialize database
        mysql = new MySQL(this);
        if (!mysql.connect()) {
            getLogger().severe("Failed to connect to MySQL! Disabling plugin.");
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

        getLogger().info("PlayerCustomisation has been enabled!");
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

        //Closing menu manager BEFORE closing database
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
        getCommand("suffix").setExecutor(new SuffixCommand(this));  // ← ADD THIS
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

            configManager.loadAllConfigs();
            PermissionUtils.clearCache();
            playerDataManager.clearCache();

            sender.sendMessage("§aPlayerCustomisation configuration reloaded!");
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