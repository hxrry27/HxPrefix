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

public class SimpleColorGUI {
    
    private final PlayerCustomizationPlugin plugin;
    private final Player player;
    private Inventory inventory;
    
    public SimpleColorGUI(PlayerCustomizationPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }
    
    public void open() {
        inventory = Bukkit.createInventory(null, 27, "§6§lName Colors");
        
        // Add basic colors
        addColorOption("red", Material.RED_DYE, 0);
        addColorOption("blue", Material.BLUE_DYE, 1);
        addColorOption("green", Material.GREEN_DYE, 2);
        addColorOption("yellow", Material.YELLOW_DYE, 3);
        addColorOption("purple", Material.PURPLE_DYE, 4);
        addColorOption("orange", Material.ORANGE_DYE, 5);
        addColorOption("pink", Material.PINK_DYE, 6);
        addColorOption("cyan", Material.CYAN_DYE, 7);
        addColorOption("white", Material.WHITE_DYE, 8);
        addColorOption("gray", Material.GRAY_DYE, 9);
        addColorOption("black", Material.BLACK_DYE, 10);
        
        // Add reset option
        ItemStack resetItem = new ItemStack(Material.BARRIER, 1);
        ItemMeta resetMeta = resetItem.getItemMeta();
        resetMeta.setDisplayName("§c§lReset Color");
        resetMeta.setLore(Arrays.asList("§7Remove your name color"));
        resetItem.setItemMeta(resetMeta);
        inventory.setItem(26, resetItem);
        
        player.openInventory(inventory);
    }
    
    private void addColorOption(String color, Material material, int slot) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        
        // Get preview
        String preview = plugin.getColorManager().previewColor(player.getName(), color);
        
        meta.setDisplayName("§a§l" + color.toUpperCase());
        meta.setLore(Arrays.asList(
            "",
            preview,
            "",
            "§eClick to select this color!"
        ));
        item.setItemMeta(meta);
        inventory.setItem(slot, item);
    }
    
    public void handleClick(InventoryClickEvent event) {
        // ALWAYS cancel to prevent item theft
        event.setCancelled(true);
        
        if (event.getClickedInventory() != inventory) {
            return;
        }
        
        int slot = event.getSlot();
        
        // Reset button
        if (slot == 26) {
            plugin.getPlayerDataManager().setNameColor(player.getUniqueId(), null);
            plugin.getPlayerDataManager().setNameGradient(player.getUniqueId(), null);
            player.sendMessage("§aYour name color has been reset!");
            player.closeInventory();
            return;
        }
        
        // Color selection
        String[] colors = {"red", "blue", "green", "yellow", "purple", "orange", "pink", "cyan", "white", "gray", "black"};
        if (slot < colors.length) {
            String selectedColor = colors[slot];
            plugin.getPlayerDataManager().setNameColor(player.getUniqueId(), selectedColor);
            player.sendMessage("§aYour name color has been set to " + selectedColor + "!");
            player.closeInventory();
        }
    }
    
    public Inventory getInventory() {
        return inventory;
    }
}