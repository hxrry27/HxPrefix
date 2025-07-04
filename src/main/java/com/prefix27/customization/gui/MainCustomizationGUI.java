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

public class MainCustomizationGUI extends CustomizationGUI {
    
    private final PlayerData playerData;
    
    public MainCustomizationGUI(PlayerCustomizationPlugin plugin, Player player, PlayerData playerData) {
        super(plugin, player);
        this.playerData = playerData;
    }
    
    @Override
    public void open() {
        inventory = Bukkit.createInventory(null, 27, "§6§lCustomization Hub");
        
        // Name Colors (slot 10)
        ItemStack nameColorItem = new ItemStack(Material.REDSTONE, 1);
        ItemMeta nameColorMeta = nameColorItem.getItemMeta();
        nameColorMeta.setDisplayName("§e§lName Colors");
        nameColorMeta.setLore(Arrays.asList(
            "§7Click to customize your name colors",
            "",
            "§7Current: §f" + getCurrentNameColorDisplay(),
            "",
            "§eClick to open color selection!"
        ));
        nameColorItem.setItemMeta(nameColorMeta);
        inventory.setItem(10, nameColorItem);
        
        // Prefixes (slot 16)
        ItemStack prefixItem = new ItemStack(Material.NAME_TAG, 1);
        ItemMeta prefixMeta = prefixItem.getItemMeta();
        prefixMeta.setDisplayName("§a§lPrefixes");
        prefixMeta.setLore(Arrays.asList(
            "§7Choose from available prefixes",
            "",
            "§7Current: §f" + getCurrentPrefixDisplay(),
            "",
            "§eClick to open prefix selection!"
        ));
        prefixItem.setItemMeta(prefixMeta);
        inventory.setItem(16, prefixItem);
        
        // Nickname (slot 22)
        ItemStack nicknameItem = new ItemStack(Material.PAPER, 1);
        ItemMeta nicknameMeta = nicknameItem.getItemMeta();
        nicknameMeta.setDisplayName("§b§lNickname");
        nicknameMeta.setLore(Arrays.asList(
            "§7Set your display name",
            "",
            "§7Current: §f" + getCurrentNicknameDisplay(),
            "",
            "§eUse /nick <name> to set your nickname!"
        ));
        nicknameItem.setItemMeta(nicknameMeta);
        inventory.setItem(22, nicknameItem);
        
        // Info (slot 13)
        ItemStack infoItem = new ItemStack(Material.BOOK, 1);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName("§d§lYour Information");
        infoMeta.setLore(Arrays.asList(
            "§7Your current customization settings",
            "",
            "§7Username: §f" + playerData.getUsername(),
            "§7Rank: §f" + playerData.getRank(),
            "§7Name Color: §f" + getCurrentNameColorDisplay(),
            "§7Prefix: §f" + getCurrentPrefixDisplay(),
            "§7Nickname: §f" + getCurrentNicknameDisplay(),
            "",
            "§7Available Features:",
            getAvailableFeaturesDisplay()
        ));
        infoItem.setItemMeta(infoMeta);
        inventory.setItem(13, infoItem);
        
        // Add gradient option for Patron+ players
        if (plugin.getPlayerDataManager().canUseGradientFeature(player.getUniqueId())) {
            ItemStack gradientItem = new ItemStack(Material.PRISMARINE_CRYSTALS, 1);
            ItemMeta gradientMeta = gradientItem.getItemMeta();
            gradientMeta.setDisplayName("§5§lGradient Builder");
            gradientMeta.setLore(Arrays.asList(
                "§7Create custom gradient colors",
                "",
                "§7Available for Patron+ ranks",
                "",
                "§eClick to open gradient builder!"
            ));
            gradientItem.setItemMeta(gradientMeta);
            inventory.setItem(4, gradientItem);
        }
        
        // Close button (slot 26)
        ItemStack closeItem = new ItemStack(Material.BARRIER, 1);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.setDisplayName("§c§lClose");
        closeMeta.setLore(Arrays.asList("§7Click to close this menu"));
        closeItem.setItemMeta(closeMeta);
        inventory.setItem(26, closeItem);
        
        player.openInventory(inventory);
    }
    
    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        
        if (event.getClickedInventory() != inventory) {
            return;
        }
        
        int slot = event.getSlot();
        playClickSound();
        
        switch (slot) {
            case 10: // Name Colors
                setTemporaryClose(true);
                plugin.getGUIManager().openColorSelectionGUI(player);
                break;
                
            case 16: // Prefixes
                setTemporaryClose(true);
                plugin.getGUIManager().openPrefixSelectionGUI(player);
                break;
                
            case 22: // Nickname
                player.sendMessage("§aUse §e/nick <name>§a to set your nickname!");
                player.sendMessage("§aUse §e/nick reset§a to remove your nickname!");
                break;
                
            case 13: // Info - refresh display
                open();
                break;
                
            case 4: // Gradient Builder
                if (plugin.getPlayerDataManager().canUseGradientFeature(player.getUniqueId())) {
                    setTemporaryClose(true);
                    plugin.getGUIManager().openGradientBuilderGUI(player);
                } else {
                    player.sendMessage("§cYou need Patron rank or higher to use gradient colors!");
                    playErrorSound();
                }
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
    
    private String getCurrentNameColorDisplay() {
        if (playerData.hasNameGradient()) {
            return "Gradient: " + playerData.getCurrentNameGradient();
        } else if (playerData.hasNameColor()) {
            return playerData.getCurrentNameColor();
        } else {
            return "Default";
        }
    }
    
    private String getCurrentPrefixDisplay() {
        if (playerData.hasPrefix()) {
            return playerData.getCurrentPrefixId();
        } else {
            return "None";
        }
    }
    
    private String getCurrentNicknameDisplay() {
        if (playerData.hasNickname()) {
            return playerData.getCurrentNickname();
        } else {
            return "None";
        }
    }
    
    private String getAvailableFeaturesDisplay() {
        StringBuilder features = new StringBuilder();
        
        if (plugin.getPlayerDataManager().canUseColorFeature(player.getUniqueId())) {
            features.append("§a✓ Name Colors");
        } else {
            features.append("§c✗ Name Colors");
        }
        
        features.append(" ");
        
        if (plugin.getPlayerDataManager().canUseGradientFeature(player.getUniqueId())) {
            features.append("§a✓ Gradients");
        } else {
            features.append("§c✗ Gradients");
        }
        
        features.append(" ");
        
        if (plugin.getPlayerDataManager().canUseCustomPrefix(player.getUniqueId())) {
            features.append("§a✓ Custom Prefixes");
        } else {
            features.append("§c✗ Custom Prefixes");
        }
        
        features.append(" ");
        
        if (plugin.getPlayerDataManager().canUseNickname(player.getUniqueId())) {
            features.append("§a✓ Nicknames");
        } else {
            features.append("§c✗ Nicknames");
        }
        
        return features.toString();
    }
}