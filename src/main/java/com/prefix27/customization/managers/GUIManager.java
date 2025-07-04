package com.prefix27.customization.managers;

import com.prefix27.customization.PlayerCustomizationPlugin;
import com.prefix27.customization.database.PlayerData;
import com.prefix27.customization.gui.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GUIManager {
    
    private final PlayerCustomizationPlugin plugin;
    private final Map<UUID, CustomizationGUI> openGUIs;
    
    public GUIManager(PlayerCustomizationPlugin plugin) {
        this.plugin = plugin;
        this.openGUIs = new HashMap<>();
    }
    
    public void openMainCustomizationGUI(Player player) {
        // Always remove any existing GUI first to prevent tracking issues  
        CustomizationGUI existingGUI = openGUIs.remove(player.getUniqueId());
        if (existingGUI != null) {
            existingGUI.setTemporaryClose(false); // Reset flag
        }
        
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            player.sendMessage("§cLoading your customization data, please wait...");
            // Try again after a short delay
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                PlayerData delayedData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
                if (delayedData == null) {
                    player.sendMessage("§cError loading your customization data. Please try again.");
                } else {
                    MainCustomizationGUI gui = new MainCustomizationGUI(plugin, player, delayedData);
                    openGUIs.put(player.getUniqueId(), gui);
                    gui.open();
                }
            }, 20L); // Wait 1 second
            return;
        }
        
        MainCustomizationGUI gui = new MainCustomizationGUI(plugin, player, playerData);
        openGUIs.put(player.getUniqueId(), gui);
        gui.open();
    }
    
    public void openColorSelectionGUI(Player player) {
        // Always remove any existing GUI first to prevent tracking issues
        CustomizationGUI existingGUI = openGUIs.remove(player.getUniqueId());
        if (existingGUI != null) {
            existingGUI.setTemporaryClose(false); // Reset flag
        }
        
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            player.sendMessage("§cLoading your customization data, please wait...");
            // Try again after a short delay
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                PlayerData delayedData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
                if (delayedData == null) {
                    player.sendMessage("§cError loading your customization data. Please try again.");
                } else {
                    ColorSelectionGUI gui = new ColorSelectionGUI(plugin, player, delayedData);
                    openGUIs.put(player.getUniqueId(), gui);
                    gui.open();
                }
            }, 20L); // Wait 1 second
            return;
        }
        
        ColorSelectionGUI gui = new ColorSelectionGUI(plugin, player, playerData);
        openGUIs.put(player.getUniqueId(), gui);
        gui.open();
    }
    
    public void openPrefixSelectionGUI(Player player) {
        // Always remove any existing GUI first to prevent tracking issues
        CustomizationGUI existingGUI = openGUIs.remove(player.getUniqueId());
        if (existingGUI != null) {
            existingGUI.setTemporaryClose(false); // Reset flag
        }
        
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            player.sendMessage("§cError loading your customization data. Please try again.");
            return;
        }
        
        PrefixSelectionGUI gui = new PrefixSelectionGUI(plugin, player, playerData);
        openGUIs.put(player.getUniqueId(), gui);
        gui.open();
    }
    
    public void openGradientBuilderGUI(Player player) {
        // Always remove any existing GUI first to prevent tracking issues
        CustomizationGUI existingGUI = openGUIs.remove(player.getUniqueId());
        if (existingGUI != null) {
            existingGUI.setTemporaryClose(false); // Reset flag
        }
        
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            player.sendMessage("§cError loading your customization data. Please try again.");
            return;
        }
        
        if (!plugin.getPlayerDataManager().canUseGradientFeature(player.getUniqueId())) {
            player.sendMessage("§cYou need Patron rank or higher to use gradient colors!");
            return;
        }
        
        GradientBuilderGUI gui = new GradientBuilderGUI(plugin, player, playerData);
        openGUIs.put(player.getUniqueId(), gui);
        gui.open();
    }
    
    public void openAdminGUI(Player player) {
        // Always remove any existing GUI first to prevent tracking issues
        CustomizationGUI existingGUI = openGUIs.remove(player.getUniqueId());
        if (existingGUI != null) {
            existingGUI.setTemporaryClose(false); // Reset flag
        }
        
        if (!player.hasPermission("customization.admin")) {
            player.sendMessage("§cYou don't have permission to access the admin GUI!");
            return;
        }
        
        AdminGUI gui = new AdminGUI(plugin, player);
        openGUIs.put(player.getUniqueId(), gui);
        gui.open();
    }
    
    public void handleInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        UUID playerUUID = player.getUniqueId();
        
        CustomizationGUI gui = openGUIs.get(playerUUID);
        if (gui != null && gui.getInventory().equals(event.getClickedInventory())) {
            // This is one of our tracked GUIs - handle the click
            gui.handleClick(event);
        }
    }
    
    public void handleInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        
        CustomizationGUI gui = openGUIs.get(playerUUID);
        if (gui != null) {
            gui.handleClose(event);
            
            // Remove from tracking if it's not a temporary close
            if (!gui.isTemporaryClose()) {
                openGUIs.remove(playerUUID);
            }
        }
    }
    
    public void closeGUI(Player player) {
        UUID playerUUID = player.getUniqueId();
        CustomizationGUI gui = openGUIs.get(playerUUID);
        
        if (gui != null) {
            gui.close();
            openGUIs.remove(playerUUID);
        }
    }
    
    public void closeAllGUIs() {
        for (CustomizationGUI gui : openGUIs.values()) {
            gui.close();
        }
        openGUIs.clear();
    }
    
    public boolean hasOpenGUI(Player player) {
        return openGUIs.containsKey(player.getUniqueId());
    }
    
    public CustomizationGUI getOpenGUI(Player player) {
        return openGUIs.get(player.getUniqueId());
    }
    
    public void reload() {
        // Close all open GUIs when reloading
        closeAllGUIs();
    }
}