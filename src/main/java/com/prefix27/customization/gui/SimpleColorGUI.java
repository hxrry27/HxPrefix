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

public class SimpleColorGUI {
    
    private final PlayerCustomizationPlugin plugin;
    private final Player player;
    private Inventory inventory;
    
    // Basic solid colors available to all supporters
    private static final String[] SOLID_COLORS = {
        "red", "blue", "green", "yellow", "purple", 
        "orange", "pink", "cyan", "white", "gray", "black"
    };
    
    // Gradient options for patron+
    private static final String[] GRADIENTS = {
        "red:blue", "green:yellow", "purple:pink", "gold:orange",
        "red:yellow", "blue:purple", "cyan:white"
    };
    
    public SimpleColorGUI(PlayerCustomizationPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }
    
    public void open() {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage("§cError: Player data not found!");
            return;
        }
        
        String rank = data.getRank().toLowerCase();
        
        // Supporter: 18 slots (solid colors only)
        // Patron/Devoted: 36 slots (solid colors + gradients)
        boolean hasGradients = rank.equals("patron") || rank.equals("devoted");
        int size = hasGradients ? 36 : 18;
        
        inventory = Bukkit.createInventory(null, size, "§6§lName Colors");
        
        // Add solid colors (available to all)
        for (int i = 0; i < SOLID_COLORS.length && i < 11; i++) {
            addColorOption(SOLID_COLORS[i], getMaterialForColor(SOLID_COLORS[i]), i);
        }
        
        // Add gradients for patron+
        if (hasGradients) {
            // Add gradient title item
            ItemStack gradientTitle = new ItemStack(Material.PRISMARINE_CRYSTALS);
            ItemMeta gradientMeta = gradientTitle.getItemMeta();
            gradientMeta.setDisplayName("§b§lGradient Colors");
            gradientMeta.setLore(Arrays.asList("§7Available for Patron rank and above"));
            gradientTitle.setItemMeta(gradientMeta);
            inventory.setItem(18, gradientTitle);
            
            // Add gradient options
            for (int i = 0; i < GRADIENTS.length && i < 14; i++) {
                addGradientOption(GRADIENTS[i], Material.FIREWORK_STAR, 19 + i);
            }
        }
        
        // Add reset button
        ItemStack resetItem = new ItemStack(Material.BARRIER);
        ItemMeta resetMeta = resetItem.getItemMeta();
        resetMeta.setDisplayName("§c§lReset Color");
        resetMeta.setLore(Arrays.asList("§7Remove your name color"));
        resetItem.setItemMeta(resetMeta);
        inventory.setItem(size - 1, resetItem);
        
        player.openInventory(inventory);
    }
    
    private void addColorOption(String color, Material material, int slot) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName("§a§l" + color.toUpperCase());
        meta.setLore(Arrays.asList(
            "",
            "§7Preview: " + getColorCode(color) + player.getName(),
            "",
            "§eClick to select this color!"
        ));
        item.setItemMeta(meta);
        inventory.setItem(slot, item);
    }
    
    private void addGradientOption(String gradient, Material material, int slot) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        String[] colors = gradient.split(":");
        String displayName = colors[0].toUpperCase() + " → " + colors[1].toUpperCase();
        
        meta.setDisplayName("§d§l" + displayName);
        meta.setLore(Arrays.asList(
            "",
            "§7Gradient from " + colors[0] + " to " + colors[1],
            "§7Preview: " + getColorCode(colors[0]) + player.getName(),
            "",
            "§eClick to select this gradient!"
        ));
        item.setItemMeta(meta);
        inventory.setItem(slot, item);
    }
    
    private Material getMaterialForColor(String color) {
        switch (color.toLowerCase()) {
            case "red": return Material.RED_DYE;
            case "blue": return Material.BLUE_DYE;
            case "green": return Material.GREEN_DYE;
            case "yellow": return Material.YELLOW_DYE;
            case "purple": return Material.PURPLE_DYE;
            case "orange": return Material.ORANGE_DYE;
            case "pink": return Material.PINK_DYE;
            case "cyan": return Material.CYAN_DYE;
            case "white": return Material.WHITE_DYE;
            case "gray": return Material.GRAY_DYE;
            case "black": return Material.BLACK_DYE;
            default: return Material.WHITE_DYE;
        }
    }
    
    private String getColorCode(String color) {
        switch (color.toLowerCase()) {
            case "red": return "§c";
            case "blue": return "§9";
            case "green": return "§a";
            case "yellow": return "§e";
            case "purple": return "§5";
            case "orange": return "§6";
            case "pink": return "§d";
            case "cyan": return "§b";
            case "white": return "§f";
            case "gray": return "§7";
            case "black": return "§0";
            default: return "§f";
        }
    }
    
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        
        if (event.getClickedInventory() != inventory) {
            return;
        }
        
        int slot = event.getSlot();
        ItemStack clicked = event.getCurrentItem();
        
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        
        // Reset button
        if (clicked.getType() == Material.BARRIER) {
            plugin.getPlayerDataManager().setNameColor(player.getUniqueId(), null);
            plugin.getPlayerDataManager().setNameGradient(player.getUniqueId(), null);
            player.sendMessage("§aYour name color has been reset!");
            player.closeInventory();
            return;
        }
        
        // Gradient title item - do nothing
        if (clicked.getType() == Material.PRISMARINE_CRYSTALS) {
            return;
        }
        
        // Solid color selection
        if (slot < SOLID_COLORS.length) {
            String selectedColor = SOLID_COLORS[slot];
            plugin.getPlayerDataManager().setNameColor(player.getUniqueId(), selectedColor);
            plugin.getPlayerDataManager().setNameGradient(player.getUniqueId(), null); // Clear gradient
            player.sendMessage("§aYour name color has been set to " + selectedColor + "!");
            player.closeInventory();
            return;
        }
        
        // Gradient selection (patron+)
        if (slot >= 19 && slot < 19 + GRADIENTS.length) {
            int gradientIndex = slot - 19;
            if (gradientIndex < GRADIENTS.length) {
                String selectedGradient = GRADIENTS[gradientIndex];
                plugin.getPlayerDataManager().setNameGradient(player.getUniqueId(), selectedGradient);
                plugin.getPlayerDataManager().setNameColor(player.getUniqueId(), null); // Clear solid color
                player.sendMessage("§aYour name gradient has been set to " + selectedGradient + "!");
                player.closeInventory();
            }
        }
    }
}