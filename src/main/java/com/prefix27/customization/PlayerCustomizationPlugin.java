package com.prefix27.customization;

import com.prefix27.customization.commands.*;
import com.prefix27.customization.database.DatabaseManager;
import com.prefix27.customization.managers.*;
import com.prefix27.customization.managers.ChatInputManager;
import com.prefix27.customization.integrations.LuckPermsIntegration;
import com.prefix27.customization.integrations.PlaceholderAPIIntegration;
import com.prefix27.customization.integrations.VentureChatIntegration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class PlayerCustomizationPlugin extends JavaPlugin {

    private DatabaseManager databaseManager;
    private PlayerDataManager playerDataManager;
    private PrefixManager prefixManager;
    private ColorManager colorManager;
    private GUIManager guiManager;
    private ChatInputManager chatInputManager;
    
    private LuckPermsIntegration luckPermsIntegration;
    private PlaceholderAPIIntegration placeholderAPIIntegration;
    private VentureChatIntegration ventureChatIntegration;
    
    private static PlayerCustomizationPlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        
        // Load our config first
        saveDefaultConfig();
        
        // Set up all the core managers
        if (!initializeManagers()) {
            getLogger().severe("Failed to initialize managers! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Hook into other plugins
        initializeIntegrations();
        
        // Set up commands
        registerCommands();
        
        // Set up event listeners  
        registerListeners();
        
        getLogger().info("PlayerCustomization plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        // Make sure we save everything before shutting down
        if (databaseManager != null) {
            databaseManager.close();
        }
        
        if (playerDataManager != null) {
            playerDataManager.saveAllData();
        }
        
        getLogger().info("PlayerCustomization plugin has been disabled!");
    }

    private boolean initializeManagers() {
        try {
            // Start with the database - everything depends on this
            databaseManager = new DatabaseManager(this);
            if (!databaseManager.initialize()) {
                getLogger().severe("Failed to initialize database!");
                return false;
            }
            
            // Set up the rest of our managers
            playerDataManager = new PlayerDataManager(this);
            prefixManager = new PrefixManager(this);
            colorManager = new ColorManager(this);
            guiManager = new GUIManager(this);
            chatInputManager = new ChatInputManager(this);
            
            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error initializing managers", e);
            return false;
        }
    }

    private void initializeIntegrations() {
        // LuckPerms is required - we need it for permissions
        if (getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            luckPermsIntegration = new LuckPermsIntegration(this);
            if (luckPermsIntegration.initialize()) {
                getLogger().info("LuckPerms integration enabled!");
            } else {
                getLogger().severe("Failed to initialize LuckPerms integration!");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        } else {
            getLogger().severe("LuckPerms is required but not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Hook into PlaceholderAPI if it's available
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderAPIIntegration = new PlaceholderAPIIntegration(this);
            if (placeholderAPIIntegration.initialize()) {
                getLogger().info("PlaceholderAPI integration enabled!");
            }
        }
        
        // Hook into VentureChat for better chat formatting
        if (getServer().getPluginManager().getPlugin("VentureChat") != null) {
            ventureChatIntegration = new VentureChatIntegration(this);
            if (ventureChatIntegration.initialize()) {
                getLogger().info("VentureChat integration enabled! Use placeholders: %customization_name%, %customization_prefix%, %customization_full%");
            }
        } else {
            // Use our own chat handler when VentureChat is not available
            getServer().getPluginManager().registerEvents(new ChatListener(this), this);
            getLogger().info("Using built-in chat formatting (VentureChat not found)");
        }
    }

    private void registerCommands() {
        // Wire up all our commands
        getCommand("customize").setExecutor(new CustomizeCommand(this));
        getCommand("color").setExecutor(new ColorCommand(this));
        getCommand("prefix").setExecutor(new PrefixCommand(this));
        getCommand("nick").setExecutor(new NickCommand(this));
        getCommand("gradient").setExecutor(new GradientCommand(this));
        getCommand("customization").setExecutor(new CustomizationAdminCommand(this));
        
        // Add tab completion where it makes sense
        getCommand("customize").setTabCompleter(new CustomizeCommand(this));
        getCommand("customization").setTabCompleter(new CustomizationAdminCommand(this));
        getCommand("nick").setTabCompleter(new NickCommand(this));
    }

    private void registerListeners() {
        // Handle player joins/leaves for data loading
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        
        // Handle GUI interactions
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        
        // Handle chat input for custom prefix requests
        getServer().getPluginManager().registerEvents(chatInputManager, this);
    }

    public void reloadPluginConfig() {
        reloadConfig();
        
        // Tell all our managers to reload their configs
        if (playerDataManager != null) {
            playerDataManager.reload();
        }
        if (prefixManager != null) {
            prefixManager.reload();
        }
        if (colorManager != null) {
            colorManager.reload();
        }
        if (guiManager != null) {
            guiManager.reload();
        }
        if (chatInputManager != null) {
            chatInputManager.reload();
        }
    }

    // Getter methods for other classes to access our managers
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public PrefixManager getPrefixManager() {
        return prefixManager;
    }

    public ColorManager getColorManager() {
        return colorManager;
    }

    public GUIManager getGUIManager() {
        return guiManager;
    }

    public ChatInputManager getChatInputManager() {
        return chatInputManager;
    }

    public LuckPermsIntegration getLuckPermsIntegration() {
        return luckPermsIntegration;
    }

    public PlaceholderAPIIntegration getPlaceholderAPIIntegration() {
        return placeholderAPIIntegration;
    }

    public VentureChatIntegration getVentureChatIntegration() {
        return ventureChatIntegration;
    }

    public static PlayerCustomizationPlugin getInstance() {
        return instance;
    }
}