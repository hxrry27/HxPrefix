package com.prefix27.customization.integrations;

import com.prefix27.customization.PlayerCustomizationPlugin;
import com.prefix27.customization.database.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class VentureChatIntegration implements Listener {
    
    private final PlayerCustomizationPlugin plugin;
    
    public VentureChatIntegration(PlayerCustomizationPlugin plugin) {
        this.plugin = plugin;
    }
    
    public boolean initialize() {
        try {
            // Register the event listener
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            plugin.getLogger().info("VentureChat integration initialized successfully!");
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("VentureChat integration failed: " + e.getMessage());
            return false;
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        
        if (playerData == null) {
            return;
        }
        
        // Format the player's display name for chat
        String formattedName = getFormattedPlayerName(playerData);
        
        // Update the event's format to include our custom formatting
        String originalFormat = event.getFormat();
        
        // Replace the player name placeholder with our formatted name
        String newFormat = originalFormat.replace("%1$s", formattedName);
        event.setFormat(newFormat);
    }
    
    private String getFormattedPlayerName(PlayerData playerData) {
        Component fullComponent = buildFullNameComponent(playerData);
        return LegacyComponentSerializer.legacySection().serialize(fullComponent);
    }
    
    private Component buildFullNameComponent(PlayerData playerData) {
        var builder = Component.text();
        
        // Add prefix if available
        if (playerData.hasPrefix()) {
            Component prefixComponent = plugin.getPrefixManager().formatPrefix(
                playerData.getCurrentPrefixId(),
                null, // Use default colors for now
                null
            );
            builder.append(prefixComponent).append(Component.text(" "));
        }
        
        // Add formatted name
        String displayName = playerData.getDisplayName();
        Component nameComponent;
        
        if (playerData.hasNameGradient()) {
            nameComponent = plugin.getColorManager().applyGradient(displayName, playerData.getCurrentNameGradient());
        } else if (playerData.hasNameColor()) {
            if (playerData.getCurrentNameColor().equals("rainbow")) {
                nameComponent = plugin.getColorManager().createRainbowText(displayName);
            } else {
                nameComponent = plugin.getColorManager().applyColor(displayName, playerData.getCurrentNameColor());
            }
        } else {
            nameComponent = Component.text(displayName);
        }
        
        builder.append(nameComponent);
        
        return builder.build();
    }
    
    public String formatPlayerNameForVentureChat(Player player) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return player.getName();
        }
        
        return getFormattedPlayerName(playerData);
    }
    
    public String formatPlayerPrefixForVentureChat(Player player) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null || !playerData.hasPrefix()) {
            return "";
        }
        
        Component prefixComponent = plugin.getPrefixManager().formatPrefix(
            playerData.getCurrentPrefixId(),
            null,
            null
        );
        
        return LegacyComponentSerializer.legacySection().serialize(prefixComponent);
    }
}