package dev.hxrry.hxprefix.database;

import dev.hxrry.hxcore.utils.Log;

import dev.hxrry.hxprefix.HxPrefix;
import dev.hxrry.hxprefix.api.models.PlayerCustomization;

import org.bukkit.Bukkit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * caching layer for player data
 */
public class DataCache {
    private final HxPrefix plugin;
    private final DatabaseManager database;
    
    // cache storage
    private final Map<UUID, PlayerCustomization> cache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastAccess = new ConcurrentHashMap<>();
    
    // cache settings
    private final int maxSize;
    private final long ttlMillis;
    
    // cache statistics
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);
    private final AtomicInteger evictions = new AtomicInteger(0);
    
    // executor for async operations
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private ScheduledExecutorService cleaner;
    
    public DataCache(@NotNull HxPrefix plugin, @NotNull DatabaseManager database) {
        this.plugin = plugin;
        this.database = database;
        
        // load settings from config
        this.maxSize = plugin.getConfigManager().getCacheMaxSize();
        this.ttlMillis = plugin.getConfigManager().getCacheTTL() * 1000L;
        
        // start cleanup task
        startCleanupTask();
    }
    
    /**
     * get player data from cache or database
     */
    @Nullable
    public PlayerCustomization getPlayerData(@NotNull UUID uuid) {
        // check cache first
        PlayerCustomization cached = cache.get(uuid);
        
        if (cached != null) {
            // check if expired
            Long lastAccessTime = lastAccess.get(uuid);
            if (lastAccessTime != null && (System.currentTimeMillis() - lastAccessTime) < ttlMillis) {
                hits.incrementAndGet();
                lastAccess.put(uuid, System.currentTimeMillis());
                return cached;
            }
            
            // expired - remove from cache
            cache.remove(uuid);
            lastAccess.remove(uuid);
            evictions.incrementAndGet();
        }
        
        // cache miss - load from database
        misses.incrementAndGet();
        PlayerCustomization data = database.loadPlayerData(uuid);
        
        if (data != null) {
            putInCache(data);
        }
        
        return data;
    }
    
    /**
     * get or create player data
     */
    @NotNull
    public PlayerCustomization getOrCreatePlayerData(@NotNull UUID uuid) {
        PlayerCustomization data = getPlayerData(uuid);
        
        if (data == null) {
            // get username from bukkit
            String username = Bukkit.getOfflinePlayer(uuid).getName();
            if (username == null) {
                username = "Unknown";
            }
            
            data = new PlayerCustomization(uuid, username);
            putInCache(data);
        }
        
        return data;
    }
    
    /**
     * save player data to cache and database
     */
    public CompletableFuture<Boolean> savePlayerData(@NotNull PlayerCustomization data) {
        // update cache immediately
        putInCache(data);
        
        // save to database async
        return CompletableFuture.supplyAsync(() -> 
            database.savePlayerData(data), executor
        );
    }
    
    /**
     * load player data async
     */
    public CompletableFuture<PlayerCustomization> loadPlayer(@NotNull UUID uuid) {
        // check cache first
        PlayerCustomization cached = cache.get(uuid);
        if (cached != null) {
            Long lastAccessTime = lastAccess.get(uuid);
            if (lastAccessTime != null && (System.currentTimeMillis() - lastAccessTime) < ttlMillis) {
                return CompletableFuture.completedFuture(cached);
            }
        }
        
        // load from database async
        return CompletableFuture.supplyAsync(() -> {
            PlayerCustomization data = database.loadPlayerData(uuid);
            if (data != null) {
                putInCache(data);
            }
            return data;
        }, executor);
    }
    
    /**
     * reload player data from database
     */
    public void reloadPlayer(@NotNull UUID uuid) {
        // remove from cache to force reload
        cache.remove(uuid);
        lastAccess.remove(uuid);
        
        // load fresh from database
        loadPlayer(uuid);
    }
    
    /**
     * put data in cache
     */
    private void putInCache(@NotNull PlayerCustomization data) {
        // check cache size
        if (cache.size() >= maxSize) {
            evictOldest();
        }
        
        cache.put(data.getUuid(), data);
        lastAccess.put(data.getUuid(), System.currentTimeMillis());
    }
    
    /**
     * evict oldest entry from cache
     */
    private void evictOldest() {
        if (cache.isEmpty()) return;
        
        // find oldest entry
        UUID oldest = null;
        long oldestTime = System.currentTimeMillis();
        
        for (Map.Entry<UUID, Long> entry : lastAccess.entrySet()) {
            if (entry.getValue() < oldestTime) {
                oldest = entry.getKey();
                oldestTime = entry.getValue();
            }
        }
        
        if (oldest != null) {
            cache.remove(oldest);
            lastAccess.remove(oldest);
            evictions.incrementAndGet();
            
            Log.debug("evicted " + oldest + " from cache (oldest)");
        }
    }
    
    /**
     * save all cached data to database
     */
    public void saveAll() {
        Log.info("saving " + cache.size() + " cached players to database...");
        
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        
        for (PlayerCustomization data : cache.values()) {
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
            Log.info("saved all cached data successfully");
        } catch (Exception e) {
            Log.error("failed to save some cached data", e);
        }
    }
    
    /**
     * clear the cache
     */
    public void clearCache() {
        cache.clear();
        lastAccess.clear();
        hits.set(0);
        misses.set(0);
        evictions.set(0);
        
        Log.info("cache cleared");
    }
    
    /**
     * start cleanup task
     */
    private void startCleanupTask() {
        cleaner = Executors.newSingleThreadScheduledExecutor();
        
        // run cleanup every minute
        cleaner.scheduleAtFixedRate(() -> {
            int removed = 0;
            long now = System.currentTimeMillis();
            
            // remove expired entries
            Iterator<Map.Entry<UUID, Long>> iterator = lastAccess.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, Long> entry = iterator.next();
                
                if ((now - entry.getValue()) > ttlMillis) {
                    cache.remove(entry.getKey());
                    iterator.remove();
                    removed++;
                }
            }
            
            if (removed > 0) {
                evictions.addAndGet(removed);
                Log.debug("cleaned up " + removed + " expired cache entries");
            }
            
            // log statistics periodically
            if (plugin.getConfigManager().isDebugMode()) {
                logStatistics();
            }
            
        }, 60, 60, TimeUnit.SECONDS);
    }
    
    /**
     * log cache statistics
     */
    private void logStatistics() {
        long totalRequests = hits.get() + misses.get();
        if (totalRequests == 0) return;
        
        double hitRate = (hits.get() * 100.0) / totalRequests;
        
        Log.debug("cache stats: " + cache.size() + " entries, " +
                 String.format("%.1f%%", hitRate) + " hit rate, " +
                 evictions.get() + " evictions");
    }
    
    /**
     * cleanup resources
     */
    public void cleanup() {
        // save all data
        saveAll();
        
        // shutdown executors
        if (cleaner != null) {
            cleaner.shutdown();
            try {
                if (!cleaner.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleaner.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleaner.shutdownNow();
            }
        }
        
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
        
        // clear cache
        clearCache();
    }
    
    // statistics methods
    
    /**
     * get cache size
     */
    public int getCacheSize() {
        return cache.size();
    }
    
    /**
     * get cache hit rate
     */
    public double getHitRate() {
        long total = hits.get() + misses.get();
        if (total == 0) return 0;
        return (hits.get() * 100.0) / total;
    }
    
    /**
     * get total hits
     */
    public long getHits() {
        return hits.get();
    }
    
    /**
     * get total misses
     */
    public long getMisses() {
        return misses.get();
    }
    
    /**
     * get total evictions
     */
    public int getEvictions() {
        return evictions.get();
    }
    
    /**
     * check if player is cached
     */
    public boolean isCached(@NotNull UUID uuid) {
        return cache.containsKey(uuid);
    }
    
    /**
     * warm up cache with online players
     */
    public void warmUp() {
        Log.info("warming up cache with online players...");
        
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (var player : Bukkit.getOnlinePlayers()) {
            futures.add(loadPlayer(player.getUniqueId()).thenAccept(data -> {
                if (data == null) {
                    // create new data for player
                    data = new PlayerCustomization(player.getUniqueId(), player.getName());
                    savePlayerData(data);
                }
            }));
        }
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenRun(() -> Log.info("cache warmed up with " + futures.size() + " players"));
    }
}