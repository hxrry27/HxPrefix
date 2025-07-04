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

public class PrefixSelectionGUI extends CustomizationGUI {
    
    private final PlayerData playerData;
    
    public PrefixSelectionGUI(PlayerCustomizationPlugin plugin, Player player, PlayerData playerData) {
        super(plugin, player);
        this.playerData = playerData;
    }
    
    @Override
    public void open() {
        inventory = Bukkit.createInventory(null, 54, "§6§lPrefix Selection");
        
        List<String> availablePrefixes = plugin.getPrefixManager().getAvailablePrefixes(player.getUniqueId());
        
        int slot = 0;
        for (String prefixId : availablePrefixes) {
            if (slot >= 45) break; // Leave space for navigation
            
            addPrefixOption(prefixId, slot);
            slot++;
        }
        
        // Add custom prefix request option for Devoted players
        if (plugin.getPlayerDataManager().canUseCustomPrefix(player.getUniqueId())) {
            addCustomPrefixOption();
        }
        
        // Add back button
        addBackButton();
        
        // Add reset button
        addResetButton();
        
        player.openInventory(inventory);
    }
    
    private void addPrefixOption(String fullPrefixId, int slot) {
        ItemStack item = new ItemStack(Material.NAME_TAG, 1);
        ItemMeta meta = item.getItemMeta();
        
        // Parse the display name from the prefix ID
        String displayName = getPrefixDisplayName(fullPrefixId);
        if (fullPrefixId.equals(playerData.getCurrentPrefixId())) {
            displayName = "§2§l" + displayName + " §7(Current)";
        } else {
            displayName = "§a§l" + displayName;
        }
        
        meta.setDisplayName(displayName);
        
        // Create lore with preview
        List<String> lore = Arrays.asList(
            "§7Type: " + getPrefixTypeDescription(fullPrefixId),
            "",
            "§7Preview: " + plugin.getPrefixManager().previewPrefix(fullPrefixId, null, null, playerData.getDisplayName()),
            "",
            "§eClick to select this prefix!"
        );
        
        meta.setLore(lore);
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
    
    private String getPrefixTypeDescription(String fullPrefixId) {
        if (fullPrefixId.startsWith("event_")) {
            return "Event Prefix";
        } else if (fullPrefixId.contains("_gradient_")) {
            return "Rank Prefix with Gradient";
        } else if (fullPrefixId.contains("_")) {
            return "Rank Prefix with Color";
        } else {
            return "Basic Prefix";
        }
    }
    
    private void addCustomPrefixOption() {
        ItemStack customItem = new ItemStack(Material.WRITABLE_BOOK, 1);
        ItemMeta customMeta = customItem.getItemMeta();
        customMeta.setDisplayName("§6§lRequest Custom Prefix");
        customMeta.setLore(Arrays.asList(
            "§7Request a custom prefix",
            "§7Requires staff approval",
            "",
            "§7Cooldown: §f30 days between requests",
            "",
            "§eClick to request a custom prefix!"
        ));
        customItem.setItemMeta(customMeta);
        inventory.setItem(49, customItem);
    }
    
    private void addBackButton() {
        ItemStack backButton = new ItemStack(Material.ARROW, 1);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName("§e§lBack");
        backMeta.setLore(Arrays.asList("§7Return to main menu"));
        backButton.setItemMeta(backMeta);
        inventory.setItem(45, backButton);
    }
    
    private void addResetButton() {
        ItemStack resetButton = new ItemStack(Material.BARRIER, 1);
        ItemMeta resetMeta = resetButton.getItemMeta();
        resetMeta.setDisplayName("§c§lReset Prefix");
        resetMeta.setLore(Arrays.asList("§7Remove your current prefix"));
        resetButton.setItemMeta(resetMeta);
        inventory.setItem(53, resetButton);
    }
    
    @Override
    protected void onInventoryClick(InventoryClickEvent event) {
        // Event is already cancelled in base class
        
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
        if (slot == 45) {
            setTemporaryClose(true);
            plugin.getGUIManager().openMainCustomizationGUI(player);
            return;
        }
        
        // Handle reset button
        if (slot == 53) {
            plugin.getPlayerDataManager().setPrefix(player.getUniqueId(), null);
            player.sendMessage("§aYour prefix has been reset!");
            playSuccessSound();
            spawnSuccessParticles();
            setTemporaryClose(true);
            plugin.getGUIManager().openMainCustomizationGUI(player);
            return;
        }
        
        // Handle custom prefix request
        if (slot == 49) {
            if (plugin.getPlayerDataManager().canUseCustomPrefix(player.getUniqueId())) {
                player.sendMessage("§aPlease enter your custom prefix in chat:");
                player.sendMessage("§7It will be reviewed by staff for approval.");
                // Need to add chat input handling system
                close();
            } else {
                player.sendMessage("§cYou need Devoted rank to request custom prefixes!");
                playErrorSound();
            }
            return;
        }
        
        // Handle prefix selection
        if (slot < 45) {
            List<String> availablePrefixes = plugin.getPrefixManager().getAvailablePrefixes(player.getUniqueId());
            if (slot < availablePrefixes.size()) {
                String selectedPrefix = availablePrefixes.get(slot);
                
                if (plugin.getPrefixManager().canUsePrefix(player.getUniqueId(), selectedPrefix)) {
                    plugin.getPlayerDataManager().setPrefix(player.getUniqueId(), selectedPrefix);
                    player.sendMessage("§aYour prefix has been set to: " + selectedPrefix);
                    playSuccessSound();
                    spawnApplyChangeParticles();
                    setTemporaryClose(true);
                    plugin.getGUIManager().openMainCustomizationGUI(player);
                } else {
                    player.sendMessage("§cYou don't have permission to use this prefix!");
                    playErrorSound();
                }
            }
        }
    }
    
    @Override
    public void handleClose(InventoryCloseEvent event) {
        // GUI closed, nothing special needed
    }
}