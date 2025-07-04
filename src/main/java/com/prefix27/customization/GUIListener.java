package com.prefix27.customization;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class GUIListener implements Listener {
    
    private final PlayerCustomizationPlugin plugin;
    
    public GUIListener(PlayerCustomizationPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Pass clicks to our GUI manager
        plugin.getGUIManager().handleInventoryClick(event);
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Let the GUI manager handle cleanup
        plugin.getGUIManager().handleInventoryClose(event);
    }
}