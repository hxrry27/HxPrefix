package com.yourserver.playercustomisation.listeners;

import com.yourserver.playercustomisation.PlayerCustomisation;
import com.yourserver.playercustomisation.models.PlayerData;
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
        // Always fetch latest data from MySQL on join (as specified)
        plugin.getPlayerDataManager().getPlayerData(event.getPlayer().getUniqueId())
            .thenAccept(data -> {
                if (data == null) {
                    // Create new player data
                    PlayerData newData = new PlayerData(event.getPlayer().getUniqueId(), event.getPlayer().getName());
                    plugin.getPlayerDataManager().savePlayerData(newData);
                } else {
                    // Update username if changed
                    if (!data.getUsername().equals(event.getPlayer().getName())) {
                        data.setUsername(event.getPlayer().getName());
                        plugin.getPlayerDataManager().savePlayerData(data);
                    }
                }
            });
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getNametagManager().updateNametag(event.getPlayer());
        }, 20L);
    }
}