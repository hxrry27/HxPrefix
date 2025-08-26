package dev.hxrry.hxprefix;

import dev.hxrry.hxcore.utils.Log;
import dev.hxrry.hxcore.utils.Scheduler;

import dev.hxrry.hxgui.HxGUI;

import dev.hxrry.hxprefix.api.HxPrefixAPI;
import dev.hxrry.hxprefix.commands.AdminCommand;
import dev.hxrry.hxprefix.commands.ColourCommand;
import dev.hxrry.hxprefix.commands.NickCommand;
import dev.hxrry.hxprefix.commands.PrefixCommand;
import dev.hxrry.hxprefix.commands.SuffixCommand;
import dev.hxrry.hxprefix.commands.TagCommand;
import dev.hxrry.hxprefix.config.ConfigManager;
import dev.hxrry.hxprefix.database.DatabaseManager;
import dev.hxrry.hxprefix.database.DataCache;
import dev.hxrry.hxprefix.hooks.PlaceholderAPIHook;
import dev.hxrry.hxprefix.hooks.LuckPermsHook;
import dev.hxrry.hxprefix.listeners.PlayerListener;
import dev.hxrry.hxprefix.nametags.NametagManager;
import dev.hxrry.hxprefix.utils.MigrationHelper;

import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.command.brigadier.Commands;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class HxPrefix extends JavaPlugin {
    private static HxPrefix instance;
    
    // core comp's
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private DataCache dataCache;
    private NametagManager nametagManager;
    
    // hooks
    private LuckPermsHook luckPermsHook;
    private PlaceholderAPIHook placeholderHook;
    
    // api
    private HxPrefixAPI api;

    // migration Helper
    private MigrationHelper migrationHelper;
    
    @SuppressWarnings("deprecation")
    @Override
    public void onEnable() {
        instance = this;
        
        // initialize components in some degree of order
        try {
            Log.info("Initializing HxPrefix v" + getDescription().getVersion());
            
            // shaded hx library's being init'd
            Scheduler.init(this);
            Log.init(this);
            HxGUI.init(this);

            Log.info("HxGUI Library initialized successfully");

            // 1. load configurations
            configManager = new ConfigManager(this);
            configManager.loadAll();
            
            // 2. setup database with retry logic
            databaseManager = new DatabaseManager(this);
            if (!databaseManager.initialize()) {
                Log.error("Failed to connect to database! Check your config.yml");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            // 3. initialize cache
            dataCache = new DataCache(this, databaseManager);

            // 3.5. initialize migration helper (has to be post-database)
            migrationHelper = new MigrationHelper(this);
            
            // 4. setup hooks
            setupHooks();
            
            // 5. initialize nametag manager
            if (configManager.isNametagsEnabled()) {
                nametagManager = new NametagManager(this);
            }
            
            // 6. register commands
            registerCommands();

            // 7. register listeners
            getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
            
            // 8. initialize API
            api = new HxPrefixAPI(this);
            
            // 9. run startup tasks
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
        
        // reverse ordr cleanup
        if (nametagManager != null) {
            nametagManager.cleanup();
        }
        
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
        
        // cancel remainers
        getServer().getScheduler().cancelTasks(this);
        
        Log.info("HxPrefix disabled successfully!");
    }
    
    private void setupHooks() {
        // lp
       if (getServer().getPluginManager().isPluginEnabled("LuckPerms")) {
            luckPermsHook = new LuckPermsHook(this); // Pass plugin instance
            Log.info("Hooked into LuckPerms");
        } else {
            Log.warning("LuckPerms not found! Rank features will be limited.");
        }
        
        // placeholderAPI
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
            
            // register each command
            new AdminCommand(this).register(commands);
            new ColourCommand(this).register(commands);
            new PrefixCommand(this).register(commands);
            new SuffixCommand(this).register(commands);
            new TagCommand(this).register(commands);
            new NickCommand(this).register(commands);
            
            Log.info("Registered 7 commands");
        });
    }
    
    private void postStartup() {
        // load all online players into cache
        getServer().getOnlinePlayers().forEach(player -> {
            dataCache.loadPlayer(player.getUniqueId());
            if (nametagManager != null) {
                nametagManager.updatePlayer(player);
            }
        });
        
        // show stats
        Log.info("Loaded " + dataCache.getCacheSize() + " players into cache");
        Log.info("Available colours: " + configManager.getStyleConfig().getColourCount());
        Log.info("Available prefixes: " + configManager.getStyleConfig().getPrefixCount());
        Log.info("Available suffixes: " + configManager.getStyleConfig().getSuffixCount());
    }
    
    // copmonent getters
    public static HxPrefix getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public DataCache getDataCache() { return dataCache; }
    public NametagManager getNametagManager() { return nametagManager; }
    public LuckPermsHook getLuckPermsHook() { return luckPermsHook; }
    public HxPrefixAPI getAPI() { return api; }
    public MigrationHelper getMigrationHelper() { return migrationHelper; }
    
    /**
     * reload the plugin configuration
     */
    public void reload() {
        Log.info("Reloading HxPrefix configuration...");
        
        // reload configs
        configManager.loadAll();
        
        // clear and rebuild cache
        dataCache.clearCache();
        
        // update all online players
        getServer().getOnlinePlayers().forEach(player -> {
            dataCache.loadPlayer(player.getUniqueId());
            if (nametagManager != null) {
                nametagManager.updatePlayer(player);
            }
        });
        
        Log.info("Configuration reloaded successfully!");
    }
}