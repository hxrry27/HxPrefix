package com.yourserver.playercustomisation.listeners;

import com.yourserver.playercustomisation.PlayerCustomisation;
import com.yourserver.playercustomisation.models.PlayerData;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {
    private final PlayerCustomisation plugin;

    public PlayerJoinListener(PlayerCustomisation plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Always fetch latest data from MySQL on join
        plugin.getPlayerDataManager().getPlayerData(player.getUniqueId())
            .thenAccept(data -> {
                if (data == null) {
                    // Create new player data
                    PlayerData newData = new PlayerData(player.getUniqueId(), player.getName());
                    plugin.getPlayerDataManager().savePlayerData(newData);
                } else {
                    // Update username if changed
                    if (!data.getUsername().equals(player.getName())) {
                        data.setUsername(player.getName());
                        plugin.getPlayerDataManager().savePlayerData(data);
                    }
                }
                
                // Update nametag - with debug
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (plugin.getConfig().getBoolean("nametags.enabled", true)) {
                        plugin.getLogger().info("Updating nametag for " + player.getName());
                        plugin.getNametagManager().updateNametag(player);
                    } else {
                        plugin.getLogger().info("Nametags disabled in config");
                    }
                }, 20L); // 1 second delay
            });
    }
}