package com.prefix27.customization;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
    
    private final PlayerCustomizationPlugin plugin;
    
    public PlayerListener(PlayerCustomizationPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Get their data ready when they join
        plugin.getPlayerDataManager().loadPlayerData(event.getPlayer().getUniqueId());
        
        // Make sure we have their current username
        plugin.getPlayerDataManager().updatePlayerUsername(event.getPlayer().getUniqueId(), event.getPlayer().getName());
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Save their data when they leave
        plugin.getPlayerDataManager().savePlayerData(event.getPlayer().getUniqueId());
        
        // Clean up cache after a short delay
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getPlayerDataManager().unloadPlayerData(event.getPlayer().getUniqueId());
        }, 100L); // 5 second delay
    }
}