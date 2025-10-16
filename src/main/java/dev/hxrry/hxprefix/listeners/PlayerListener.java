package dev.hxrry.hxprefix.listeners;

import dev.hxrry.hxcore.utils.Log;

import dev.hxrry.hxprefix.HxPrefix;
import dev.hxrry.hxprefix.api.models.PlayerCustomization;

import io.papermc.paper.event.player.AsyncChatEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * main player event listener
 */
public class PlayerListener implements Listener {
    private final HxPrefix plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    @SuppressWarnings("unused")
    private final PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();
    
    public PlayerListener(@NotNull HxPrefix plugin) {
        this.plugin = plugin;
    }
    
    /**
     * handle player join
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // load player data async
        CompletableFuture.runAsync(() -> {
            PlayerCustomization data = plugin.getDataCache().getOrCreatePlayerData(player.getUniqueId());
            
            // update username if changed
            if (!player.getName().equals(data.getUsername())) {
                data.setUsername(player.getName());
                plugin.getDataCache().savePlayerData(data);
                Log.debug("updated username for " + player.getUniqueId() + " to " + player.getName());
            }
            
        });
        
        // reload luckperms data if needed
        if (plugin.getLuckPermsHook() != null) {
            plugin.getLuckPermsHook().reloadUser(player.getUniqueId());
        }
    }
    
    /**
     * handle player quit
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // save player data
        PlayerCustomization data = plugin.getDataCache().getPlayerData(player.getUniqueId());
        if (data != null && data.hasCustomizations()) {
            plugin.getDataCache().savePlayerData(data);
        }

    }
    
    /**
     * handle chat messages
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAsyncChat(@NotNull AsyncChatEvent event) {
        Player player = event.getPlayer();
        
        // check if chat formatting is enabled
        if (!plugin.getConfigManager().getMainConfig().getBoolean("chat.format-enabled", true)) {
            return;
        }
        
        // get player data
        PlayerCustomization data = plugin.getDataCache().getPlayerData(player.getUniqueId());
        if (data == null) {
            return;
        }
        
        // build display name
        Component displayName = buildDisplayName(player, data);
        
        // update the renderer
        event.renderer((source, sourceDisplayName, message, viewer) -> {
            // build chat format
            Component format = mm.deserialize(
                plugin.getConfigManager().getMainConfig()
                    .getString("chat.format", "{displayname} <gray>» <white>{message}")
            );
            
            // replace placeholders
            format = format.replaceText(TextReplacementConfig.builder()
                .matchLiteral("{displayname}")
                .replacement(displayName)
                .build());
            
            format = format.replaceText(TextReplacementConfig.builder()
                .matchLiteral("{message}")
                .replacement(message)
                .build());
            
            return format;
        });
    }
    
    /**
     * build display name for a player
     */
    @NotNull
    private Component buildDisplayName(@NotNull Player player, @NotNull PlayerCustomization data) {
        Component displayName = Component.empty();
        
        // add prefix
        if (data.getPrefix() != null) {
            displayName = displayName.append(mm.deserialize(data.getPrefix()))
                .append(Component.space());
        }
        
        // add name with colour
        String name = data.getNickname() != null ? data.getNickname() : player.getName();
        
        if (data.getNameColour() != null) {
            String coloured = plugin.getConfigManager().getStyleConfig()
                .formatWithColour(data.getNameColour(), name);
            displayName = displayName.append(mm.deserialize(coloured));
        } else {
            displayName = displayName.append(Component.text(name, NamedTextColor.WHITE));
        }
        
        // add suffix
        if (data.getSuffix() != null) {
            displayName = displayName.append(Component.space())
                .append(mm.deserialize(data.getSuffix()));
        }
        
        return displayName;
    }
    
    
}