package com.yourserver.playercustomisation;

import com.yourserver.playercustomisation.commands.*;
import com.yourserver.playercustomisation.database.MySQL;
import com.yourserver.playercustomisation.database.PlayerDataManager;
import com.yourserver.playercustomisation.listeners.PlayerJoinListener;
import com.yourserver.playercustomisation.placeholders.CustomisationExpansion;
import com.yourserver.playercustomisation.utils.PermissionUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerCustomisation extends JavaPlugin implements CommandExecutor {
    private MySQL mysql;
    private PlayerDataManager playerDataManager;
    private CustomisationExpansion placeholderExpansion;

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

        // Close database connection
        if (mysql != null) {
            mysql.disconnect();
        }

        getLogger().info("PlayerCustomisation has been disabled!");
    }

    private void registerCommands() {
        getCommand("color").setExecutor(new ColorCommand(this));
        getCommand("prefix").setExecutor(new PrefixCommand(this));
        getCommand("nick").setExecutor(new NickCommand(this));
        getCommand("requesttag").setExecutor(new RequestTagCommand(this));
        getCommand("managetags").setExecutor(new ManageTagsCommand(this));
        getCommand("pcreload").setExecutor(this);
        
        // Internal command for GUI interactions
        getCommand("pc-internal").setExecutor(new InternalCommand(this));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equals("pcreload")) {
            if (!sender.hasPermission("playercustomisation.admin.reload")) {
                sender.sendMessage("§cYou don't have permission to use this command!");
                return true;
            }

            reloadConfig();
            PermissionUtils.clearCache();
            playerDataManager.clearCache();
            sender.sendMessage("§aPlayerCustomisation configuration reloaded!");
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