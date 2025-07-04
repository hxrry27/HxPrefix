package com.prefix27.customization.gui;

import com.prefix27.customization.PlayerCustomizationPlugin;
import com.prefix27.customization.database.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public class SimplePrefixGUI {
    
    private final PlayerCustomizationPlugin plugin;
    private final Player player;
    private Inventory inventory;
    private List<String> availablePrefixes;
    
    public SimplePrefixGUI(PlayerCustomizationPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }
    
    public void open() {
        inventory = Bukkit.createInventory(null, 54, "§6§lPrefix Selection");
        
        // Get available prefixes
        availablePrefixes = plugin.getPrefixManager().getAvailablePrefixes(player.getUniqueId());
        
        // Add prefix options
        for (int i = 0; i < availablePrefixes.size() && i < 45; i++) {
            addPrefixOption(availablePrefixes.get(i), i);
        }
        
        // Add reset option
        ItemStack resetItem = new ItemStack(Material.BARRIER, 1);
        ItemMeta resetMeta = resetItem.getItemMeta();
        resetMeta.setDisplayName("§c§lReset Prefix");
        resetMeta.setLore(Arrays.asList("§7Remove your current prefix"));
        resetItem.setItemMeta(resetMeta);
        inventory.setItem(53, resetItem);
        
        player.openInventory(inventory);
    }
    
    private void addPrefixOption(String fullPrefixId, int slot) {
        ItemStack item = new ItemStack(Material.NAME_TAG, 1);
        ItemMeta meta = item.getItemMeta();
        
        // Get display name
        String displayName = getPrefixDisplayName(fullPrefixId);
        
        // Get preview
        String preview = plugin.getPrefixManager().previewPrefix(fullPrefixId, null, null, player.getName());
        
        meta.setDisplayName("§a§l" + displayName);
        meta.setLore(Arrays.asList(
            "",
            preview,
            "",
            "§eClick to select this prefix!"
        ));
        item.setItemMeta(meta);
        inventory.setItem(slot, item);
    }
    
    private String getPrefixDisplayName(String fullPrefixId) {
        if (fullPrefixId.contains("_gradient_")) {
            String[] parts = fullPrefixId.split("_gradient_", 2);
            String base = parts[0].toUpperCase();
            String gradient = parts.length > 1 ? parts[1].replace("_to_", " → ") : "";
            return base + " (Gradient: " + gradient + ")";
        } else if (fullPrefixId.contains("_")) {
            String[] parts = fullPrefixId.split("_", 2);
            String base = parts[0].toUpperCase();
            String color = parts.length > 1 ? parts[1].toUpperCase() : "";
            return base + " (" + color + ")";
        } else {
            return fullPrefixId.toUpperCase();
        }
    }
    
    public void handleClick(InventoryClickEvent event) {
        // ALWAYS cancel to prevent item theft
        event.setCancelled(true);
        
        if (event.getClickedInventory() != inventory) {
            return;
        }
        
        int slot = event.getSlot();
        
        // Reset button
        if (slot == 53) {
            plugin.getPlayerDataManager().setPrefix(player.getUniqueId(), null);
            player.sendMessage("§aYour prefix has been reset!");
            player.closeInventory();
            return;
        }
        
        // Prefix selection
        if (slot < availablePrefixes.size()) {
            String selectedPrefix = availablePrefixes.get(slot);
            plugin.getPlayerDataManager().setPrefix(player.getUniqueId(), selectedPrefix);
            player.sendMessage("§aYour prefix has been set!");
            player.closeInventory();
        }
    }
    
    public Inventory getInventory() {
        return inventory;
    }
}