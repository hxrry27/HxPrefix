package com.prefix27.customization.gui;

import com.prefix27.customization.PlayerCustomizationPlugin;
import com.prefix27.customization.database.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class GradientBuilderGUI extends CustomizationGUI {
    
    private final PlayerData playerData;
    private String startColor = "red";
    private String endColor = "blue";
    
    public GradientBuilderGUI(PlayerCustomizationPlugin plugin, Player player, PlayerData playerData) {
        super(plugin, player);
        this.playerData = playerData;
    }
    
    @Override
    public void open() {
        inventory = Bukkit.createInventory(null, 54, "§5§lGradient Builder");
        
        // Start color picker row (slots 0-8)
        addColorPickerRow(0, "§aStart Color", startColor);
        
        // Preview section (slots 18-26)
        addPreviewSection();
        
        // End color picker row (slots 36-44)
        addColorPickerRow(36, "§cEnd Color", endColor);
        
        // Control buttons (bottom row)
        addControlButtons();
        
        player.openInventory(inventory);
    }
    
    private void addColorPickerRow(int startSlot, String title, String selectedColor) {
        List<String> colors = plugin.getColorManager().getBasicColors();
        
        // Add title item
        ItemStack titleItem = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);
        ItemMeta titleMeta = titleItem.getItemMeta();
        titleMeta.setDisplayName(title);
        titleItem.setItemMeta(titleMeta);
        inventory.setItem(startSlot, titleItem);
        
        // Add color options
        for (int i = 0; i < Math.min(8, colors.size()); i++) {
            String color = colors.get(i);
            Material material = getColorMaterial(color);
            ItemStack item = new ItemStack(material, 1);
            ItemMeta meta = item.getItemMeta();
            
            String displayName = "§" + getColorCode(color) + capitalizeFirst(color);
            if (color.equals(selectedColor)) {
                displayName += " §7(Selected)";
            }
            
            meta.setDisplayName(displayName);
            meta.setLore(Arrays.asList("§eClick to select this color!"));
            item.setItemMeta(meta);
            inventory.setItem(startSlot + 1 + i, item);
        }
    }
    
    private void addPreviewSection() {
        // Preview background
        for (int i = 9; i < 18; i++) {
            ItemStack bg = new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1);
            ItemMeta bgMeta = bg.getItemMeta();
            bgMeta.setDisplayName(" ");
            bg.setItemMeta(bgMeta);
            inventory.setItem(i, bg);
        }
        
        // Preview item
        ItemStack preview = new ItemStack(Material.NETHER_STAR, 1);
        ItemMeta previewMeta = preview.getItemMeta();
        previewMeta.setDisplayName("§6§lPreview");
        
        String gradientString = startColor + ":" + endColor;
        String previewText = plugin.getColorManager().previewGradient(playerData.getDisplayName(), gradientString);
        
        previewMeta.setLore(Arrays.asList(
            "§7Your name with gradient:",
            "§f" + previewText,
            "",
            "§7Start: §" + getColorCode(startColor) + startColor,
            "§7End: §" + getColorCode(endColor) + endColor
        ));
        
        preview.setItemMeta(previewMeta);
        inventory.setItem(13, preview);
        
        // Add gradient background
        for (int i = 18; i < 27; i++) {
            ItemStack bg = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE, 1);
            ItemMeta bgMeta = bg.getItemMeta();
            bgMeta.setDisplayName(" ");
            bg.setItemMeta(bgMeta);
            inventory.setItem(i, bg);
        }
        
        // Add separator background
        for (int i = 27; i < 36; i++) {
            ItemStack bg = new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1);
            ItemMeta bgMeta = bg.getItemMeta();
            bgMeta.setDisplayName(" ");
            bg.setItemMeta(bgMeta);
            inventory.setItem(i, bg);
        }
    }
    
    private void addControlButtons() {
        // Save button
        ItemStack saveButton = new ItemStack(Material.EMERALD, 1);
        ItemMeta saveMeta = saveButton.getItemMeta();
        saveMeta.setDisplayName("§a§lSave Gradient");
        saveMeta.setLore(Arrays.asList(
            "§7Apply this gradient to your name",
            "",
            "§7Gradient: §f" + startColor + " → " + endColor
        ));
        saveButton.setItemMeta(saveMeta);
        inventory.setItem(45, saveButton);
        
        // Reset button
        ItemStack resetButton = new ItemStack(Material.YELLOW_DYE, 1);
        ItemMeta resetMeta = resetButton.getItemMeta();
        resetMeta.setDisplayName("§e§lReset");
        resetMeta.setLore(Arrays.asList("§7Reset to default colors"));
        resetButton.setItemMeta(resetMeta);
        inventory.setItem(46, resetButton);
        
        // Random button
        ItemStack randomButton = new ItemStack(Material.FIREWORK_STAR, 1);
        ItemMeta randomMeta = randomButton.getItemMeta();
        randomMeta.setDisplayName("§d§lRandom");
        randomMeta.setLore(Arrays.asList("§7Generate a random gradient"));
        randomButton.setItemMeta(randomMeta);
        inventory.setItem(47, randomButton);
        
        // Cancel button
        ItemStack cancelButton = new ItemStack(Material.BARRIER, 1);
        ItemMeta cancelMeta = cancelButton.getItemMeta();
        cancelMeta.setDisplayName("§c§lCancel");
        cancelMeta.setLore(Arrays.asList("§7Return without saving"));
        cancelButton.setItemMeta(cancelMeta);
        inventory.setItem(53, cancelButton);
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
        
        // Handle start color selection (slots 1-8)
        if (slot >= 1 && slot <= 8) {
            List<String> colors = plugin.getColorManager().getBasicColors();
            int colorIndex = slot - 1;
            if (colorIndex < colors.size()) {
                startColor = colors.get(colorIndex);
                open(); // Refresh GUI
            }
            return;
        }
        
        // Handle end color selection (slots 37-44)
        if (slot >= 37 && slot <= 44) {
            List<String> colors = plugin.getColorManager().getBasicColors();
            int colorIndex = slot - 37;
            if (colorIndex < colors.size()) {
                endColor = colors.get(colorIndex);
                open(); // Refresh GUI
            }
            return;
        }
        
        // Handle control buttons
        switch (slot) {
            case 45: // Save
                String gradientString = startColor + ":" + endColor;
                plugin.getPlayerDataManager().setNameGradient(player.getUniqueId(), gradientString);
                player.sendMessage("§aYour gradient has been applied: " + startColor + " → " + endColor);
                playSuccessSound();
                spawnApplyChangeParticles();
                setTemporaryClose(true);
                plugin.getGUIManager().openMainCustomizationGUI(player);
                break;
                
            case 46: // Reset
                startColor = "red";
                endColor = "blue";
                open(); // Refresh GUI
                break;
                
            case 47: // Random
                List<String> colors = plugin.getColorManager().getBasicColors();
                Random random = new Random();
                startColor = colors.get(random.nextInt(colors.size()));
                endColor = colors.get(random.nextInt(colors.size()));
                open(); // Refresh GUI
                break;
                
            case 53: // Cancel
                setTemporaryClose(true);
                plugin.getGUIManager().openColorSelectionGUI(player);
                break;
        }
    }
    
    @Override
    public void handleClose(InventoryCloseEvent event) {
        // GUI closed, nothing special needed
    }
    
    private Material getColorMaterial(String color) {
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
            case "red": return "c";
            case "blue": return "9";
            case "green": return "a";
            case "yellow": return "e";
            case "purple": return "5";
            case "orange": return "6";
            case "pink": return "d";
            case "cyan": return "b";
            case "white": return "f";
            case "gray": return "7";
            case "black": return "0";
            default: return "f";
        }
    }
    
    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}