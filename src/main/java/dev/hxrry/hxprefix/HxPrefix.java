package dev.hxrry.hxprefix;

import dev.hxrry.hxcore.HxCore;
import dev.hxrry.hxcore.utils.Log;
import dev.hxrry.hxcore.utils.Scheduler;

import dev.hxrry.hxgui.HxGUI;

import dev.hxrry.hxprefix.api.HxPrefixAPI;
import dev.hxrry.hxprefix.commands.AdminCommand;
import dev.hxrry.hxprefix.commands.ColourCommand;
import dev.hxrry.hxprefix.commands.NickCommand;
import dev.hxrry.hxprefix.commands.PrefixCommand;
import dev.hxrry.hxprefix.commands.SuffixCommand;
import dev.hxrry.hxprefix.config.ConfigManager;
import dev.hxrry.hxprefix.database.DatabaseManager;
import dev.hxrry.hxprefix.database.DataCache;
import dev.hxrry.hxprefix.hooks.PlaceholderAPIHook;
import dev.hxrry.hxprefix.hooks.LuckPermsHook;
import dev.hxrry.hxprefix.listeners.PlayerListener;

import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.command.brigadier.Commands;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class HxPrefix extends JavaPlugin {
    private static HxPrefix instance;
    
    // Core components
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private DataCache dataCache;
    private HxCore core;
    
    // Hooks
    private LuckPermsHook luckPermsHook;
    private PlaceholderAPIHook placeholderHook;
    
    // API
    private HxPrefixAPI api;
    
    @SuppressWarnings("deprecation")
    @Override
    public void onEnable() {
        instance = this;
        
        try {
            Log.info("Initializing HxPrefix v" + getDescription().getVersion());
            
            // Initialize shaded libraries
            Scheduler.init(this);
            Log.init(this);

            // Initialize HxCore
            core = new HxCore(this);
            if (!core.initialize()) {
                Log.error("Failed to initialize HxCore!");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }

            // Initialize HxGUI
            HxGUI.init(this);
            Log.info("HxGUI Library initialized successfully");

            // Load configurations
            configManager = new ConfigManager(this);
            configManager.loadAll();
            
            // Setup database
            databaseManager = new DatabaseManager(this);
            if (!databaseManager.initialize()) {
                Log.error("Failed to connect to database! Check your config.yml");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            // Initialize cache
            dataCache = new DataCache(this, databaseManager);
            
            // Setup hooks
            setupHooks();
            
            // Register commands
            registerCommands();

            // Register listeners
            getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
            
            // Initialize API
            api = new HxPrefixAPI(this);
            
            // Run startup tasks
            Scheduler.runTaskLater(this::postStartup, 20L);
            
            Log.info("HxPrefix enabled successfully!");
            
        } catch (Exception e) {
            Log.error("Failed to initialize HxPrefix: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        Log.info("Disabling HxPrefix...");
        
        if (placeholderHook != null) {
            placeholderHook.unregister();
        }
        
        if (dataCache != null) {
            dataCache.saveAll();
            dataCache.cleanup();
        }
        
        if (databaseManager != null) {
            databaseManager.close();
        }

        if (core != null) {
            core.shutdown();
        }
        
        // Cancel remaining tasks
        getServer().getScheduler().cancelTasks(this);
        
        Log.info("HxPrefix disabled successfully!");
    }
    
    private void setupHooks() {
        // LuckPerms
        if (getServer().getPluginManager().isPluginEnabled("LuckPerms")) {
            luckPermsHook = new LuckPermsHook(this);
            if (!luckPermsHook.init()) {
                Log.warning("Failed to initialize LuckPerms hook - rank features will be limited.");
                luckPermsHook = null; // Clear it if init failed
            }
        } else {
            Log.warning("LuckPerms not found! Rank features will be limited.");
        }
        
        // PlaceholderAPI
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            placeholderHook = new PlaceholderAPIHook(this);
            placeholderHook.register();
            Log.info("Registered PlaceholderAPI expansion");
        }
    }

    private void registerCommands() {
        LifecycleEventManager<Plugin> manager = this.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            
            // Register each command
            new AdminCommand(this).register(commands);
            new ColourCommand(this).register(commands);
            new PrefixCommand(this).register(commands);
            new SuffixCommand(this).register(commands);
            new NickCommand(this).register(commands);
            
            Log.info("Registered 6 commands");
        });
    }
    
    private void postStartup() {
        // Load all online players into cache
        getServer().getOnlinePlayers().forEach(player -> {
            dataCache.loadPlayer(player.getUniqueId());
        });
        
        // Show stats
        Log.info("Loaded " + dataCache.getCacheSize() + " players into cache");
        Log.info("Available colours: " + configManager.getStyleConfig().getColourCount());
        Log.info("Available prefixes: " + configManager.getStyleConfig().getPrefixCount());
        Log.info("Available suffixes: " + configManager.getStyleConfig().getSuffixCount());
    }
    
    // Component getters
    public static HxPrefix getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public DataCache getDataCache() { return dataCache; }
    public LuckPermsHook getLuckPermsHook() { return luckPermsHook; }
    public HxPrefixAPI getAPI() { return api; }
    @NotNull public HxCore getCore() { return core; }
    
    /**
     * Reload the plugin configuration
     */
    public void reload() {
        Log.info("Reloading HxPrefix configuration...");
        
        if (dataCache != null) {
            dataCache.saveAll();
        }
        
        // Reload configs
        configManager.loadAll();
        
        // Clear and rebuild cache
        dataCache.clearCache();
        
        // Update all online players
        getServer().getOnlinePlayers().forEach(player -> {
            dataCache.loadPlayer(player.getUniqueId());
        });
        
        Log.info("Configuration reloaded successfully!");
    }
}