package com.yourserver.playercustomisation.gui;

import com.yourserver.playercustomisation.PlayerCustomisation;
import com.yourserver.playercustomisation.config.ConfigManager;
import com.yourserver.playercustomisation.models.PlayerData;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Menu for selecting suffixes
 * Shows symbols and short tags that appear after player names
 * All configuration is loaded from suffix.yml
 */
public class SuffixSelectionMenu extends AbstractMenu {
    // Configuration-loaded slot positions
    private int resetSlot = 49;
    
    // Store player's current color for preview
    private String playerColor = null;
    
    public SuffixSelectionMenu(PlayerCustomisation plugin, Player player, String rank) {
        super(plugin, player, rank, getMenuTitle(plugin, rank), getMenuSize(plugin));
        
        // Load slot positions from config
        loadSlotPositions();
        
        // Get player's current color for preview
        loadPlayerColor();
    }
    
    private static String getMenuTitle(PlayerCustomisation plugin, String rank) {
        ConfigurationSection menuConfig = plugin.getConfigManager().getSuffixMenuConfig()
            .getConfigurationSection("menu");
        String titleFormat = menuConfig != null ? 
            menuConfig.getString("title", "&b&lSuffix Selection &7({rank})") : 
            "&b&lSuffix Selection &7({rank})";
        return titleFormat.replace("{rank}", rank);
    }
    
    private static int getMenuSize(PlayerCustomisation plugin) {
        ConfigurationSection menuConfig = plugin.getConfigManager().getSuffixMenuConfig()
            .getConfigurationSection("menu");
        return menuConfig != null ? menuConfig.getInt("size", 54) : 54;
    }
    
    private void loadSlotPositions() {
        ConfigurationSection suffixConfig = plugin.getConfigManager().getSuffixMenuConfig();
        ConfigurationSection menuConfig = suffixConfig.getConfigurationSection("menu");
        
        if (menuConfig != null) {
            // Load special slots
            ConfigurationSection specialSlots = menuConfig.getConfigurationSection("special-slots");
            if (specialSlots != null) {
                resetSlot = specialSlots.getInt("reset-button", 49);
            }
        }
    }
    
    @Override
    protected void build() {
        // Get available suffixes for this rank
        List<ConfigManager.SuffixOption> availableSuffixes = plugin.getConfigManager().getAvailableSuffixOptions(rank);
        
        // Display suffixes in a grid, automatically finding empty slots
        int slot = 0;
        int addedCount = 0;
        
        for (ConfigManager.SuffixOption suffix : availableSuffixes) {
            // Find next available slot
            while (slot < inventory.getSize()) {
                // Skip special slots
                if (slot == resetSlot) {
                    slot++;
                    continue;
                }
                
                // Skip bottom row (reserved for special items)
                if (slot >= inventory.getSize() - 9) {
                    break;
                }
                
                // Use this slot
                addSuffixOption(slot, suffix);
                slot++;
                addedCount++;
                break;
            }
            
            // Stop if we've filled all available slots
            if (slot >= inventory.getSize() - 9) {
                break;
            }
        }
        
        // Add reset button
        addResetButton();
        
        // Fill empty slots with configured filler
        fillEmptyWithConfiguredFiller();
    }
    
    private void addSuffixOption(int slot, ConfigManager.SuffixOption suffixOption) {
        // Get material from config
        Material material = suffixOption.material;
        
        // Create preview with player's color
        String preview;
        if (playerColor != null) {
            preview = "&f" + player.getName() + " " + MenuUtils.applyPlayerColor(playerColor, suffixOption.value);
        } else {
            preview = "&f" + player.getName() + " " + suffixOption.value;
        }
        
        // Style the suffix using config color
        String displayName = suffixOption.color + suffixOption.value;
        
        List<String> lore = Arrays.asList(
            "&7Preview: " + preview,
            "",
            "&eClick to apply!"
        );
        
        // Use glow from config
        ItemStack item;
        if (suffixOption.glow) {
            item = createGlowingItem(material, displayName, lore);
        } else {
            item = createItem(material, displayName, lore);
        }
        
        setItem(slot, item, (Runnable) () -> {
            applySuffix(suffixOption.value);
        });
    }
    
    private void addResetButton() {
        // Get reset button configuration
        ConfigurationSection menuConfig = plugin.getConfigManager().getSuffixMenuConfig()
            .getConfigurationSection("menu");
        Material material = Material.BARRIER;
        
        if (menuConfig != null) {
            ConfigurationSection resetSection = menuConfig.getConfigurationSection("reset-button");
            if (resetSection != null) {
                String materialName = resetSection.getString("material");
                if (materialName != null) {
                    try {
                        material = Material.valueOf(materialName.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid material for reset button: " + materialName);
                    }
                }
            }
        }
        
        setItem(resetSlot, createItem(material, "&c&lReset Suffix", Arrays.asList(
            "&7Remove your current suffix",
            "",
            "&cClick to reset!"
        )), (Runnable) () -> {
            resetSuffix();
        });
    }
    
    private void applySuffix(String suffix) {
        plugin.getPlayerDataManager().getPlayerData(player.getUniqueId())
            .thenAccept(data -> {
                if (data == null) {
                    data = new PlayerData(player.getUniqueId(), player.getName());
                }
                
                data.setSuffix(suffix);
                
                plugin.getPlayerDataManager().savePlayerData(data).thenRun(() -> {
                    // Play sound
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    
                    // Send message with preview
                    String preview = playerColor != null ? 
                        MenuUtils.applyPlayerColor(playerColor, suffix) : suffix;
                    
                    String message = plugin.getConfigManager().getMessage("suffix.changed");
                    message = message.replace("{value}", preview);
                    player.sendMessage(message);
                    
                    // Close menu
                    player.closeInventory();
                    player.performCommand("pc-internal setsuffix " + suffix);
                });
            });
    }
    
    private void resetSuffix() {
        plugin.getPlayerDataManager().getPlayerData(player.getUniqueId())
            .thenAccept(data -> {
                if (data != null) {
                    data.setSuffix(null);
                    
                    plugin.getPlayerDataManager().savePlayerData(data).thenRun(() -> {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                        player.sendMessage(plugin.getConfigManager().getMessage("suffix.removed"));
                        player.closeInventory();
                        player.performCommand("pc-internal reset suffix");
                    });
                }
            });
    }
    
    private void loadPlayerColor() {
        try {
            PlayerData data = plugin.getPlayerDataManager()
                .getPlayerData(player.getUniqueId()).get();
            if (data != null) {
                playerColor = data.getNameColor();
            }
        } catch (Exception e) {
            // Ignore - will use default preview
        }
    }
    
    private void fillEmptyWithConfiguredFiller() {
        // Get filler configuration
        ConfigurationSection suffixConfig = plugin.getConfigManager().getSuffixMenuConfig();
        ConfigurationSection fillerConfig = suffixConfig.getConfigurationSection("menu.filler");
        
        if (fillerConfig == null) {
            plugin.getLogger().warning("No filler configuration found for suffix menu");
            return;
        }
        
        String materialName = fillerConfig.getString("material");
        if (materialName == null) {
            plugin.getLogger().warning("No filler material specified for suffix menu");
            return;
        }
        
        try {
            Material fillerMaterial = Material.valueOf(materialName.toUpperCase());
            String fillerName = fillerConfig.getString("name", " ");
            
            ItemStack filler = createItem(fillerMaterial, fillerName, null);
            
            // Fill empty slots
            for (int i = 0; i < inventory.getSize(); i++) {
                if (inventory.getItem(i) == null) {
                    inventory.setItem(i, filler);
                }
            }
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid filler material: " + materialName);
        }
    }
}