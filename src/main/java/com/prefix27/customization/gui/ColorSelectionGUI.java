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

public class ColorSelectionGUI extends CustomizationGUI {
    
    private final PlayerData playerData;
    
    public ColorSelectionGUI(PlayerCustomizationPlugin plugin, Player player, PlayerData playerData) {
        super(plugin, player);
        this.playerData = playerData;
    }
    
    @Override
    public void open() {
        String playerRank = playerData.getRank();
        List<String> availableColors = plugin.getColorManager().getBasicColors();
        
        // Determine inventory size based on rank
        int size = getInventorySize(playerRank);
        inventory = Bukkit.createInventory(null, size, "§6§lColor Selection - " + playerRank);
        
        // Add basic colors
        addBasicColors(availableColors);
        
        // Add gradient options for Patron+
        if (plugin.getPlayerDataManager().canUseGradientFeature(player.getUniqueId())) {
            addGradientOptions();
        }
        
        // Add special options for Devoted
        if (playerRank.equals("devoted")) {
            addDevotedOptions();
        }
        
        // Add back button
        addBackButton();
        
        // Add reset button
        addResetButton();
        
        player.openInventory(inventory);
    }
    
    private int getInventorySize(String rank) {
        switch (rank.toLowerCase()) {
            case "supporter":
                return 18; // 2 rows
            case "patron":
                return 36; // 4 rows
            case "devoted":
                return 54; // 6 rows
            default:
                return 18;
        }
    }
    
    private void addBasicColors(List<String> colors) {
        int slot = 0;
        
        for (String color : colors) {
            if (slot >= 11) break; // Leave space for other items
            
            Material material = getColorMaterial(color);
            ItemStack item = new ItemStack(material, 1);
            ItemMeta meta = item.getItemMeta();
            
            meta.setDisplayName("§" + getColorCode(color) + "§l" + capitalizeFirst(color));
            meta.setLore(Arrays.asList(
                "§7Preview: §" + getColorCode(color) + playerData.getDisplayName(),
                "",
                "§eClick to apply this color!"
            ));
            
            item.setItemMeta(meta);
            inventory.setItem(slot, item);
            slot++;
        }
    }
    
    private void addGradientOptions() {
        List<String> gradients = plugin.getColorManager().getPresetGradients();
        int startSlot = 18; // Second row
        
        // Add gradient section header
        ItemStack gradientHeader = new ItemStack(Material.NETHER_STAR, 1);
        ItemMeta gradientHeaderMeta = gradientHeader.getItemMeta();
        gradientHeaderMeta.setDisplayName("§5§lGradient Options");
        gradientHeaderMeta.setLore(Arrays.asList("§7Choose from preset gradients"));
        gradientHeader.setItemMeta(gradientHeaderMeta);
        inventory.setItem(startSlot, gradientHeader);
        
        int slot = startSlot + 1;
        for (String gradient : gradients) {
            if (slot >= 27) break; // Don't overflow
            
            ItemStack item = new ItemStack(Material.PRISMARINE_CRYSTALS, 1);
            ItemMeta meta = item.getItemMeta();
            
            meta.setDisplayName("§5§lGradient: " + gradient);
            meta.setLore(Arrays.asList(
                "§7Preview: " + plugin.getColorManager().previewGradient(playerData.getDisplayName(), gradient),
                "",
                "§eClick to apply this gradient!"
            ));
            
            item.setItemMeta(meta);
            inventory.setItem(slot, item);
            slot++;
        }
        
        // Add custom gradient builder button
        ItemStack customGradient = new ItemStack(Material.ENCHANTED_BOOK, 1);
        ItemMeta customGradientMeta = customGradient.getItemMeta();
        customGradientMeta.setDisplayName("§5§lCustom Gradient Builder");
        customGradientMeta.setLore(Arrays.asList(
            "§7Create your own gradient",
            "",
            "§eClick to open gradient builder!"
        ));
        customGradient.setItemMeta(customGradientMeta);
        inventory.setItem(slot, customGradient);
    }
    
    private void addDevotedOptions() {
        int startSlot = 36; // Fifth row
        
        // Add hex input option
        ItemStack hexInput = new ItemStack(Material.WRITABLE_BOOK, 1);
        ItemMeta hexInputMeta = hexInput.getItemMeta();
        hexInputMeta.setDisplayName("§6§lHex Color Input");
        hexInputMeta.setLore(Arrays.asList(
            "§7Enter a custom hex color",
            "§7Format: #RRGGBB",
            "",
            "§eClick to enter hex color!"
        ));
        hexInput.setItemMeta(hexInputMeta);
        inventory.setItem(startSlot, hexInput);
        
        // Add rainbow option
        ItemStack rainbow = new ItemStack(Material.FIREWORK_STAR, 1);
        ItemMeta rainbowMeta = rainbow.getItemMeta();
        rainbowMeta.setDisplayName("§c§lR§6§la§e§li§a§ln§b§lb§d§lo§5§lw");
        rainbowMeta.setLore(Arrays.asList(
            "§7Preview: " + "§c" + playerData.getDisplayName().charAt(0) + 
            "§6" + (playerData.getDisplayName().length() > 1 ? playerData.getDisplayName().charAt(1) : "") +
            "§e" + (playerData.getDisplayName().length() > 2 ? playerData.getDisplayName().charAt(2) : "") +
            "§a" + (playerData.getDisplayName().length() > 3 ? playerData.getDisplayName().charAt(3) : "") +
            "§b" + (playerData.getDisplayName().length() > 4 ? playerData.getDisplayName().charAt(4) : "") +
            "§d" + (playerData.getDisplayName().length() > 5 ? playerData.getDisplayName().substring(5) : ""),
            "",
            "§eClick to apply rainbow color!"
        ));
        rainbow.setItemMeta(rainbowMeta);
        inventory.setItem(startSlot + 1, rainbow);
    }
    
    private void addBackButton() {
        ItemStack backButton = new ItemStack(Material.ARROW, 1);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName("§e§lBack");
        backMeta.setLore(Arrays.asList("§7Return to main menu"));
        backButton.setItemMeta(backMeta);
        inventory.setItem(inventory.getSize() - 9, backButton);
    }
    
    private void addResetButton() {
        ItemStack resetButton = new ItemStack(Material.BARRIER, 1);
        ItemMeta resetMeta = resetButton.getItemMeta();
        resetMeta.setDisplayName("§c§lReset Color");
        resetMeta.setLore(Arrays.asList("§7Remove your current color"));
        resetButton.setItemMeta(resetMeta);
        inventory.setItem(inventory.getSize() - 1, resetButton);
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
        
        // Handle back button
        if (slot == inventory.getSize() - 9) {
            setTemporaryClose(true);
            plugin.getGUIManager().openMainCustomizationGUI(player);
            return;
        }
        
        // Handle reset button
        if (slot == inventory.getSize() - 1) {
            plugin.getPlayerDataManager().setNameColor(player.getUniqueId(), null);
            plugin.getPlayerDataManager().setNameGradient(player.getUniqueId(), null);
            player.sendMessage("§aYour name color has been reset!");
            playSuccessSound();
            spawnSuccessParticles();
            setTemporaryClose(true);
            plugin.getGUIManager().openMainCustomizationGUI(player);
            return;
        }
        
        // Handle color selection
        if (slot < 11) {
            String colorName = getColorFromSlot(slot);
            if (colorName != null) {
                plugin.getPlayerDataManager().setNameColor(player.getUniqueId(), colorName);
                player.sendMessage("§aYour name color has been set to " + colorName + "!");
                playSuccessSound();
                spawnApplyChangeParticles();
                setTemporaryClose(true);
                plugin.getGUIManager().openMainCustomizationGUI(player);
            }
            return;
        }
        
        // Handle gradient selection
        if (slot > 18 && slot < 27) {
            String gradientName = getGradientFromSlot(slot);
            if (gradientName != null) {
                plugin.getPlayerDataManager().setNameGradient(player.getUniqueId(), gradientName);
                player.sendMessage("§aYour name gradient has been set to " + gradientName + "!");
                playSuccessSound();
                spawnApplyChangeParticles();
                setTemporaryClose(true);
                plugin.getGUIManager().openMainCustomizationGUI(player);
            }
            return;
        }
        
        // Handle custom gradient builder
        if (slot == 27) {
            setTemporaryClose(true);
            plugin.getGUIManager().openGradientBuilderGUI(player);
            return;
        }
        
        // Handle devoted-specific options
        if (slot == 36) { // Hex input
            player.sendMessage("§aPlease enter a hex color in chat (format: #RRGGBB):");
            player.sendMessage("§7Example: #FF0000 for red");
            // Still need to implement chat input system
            close();
            return;
        }
        
        if (slot == 37) { // Rainbow
            plugin.getPlayerDataManager().setNameColor(player.getUniqueId(), "rainbow");
            player.sendMessage("§aYour name color has been set to rainbow!");
            playSuccessSound();
            spawnApplyChangeParticles();
            setTemporaryClose(true);
            plugin.getGUIManager().openMainCustomizationGUI(player);
            return;
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
    
    private String getColorFromSlot(int slot) {
        List<String> colors = plugin.getColorManager().getBasicColors();
        if (slot >= 0 && slot < colors.size()) {
            return colors.get(slot);
        }
        return null;
    }
    
    private String getGradientFromSlot(int slot) {
        List<String> gradients = plugin.getColorManager().getPresetGradients();
        int gradientIndex = slot - 19; // Adjust for gradient start position
        if (gradientIndex >= 0 && gradientIndex < gradients.size()) {
            return gradients.get(gradientIndex);
        }
        return null;
    }
}