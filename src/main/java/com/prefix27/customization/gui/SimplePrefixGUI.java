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

public class SimplePrefixGUI {
    
    private final PlayerCustomizationPlugin plugin;
    private final Player player;
    private Inventory inventory;
    
    // Basic solid colors for prefixes
    private static final String[] SOLID_COLORS = {
        "red", "blue", "green", "yellow", "purple", 
        "orange", "pink", "cyan", "white", "gray"
    };
    
    // Gradient options for patron+
    private static final String[] GRADIENTS = {
        "red:blue", "green:yellow", "purple:pink", "gold:orange",
        "red:yellow", "blue:purple", "cyan:white"
    };
    
    public SimplePrefixGUI(PlayerCustomizationPlugin plugin, Player player) {
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
        
        // Supporter: 27 slots (solid prefix colors only)
        // Patron/Devoted: 54 slots (solid colors + gradients)
        boolean hasGradients = rank.equals("patron") || rank.equals("devoted");
        int size = hasGradients ? 54 : 27;
        
        inventory = Bukkit.createInventory(null, size, "§d§lPrefix Selection");
        
        // Add section title for solid colors
        ItemStack solidTitle = new ItemStack(Material.NAME_TAG);
        ItemMeta solidMeta = solidTitle.getItemMeta();
        solidMeta.setDisplayName("§6§lSolid Color Prefixes");
        solidMeta.setLore(Arrays.asList("§7Choose a solid color for your " + rank.toUpperCase() + " prefix"));
        solidTitle.setItemMeta(solidMeta);
        inventory.setItem(4, solidTitle);
        
        // Add solid color prefix options
        String prefixText = getPrefixTextForRank(rank);
        for (int i = 0; i < SOLID_COLORS.length && i < 10; i++) {
            addPrefixOption(prefixText, SOLID_COLORS[i], null, getMaterialForColor(SOLID_COLORS[i]), 9 + i);
        }
        
        // Add gradients for patron+
        if (hasGradients) {
            // Add gradient section title
            ItemStack gradientTitle = new ItemStack(Material.FIREWORK_STAR);
            ItemMeta gradientMeta = gradientTitle.getItemMeta();
            gradientMeta.setDisplayName("§b§lGradient Prefixes");
            gradientMeta.setLore(Arrays.asList("§7Available for Patron rank and above"));
            gradientTitle.setItemMeta(gradientMeta);
            inventory.setItem(31, gradientTitle);
            
            // Add gradient options
            for (int i = 0; i < GRADIENTS.length && i < 14; i++) {
                addPrefixOption(prefixText, null, GRADIENTS[i], Material.PRISMARINE_CRYSTALS, 36 + i);
            }
        }
        
        // Add reset button
        ItemStack resetItem = new ItemStack(Material.BARRIER);
        ItemMeta resetMeta = resetItem.getItemMeta();
        resetMeta.setDisplayName("§c§lRemove Prefix");
        resetMeta.setLore(Arrays.asList("§7Remove your current prefix"));
        resetItem.setItemMeta(resetMeta);
        inventory.setItem(size - 1, resetItem);
        
        player.openInventory(inventory);
    }
    
    private String getPrefixTextForRank(String rank) {
        switch (rank.toLowerCase()) {
            case "supporter": return "[SUPPORTER]";
            case "patron": return "[PATRON]";
            case "devoted": return "[DEVOTED]";
            default: return "[" + rank.toUpperCase() + "]";
        }
    }
    
    private void addPrefixOption(String prefixText, String color, String gradient, Material material, int slot) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        String displayName;
        String preview;
        
        if (gradient != null) {
            String[] colors = gradient.split(":");
            displayName = "§d§l" + prefixText + " §7(" + colors[0].toUpperCase() + " → " + colors[1].toUpperCase() + ")";
            preview = "§7Preview: " + getColorCode(colors[0]) + prefixText + " §f" + player.getName() + "§7: Hello!";
        } else {
            displayName = "§a§l" + prefixText + " §7(" + color.toUpperCase() + ")";
            preview = "§7Preview: " + getColorCode(color) + "§l" + prefixText + " §f" + player.getName() + "§7: Hello!";
        }
        
        meta.setDisplayName(displayName);
        meta.setLore(Arrays.asList(
            "",
            preview,
            "",
            "§eClick to select this prefix!"
        ));
        item.setItemMeta(meta);
        inventory.setItem(slot, item);
    }
    
    private Material getMaterialForColor(String color) {
        switch (color.toLowerCase()) {
            case "red": return Material.RED_CONCRETE;
            case "blue": return Material.BLUE_CONCRETE;
            case "green": return Material.GREEN_CONCRETE;
            case "yellow": return Material.YELLOW_CONCRETE;
            case "purple": return Material.PURPLE_CONCRETE;
            case "orange": return Material.ORANGE_CONCRETE;
            case "pink": return Material.PINK_CONCRETE;
            case "cyan": return Material.CYAN_CONCRETE;
            case "white": return Material.WHITE_CONCRETE;
            case "gray": return Material.GRAY_CONCRETE;
            default: return Material.WHITE_CONCRETE;
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
            plugin.getPlayerDataManager().setPrefix(player.getUniqueId(), null);
            player.sendMessage("§aYour prefix has been removed!");
            player.closeInventory();
            return;
        }
        
        // Title items - do nothing
        if (clicked.getType() == Material.NAME_TAG || clicked.getType() == Material.FIREWORK_STAR) {
            return;
        }
        
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        String rank = data.getRank().toLowerCase();
        
        // Solid color prefix selection (slots 9-18)
        if (slot >= 9 && slot < 19) {
            int colorIndex = slot - 9;
            if (colorIndex < SOLID_COLORS.length) {
                String selectedColor = SOLID_COLORS[colorIndex];
                String prefixId = rank + "_" + selectedColor;
                plugin.getPlayerDataManager().setPrefix(player.getUniqueId(), prefixId);
                player.sendMessage("§aYour prefix has been set to " + getPrefixTextForRank(rank) + " in " + selectedColor + "!");
                player.closeInventory();
            }
            return;
        }
        
        // Gradient prefix selection (slots 36+)
        if (slot >= 36 && slot < 50) {
            int gradientIndex = slot - 36;
            if (gradientIndex < GRADIENTS.length) {
                String selectedGradient = GRADIENTS[gradientIndex];
                String prefixId = rank + "_gradient_" + selectedGradient.replace(":", "_to_");
                plugin.getPlayerDataManager().setPrefix(player.getUniqueId(), prefixId);
                player.sendMessage("§aYour prefix has been set to " + getPrefixTextForRank(rank) + " with " + selectedGradient + " gradient!");
                player.closeInventory();
            }
        }
    }
}