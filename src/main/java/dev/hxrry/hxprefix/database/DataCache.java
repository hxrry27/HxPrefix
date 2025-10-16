package dev.hxrry.hxprefix.database;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;

import dev.hxrry.hxcore.cache.CacheManager;
import dev.hxrry.hxcore.utils.Log;

import dev.hxrry.hxprefix.HxPrefix;
import dev.hxrry.hxprefix.api.models.PlayerCustomization;

import org.bukkit.Bukkit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Caching layer for player data using HxCore's CacheManager
 * 
 * This class now delegates cache management to HxCore instead of
 * implementing its own eviction, TTL, and statistics tracking.
 * 
 * Uses Caffeine cache (via HxCore) which provides:
 * - Automatic TTL-based expiration
 * - Size-based eviction
 * - Thread-safe operations
 * - Built-in statistics tracking
 */

public class DataCache {
    private final HxPrefix plugin;
    private final DatabaseManager database;
    
    // Caffeine cache instance from HxCore
    private final Cache<UUID, PlayerCustomization> cache;
    
    // Executor for async operations
    private final ExecutorService executor = Executors.newCachedThreadPool();
    
    public DataCache(@NotNull HxPrefix plugin, @NotNull DatabaseManager database) {
        this.plugin = plugin;
        this.database = database;
        
        // Get HxCore's cache manager
        CacheManager cacheManager = plugin.getCore().getCacheManager();
        
        // Create cache with TTL and max size from config
        int ttlMinutes = plugin.getConfigManager().getCacheTTL() / 60; // Convert seconds to minutes
        int maxSize = plugin.getConfigManager().getCacheMaxSize();
        
        this.cache = cacheManager.createSimpleCache("player-data", ttlMinutes, maxSize);
        
        Log.info("Initialized data cache (TTL: " + ttlMinutes + "m, Max: " + maxSize + ")");
    }
    
    /**
     * Get player data from cache or database
     * 
     * @param uuid Player UUID
     * @return PlayerCustomization or null if not found
     */
    @Nullable
    public PlayerCustomization getPlayerData(@NotNull UUID uuid) {
        // try cache first (Caffeine's getIfPresent returns null if not cached)
        PlayerCustomization cached = cache.getIfPresent(uuid);
        if (cached != null) {
            Log.debug("Cache HIT for " + uuid);
            return cached;
        }
        
        // cache miss - load from database
        Log.debug("Cache MISS for " + uuid);
        PlayerCustomization data = database.loadPlayerData(uuid);
        
        if (data != null) {
            cache.put(uuid, data);
        }
        
        return data;
    }
    
    /**
     * get or create player data
     * 
     * @param uuid Player UUID
     * @return PlayerCustomization (never null)
     */
    @NotNull
    public PlayerCustomization getOrCreatePlayerData(@NotNull UUID uuid) {
        PlayerCustomization data = getPlayerData(uuid);
        
        if (data == null) {
            // Get username from Bukkit
            String username = Bukkit.getOfflinePlayer(uuid).getName();
            if (username == null) {
                username = "Unknown";
            }
            
            data = new PlayerCustomization(uuid, username);
            cache.put(uuid, data);
        }
        
        return data;
    }
    
    /**
     * save player data to cache and database
     * 
     * @param data PlayerCustomization to save
     * @return CompletableFuture that completes when save is done
     */
    public CompletableFuture<Boolean> savePlayerData(@NotNull PlayerCustomization data) {
        // update cache immediately
        cache.put(data.getUuid(), data);
        
        // save to database async
        return CompletableFuture.supplyAsync(() -> 
            database.savePlayerData(data), executor
        );
    }
    
    /**
     * load player data asynchronously
     * 
     * @param uuid Player UUID
     * @return CompletableFuture with PlayerCustomization
     */
    public CompletableFuture<PlayerCustomization> loadPlayer(@NotNull UUID uuid) {
        // check cache first
        PlayerCustomization cached = cache.getIfPresent(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        
        // load from database async
        return CompletableFuture.supplyAsync(() -> {
            PlayerCustomization data = database.loadPlayerData(uuid);
            if (data != null) {
                cache.put(uuid, data);
            }
            return data;
        }, executor);
    }
    
    /**
     * reload player data from database (invalidate cache)
     * 
     * @param uuid Player UUID
     */
    public void reloadPlayer(@NotNull UUID uuid) {
        // invalidate cache entry
        cache.invalidate(uuid);
        
        // load fresh from database
        loadPlayer(uuid);
    }
    
    /**
     * save all cached data to database
     */
    public void saveAll() {
        // get all cached entries
        var allEntries = cache.asMap();
        
        if (allEntries.isEmpty()) {
            Log.debug("No cached data to save");
            return;
        }
        
        Log.info("Saving " + allEntries.size() + " cached players to database...");
        
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        
        for (PlayerCustomization data : allEntries.values()) {
            futures.add(CompletableFuture.supplyAsync(() -> 
                database.savePlayerData(data), executor
            ));
        }
        
        // wait for all saves to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );
        
        try {
            allFutures.get(10, TimeUnit.SECONDS);
            Log.info("Saved all cached data successfully");
        } catch (Exception e) {
            Log.error("Failed to save some cached data", e);
        }
    }
    
    /**
     * clear the entire cache
     */
    public void clearCache() {
        cache.invalidateAll();
        Log.info("Cache cleared");
    }
    
    /**
     * cleanup resources
     */
    public void cleanup() {
        Log.info("Cleaning up data cache...");
        
        // save all data before cleanup
        saveAll();
        
        // Shutdown executor
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // clear cache
        cache.invalidateAll();
        
        Log.info("Data cache cleaned up");
    }
    
    // ===== STATISTICS METHODS =====
    
    /**
     * get current cache size
     * 
     * @return number of entries in cache
     */
    public int getCacheSize() {
        return (int) cache.estimatedSize();
    }
    
    /**
     * get cache hit rate
     * 
     * @return hit rate as percentage (0-100)
     */
    public double getHitRate() {
        CacheStats stats = cache.stats();
        long hits = stats.hitCount();
        long misses = stats.missCount();
        long total = hits + misses;
        
        if (total == 0) return 0;
        return (hits * 100.0) / total;
    }
    
    /**
     * get total cache hits
     * 
     * @return number of cache hits
     */
    public long getHits() {
        return cache.stats().hitCount();
    }
    
    /**
     * get total cache misses
     * 
     * @return number of cache misses
     */
    public long getMisses() {
        return cache.stats().missCount();
    }
    
    /**
     * get total evictions
     * 
     * @return number of cache evictions
     */
    public long getEvictions() {
        return cache.stats().evictionCount();
    }
    
    /**
     * check if a player is currently cached
     * 
     * @param uuid Player UUID
     * @return true if player is in cache
     */
    public boolean isCached(@NotNull UUID uuid) {
        return cache.getIfPresent(uuid) != null;
    }
    
    /**
     * warm up cache with all online players
     * useful at startup to pre-load data
     */
    public void warmUp() {
        Log.info("Warming up cache with online players...");
        
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (var player : Bukkit.getOnlinePlayers()) {
            futures.add(loadPlayer(player.getUniqueId()).thenAccept(data -> {
                if (data == null) {
                    // Create new data for player
                    data = new PlayerCustomization(player.getUniqueId(), player.getName());
                    savePlayerData(data);
                }
            }));
        }
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenRun(() -> Log.info("Cache warmed up with " + futures.size() + " players"));
    }
    
    /**
     * Get cache statistics as a formatted string
     * 
     * @return statistics summary
     */
    @NotNull
    public String getStatistics() {
        CacheStats stats = cache.stats();
        return String.format(
            "Cache: %d entries | Hit Rate: %.1f%% | Hits: %d | Misses: %d | Evictions: %d",
            getCacheSize(),
            getHitRate(),
            stats.hitCount(),
            stats.missCount(),
            stats.evictionCount()
        );
    }
}