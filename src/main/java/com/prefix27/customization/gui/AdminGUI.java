package com.prefix27.customization.gui;

import com.prefix27.customization.PlayerCustomizationPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class AdminGUI extends CustomizationGUI {
    
    public AdminGUI(PlayerCustomizationPlugin plugin, Player player) {
        super(plugin, player);
    }
    
    @Override
    public void open() {
        inventory = Bukkit.createInventory(null, 27, "§c§lCustomization Admin");
        
        // Prefix Approval System
        ItemStack prefixApproval = new ItemStack(Material.WRITABLE_BOOK, 1);
        ItemMeta prefixMeta = prefixApproval.getItemMeta();
        prefixMeta.setDisplayName("§6§lPrefix Approval System");
        prefixMeta.setLore(Arrays.asList(
            "§7Review pending custom prefix requests",
            "§7Approve or deny player requests",
            "",
            "§ePending requests: §f0", // Will implement request counting later
            "",
            "§eClick to manage prefix requests!"
        ));
        prefixApproval.setItemMeta(prefixMeta);
        inventory.setItem(10, prefixApproval);
        
        // Player Management
        ItemStack playerManagement = new ItemStack(Material.PLAYER_HEAD, 1);
        ItemMeta playerMeta = playerManagement.getItemMeta();
        playerMeta.setDisplayName("§b§lPlayer Management");
        playerMeta.setLore(Arrays.asList(
            "§7View and modify player data",
            "§7Reset player customizations",
            "",
            "§eClick to manage players!"
        ));
        playerManagement.setItemMeta(playerMeta);
        inventory.setItem(12, playerManagement);
        
        // Analytics & Statistics
        ItemStack analytics = new ItemStack(Material.BOOK, 1);
        ItemMeta analyticsMeta = analytics.getItemMeta();
        analyticsMeta.setDisplayName("§d§lAnalytics & Statistics");
        analyticsMeta.setLore(Arrays.asList(
            "§7View usage statistics",
            "§7Popular colors and prefixes",
            "§7Player activity data",
            "",
            "§eClick to view analytics!"
        ));
        analytics.setItemMeta(analyticsMeta);
        inventory.setItem(14, analytics);
        
        // Configuration Management
        ItemStack config = new ItemStack(Material.REDSTONE, 1);
        ItemMeta configMeta = config.getItemMeta();
        configMeta.setDisplayName("§c§lConfiguration");
        configMeta.setLore(Arrays.asList(
            "§7Reload plugin configuration",
            "§7Manage plugin settings",
            "",
            "§eClick to manage configuration!"
        ));
        config.setItemMeta(configMeta);
        inventory.setItem(16, config);
        
        // Close button
        ItemStack closeButton = new ItemStack(Material.BARRIER, 1);
        ItemMeta closeMeta = closeButton.getItemMeta();
        closeMeta.setDisplayName("§c§lClose");
        closeMeta.setLore(Arrays.asList("§7Close this menu"));
        closeButton.setItemMeta(closeMeta);
        inventory.setItem(26, closeButton);
        
        player.openInventory(inventory);
    }
    
    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        
        if (event.getClickedInventory() != inventory) {
            return;
        }
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        
        playClickSound();
        
        int slot = event.getSlot();
        
        switch (slot) {
            case 10: // Prefix Approval
                player.sendMessage("§aOpening prefix approval system...");
                // Will add the full approval GUI in a future update
                player.sendMessage("§7This feature is coming soon!");
                break;
                
            case 12: // Player Management
                player.sendMessage("§aOpening player management...");
                // Plan to add player data management here
                player.sendMessage("§7This feature is coming soon!");
                break;
                
            case 14: // Analytics
                showAnalytics();
                break;
                
            case 16: // Configuration
                plugin.reloadPluginConfig();
                player.sendMessage("§aPlugin configuration reloaded!");
                playSuccessSound();
                break;
                
            case 26: // Close
                close();
                break;
        }
    }
    
    @Override
    public void handleClose(InventoryCloseEvent event) {
        // GUI closed, nothing special needed
    }
    
    private void showAnalytics() {
        player.sendMessage("§6§l=== Customization Analytics ===");
        player.sendMessage("§7This is a simplified analytics display.");
        player.sendMessage("§7In a full implementation, this would show:");
        player.sendMessage("§7- Most popular colors");
        player.sendMessage("§7- Most used prefixes");
        player.sendMessage("§7- Player activity statistics");
        player.sendMessage("§7- Custom prefix request trends");
        player.sendMessage("§6§l===========================");
    }
}