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
 * Menu for selecting prefixes
 * Adapts based on player rank and shows appropriate options
 * All configuration is loaded from prefix.yml
 */
public class PrefixSelectionMenu extends AbstractMenu {
    // Configuration-loaded slot positions
    private int resetSlot = 49;
    private int customTagInfoSlot = 43;
    
    // Store player's current color for preview
    private String playerColor = null;
    
    public PrefixSelectionMenu(PlayerCustomisation plugin, Player player, String rank) {
        super(plugin, player, rank, getMenuTitle(plugin, rank), getMenuSize(plugin));
        
        // Load slot positions from config
        loadSlotPositions();
        
        // Get player's current color for preview
        loadPlayerColor();
    }
    
    private static String getMenuTitle(PlayerCustomisation plugin, String rank) {
        ConfigurationSection menuConfig = plugin.getConfigManager().getPrefixMenuConfig()
            .getConfigurationSection("menu");
        String titleFormat = menuConfig != null ? 
            menuConfig.getString("title", "&d&lPrefix Selection &7({rank})") : 
            "&d&lPrefix Selection &7({rank})";
        return titleFormat.replace("{rank}", rank);
    }
    
    private static int getMenuSize(PlayerCustomisation plugin) {
        ConfigurationSection menuConfig = plugin.getConfigManager().getPrefixMenuConfig()
            .getConfigurationSection("menu");
        return menuConfig != null ? menuConfig.getInt("size", 54) : 54;
    }
    
    private void loadSlotPositions() {
        ConfigurationSection prefixConfig = plugin.getConfigManager().getPrefixMenuConfig();
        ConfigurationSection menuConfig = prefixConfig.getConfigurationSection("menu");
        
        if (menuConfig != null) {
            // Load special slots
            ConfigurationSection specialSlots = menuConfig.getConfigurationSection("special-slots");
            if (specialSlots != null) {
                resetSlot = specialSlots.getInt("reset-button", 49);
                customTagInfoSlot = specialSlots.getInt("custom-tag-info", 43);
            }
        }
    }
    
    @Override
    protected void build() {
        // Get available prefix options for this rank
        List<ConfigManager.PrefixOption> availableOptions = plugin.getConfigManager().getAvailablePrefixOptions(rank);
        
        // Debug logging
        plugin.getLogger().info("Building prefix menu for rank: " + rank);
        plugin.getLogger().info("Found " + availableOptions.size() + " available prefix options");
        
        // Display prefixes in a grid, automatically finding empty slots
        int slot = 0;
        int addedCount = 0;
        
        for (ConfigManager.PrefixOption option : availableOptions) {
            // Debug log each option
            plugin.getLogger().info("Adding prefix option: " + option.name + " in slot " + slot);
            
            // Find next available slot
            while (slot < inventory.getSize()) {
                // Skip special slots
                if (slot == resetSlot || (slot == customTagInfoSlot && canUseCustomTags())) {
                    slot++;
                    continue;
                }
                
                // Skip bottom row (reserved for special items)
                if (slot >= inventory.getSize() - 9) {
                    break;
                }
                
                // Use this slot
                addPrefixOption(slot, option);
                slot++;
                addedCount++;
                break;
            }
            
            // Stop if we've filled all available slots
            if (slot >= inventory.getSize() - 9) {
                break;
            }
        }
        
        plugin.getLogger().info("Added " + addedCount + " prefix options to the menu");
        
        // Add custom tag info for ranks that support it
        if (canUseCustomTags()) {
            addCustomTagInfo();
        }
        
        // Add reset button
        addResetButton();
        
        // Fill empty slots with configured filler
        fillEmptyWithConfiguredFiller();
    }
    
    private void addPrefixOption(int slot, ConfigManager.PrefixOption option) {
        // Create preview - the value already contains MiniMessage formatting
        String preview = MenuUtils.createPreview(option.value, "") + " &f" + player.getName() + " &7» &fHello!";
        
        List<String> lore = Arrays.asList(
            "&7Preview: " + preview,
            "",
            "&eClick to apply!"
        );
        
        // Create the item with proper material
        ItemStack item;
        if (option.glow) {
            item = createGlowingItem(option.material, "&f" + option.name, lore);
        } else {
            item = createItem(option.material, "&f" + option.name, lore);
        }
        
        setItem(slot, item, (Runnable) () -> {
            applyPrefix(option.value);
        });
    }
    
    private void addCustomTagInfo() {
        // Get material from config
        ConfigurationSection menuConfig = plugin.getConfigManager().getPrefixMenuConfig()
            .getConfigurationSection("menu");
        Material material = Material.WRITABLE_BOOK;
        
        if (menuConfig != null) {
            ConfigurationSection customTagSection = menuConfig.getConfigurationSection("custom-tag-info");
            if (customTagSection != null) {
                String materialName = customTagSection.getString("material");
                if (materialName != null) {
                    try {
                        material = Material.valueOf(materialName.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid material for custom tag info: " + materialName);
                    }
                }
            }
        }
        
        List<String> lore = Arrays.asList(
            "&7Want a custom prefix like &b[LEGEND]&7?",
            "&7Use &f/requesttag <name> &7to request one!",
            "",
            "&7Staff will review your request.",
            "",
            "&cClick to learn more!"
        );
        
        setItem(customTagInfoSlot, createItem(material, "&e&lCustom Tags", lore), () -> {
            player.closeInventory();
            player.sendMessage(plugin.getConfigManager().getMessage("tags.info-line1"));
            player.sendMessage(plugin.getConfigManager().getMessage("tags.info-line2"));
            player.sendMessage(plugin.getConfigManager().getMessage("tags.info-line3"));
        });
    }
    
    private void addResetButton() {
        // Get reset button configuration
        ConfigurationSection menuConfig = plugin.getConfigManager().getPrefixMenuConfig()
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
        
        setItem(resetSlot, createItem(material, "&c&lReset Prefix", Arrays.asList(
            "&7Remove your current prefix",
            "",
            "&cClick to reset!"
        )), (Runnable) () -> {
            resetPrefix();
        });
    }
    
    private void applyPrefix(String prefixValue) {
        plugin.getPlayerDataManager().getPlayerData(player.getUniqueId())
            .thenAccept(data -> {
                if (data == null) {
                    data = new PlayerData(player.getUniqueId(), player.getName());
                }
                
                // Store the MiniMessage formatted prefix
                data.setPrefixStyle(prefixValue);
                
                plugin.getPlayerDataManager().savePlayerData(data).thenRun(() -> {
                    // Play sound
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    
                    // Send message with preview
                    String preview = MenuUtils.createPreview(prefixValue, "");
                    String message = plugin.getConfigManager().getMessage("prefix.changed");
                    message = message.replace("{value}", preview);
                    player.sendMessage(message);
                    
                    // Close menu
                    player.closeInventory();
                });
            });
    }
    
    private void resetPrefix() {
        plugin.getPlayerDataManager().getPlayerData(player.getUniqueId())
            .thenAccept(data -> {
                if (data != null) {
                    data.setPrefixStyle(null);
                    
                    plugin.getPlayerDataManager().savePlayerData(data).thenRun(() -> {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                        player.sendMessage(plugin.getConfigManager().getMessage("prefix.removed"));
                        player.closeInventory();
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
    
    private boolean canUseCustomTags() {
        return plugin.getConfigManager().canUseCustomTags(rank);
    }
    
    private void fillEmptyWithConfiguredFiller() {
        // Get filler configuration
        ConfigurationSection prefixConfig = plugin.getConfigManager().getPrefixMenuConfig();
        ConfigurationSection fillerConfig = prefixConfig.getConfigurationSection("menu.filler");
        
        if (fillerConfig == null) {
            plugin.getLogger().warning("No filler configuration found for prefix menu");
            return;
        }
        
        String materialName = fillerConfig.getString("material");
        if (materialName == null) {
            plugin.getLogger().warning("No filler material specified for prefix menu");
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