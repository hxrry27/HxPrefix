package com.yourserver.playercustomisation.gui;

import com.yourserver.playercustomisation.PlayerCustomisation;
import com.yourserver.playercustomisation.config.ConfigManager;
import com.yourserver.playercustomisation.models.PlayerData;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Menu for selecting prefixes
 * Adapts based on player rank and shows appropriate options
 */
public class PrefixSelectionMenu extends AbstractMenu {
    private static final int RESET_SLOT = 49;
    private static final int CUSTOM_TAG_INFO_SLOT = 43;
    
    // Store player's current color for preview
    private String playerColor = null;
    
    public PrefixSelectionMenu(PlayerCustomisation plugin, Player player, String rank) {
        super(plugin, player, rank, "&d&lPrefix Selection &7(" + rank + ")", 54);
        
        // Get player's current color for preview
        loadPlayerColor();
    }
    
    @Override
    protected void build() {
        // Get available prefix options for this rank
        List<ConfigManager.PrefixOption> availableOptions = plugin.getConfigManager().getAvailablePrefixOptions(rank);
        
        // Display prefixes in a grid
        int slot = 0;
        for (ConfigManager.PrefixOption option : availableOptions) {
            if (slot >= 45) break; // Leave bottom row for special items
            
            // Skip slot for custom tag info if needed
            if (slot == CUSTOM_TAG_INFO_SLOT && canUseCustomTags()) slot++;
            
            addPrefixOption(slot, option);
            slot++;
        }
        
        // Add custom tag info for ranks that support it
        if (canUseCustomTags()) {
            addCustomTagInfo();
        }
        
        // Add reset button
        setItem(RESET_SLOT, createResetButton("Prefix"), (Runnable) () -> {
            resetPrefix();
        });
        
        // Fill empty slots
        fillEmpty();
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
        List<String> lore = Arrays.asList(
            "&7Want a custom prefix like &b[LEGEND]&7?",
            "&7Use &f/requesttag <name> &7to request one!",
            "",
            "&7Staff will review your request.",
            "",
            "&cClick to learn more!"
        );
        
        setItem(CUSTOM_TAG_INFO_SLOT, createItem(Material.WRITABLE_BOOK, 
            "&e&lCustom Tags", lore), () -> {
            player.closeInventory();
            player.sendMessage(MenuUtils.colorize("&8[&bCustom&8] &7Use &f/requesttag <name> &7to request a custom prefix!"));
            player.sendMessage(MenuUtils.colorize("&8[&bCustom&8] &7Example: &f/requesttag LEGEND"));
            player.sendMessage(MenuUtils.colorize("&8[&bCustom&8] &7Staff will review and approve/deny your request."));
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
                    String message = plugin.getConfigManager().getMessage("prefix-changed");
                    message = message.replace("{value}", preview);
                    player.sendMessage(MenuUtils.colorize(message));
                    
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
                        player.sendMessage(MenuUtils.colorize("&8[&bCustom&8] &aYour prefix has been removed!"));
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
}