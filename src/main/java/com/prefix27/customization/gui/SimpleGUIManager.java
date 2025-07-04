package com.prefix27.customization.gui;

import com.prefix27.customization.PlayerCustomizationPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SimpleGUIManager implements Listener {
    
    private final PlayerCustomizationPlugin plugin;
    private final Map<UUID, Object> openGUIs; // Player UUID -> GUI object
    
    public SimpleGUIManager(PlayerCustomizationPlugin plugin) {
        this.plugin = plugin;
        this.openGUIs = new HashMap<>();
    }
    
    public void openColorGUI(Player player) {
        closeAnyOpenGUI(player);
        SimpleColorGUI gui = new SimpleColorGUI(plugin, player);
        openGUIs.put(player.getUniqueId(), gui);
        gui.open();
    }
    
    public void openPrefixGUI(Player player) {
        closeAnyOpenGUI(player);
        SimplePrefixGUI gui = new SimplePrefixGUI(plugin, player);
        openGUIs.put(player.getUniqueId(), gui);
        gui.open();
    }
    
    private void closeAnyOpenGUI(Player player) {
        Object existingGUI = openGUIs.remove(player.getUniqueId());
        if (existingGUI != null) {
            player.closeInventory();
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        Object gui = openGUIs.get(player.getUniqueId());
        
        if (gui == null) {
            return;
        }
        
        // Check if this is our GUI by title
        String title = event.getView().getTitle();
        if (!title.contains("Name Colors") && !title.contains("Prefix Selection")) {
            return;
        }
        
        // ALWAYS cancel clicks in our GUIs
        event.setCancelled(true);
        
        // Handle the click
        if (gui instanceof SimpleColorGUI) {
            ((SimpleColorGUI) gui).handleClick(event);
        } else if (gui instanceof SimplePrefixGUI) {
            ((SimplePrefixGUI) gui).handleClick(event);
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        openGUIs.remove(player.getUniqueId());
    }
    
    public void closeAllGUIs() {
        openGUIs.clear();
    }
}