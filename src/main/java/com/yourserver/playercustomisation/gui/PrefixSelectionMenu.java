package com.yourserver.playercustomisation.gui;

import com.yourserver.playercustomisation.PlayerCustomisation;
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
        // Get available prefixes for this rank
        List<String> availablePrefixes = plugin.getConfigManager().getAvailablePrefixes(rank);
        
        // Display prefixes in a grid
        int slot = 0;
        for (String prefix : availablePrefixes) {
            if (slot >= 45) break; // Leave bottom row for special items
            
            // Skip slots that would interfere with special items
            if (slot == CUSTOM_TAG_INFO_SLOT) slot++;
            
            addPrefixOption(slot, prefix);
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
    
    private void addPrefixOption(int slot, String prefixText) {
        // Determine material based on prefix
        Material material = getPrefixMaterial(prefixText);
        
        // Format the prefix with brackets
        String formattedPrefix = "[" + prefixText + "]";
        
        // Create preview with player's color
        String preview;
        if (playerColor != null) {
            preview = MenuUtils.applyPlayerColor(playerColor, formattedPrefix) + " &f" + player.getName() + " &7» &fHello!";
        } else {
            preview = "&f" + formattedPrefix + " " + player.getName() + " &7» &fHello!";
        }
        
        // Determine display name based on rank
        String displayName = getColoredPrefix(formattedPrefix);
        
        List<String> lore = Arrays.asList(
            "&7Preview: " + preview,
            "",
            "&eClick to apply!"
        );
        
        // Make special prefixes glow
        ItemStack item;
        if (isSpecialPrefix(prefixText)) {
            item = createGlowingItem(material, displayName, lore);
        } else {
            item = createItem(material, displayName, lore);
        }
        
        setItem(slot, item, (Runnable) () -> {
            applyPrefix(formattedPrefix);
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
    
    private void applyPrefix(String prefix) {
        plugin.getPlayerDataManager().getPlayerData(player.getUniqueId())
            .thenAccept(data -> {
                if (data == null) {
                    data = new PlayerData(player.getUniqueId(), player.getName());
                }
                
                data.setPrefixStyle(prefix);
                
                plugin.getPlayerDataManager().savePlayerData(data).thenRun(() -> {
                    // Play sound
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    
                    // Send message with preview
                    String preview = playerColor != null ? 
                        MenuUtils.applyPlayerColor(playerColor, prefix) : prefix;
                    player.sendMessage(MenuUtils.colorize(
                        "&8[&bCustom&8] &aPrefix set to: " + preview
                    ));
                    
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
    
    private Material getPrefixMaterial(String prefix) {
        // Map prefixes to appropriate materials
        switch (prefix.toUpperCase()) {
            case "VIP":
            case "VIP+":
                return Material.GOLD_INGOT;
            case "ELITE":
            case "PREMIUM":
                return Material.DIAMOND;
            case "SUPPORTER":
            case "PATRON":
            case "DEVOTED":
                return Material.EMERALD;
            case "HERO":
            case "CHAMPION":
            case "WARRIOR":
                return Material.IRON_SWORD;
            case "LEGEND":
            case "MYTHIC":
                return Material.NETHER_STAR;
            case "★":
            case "✦":
            case "✪":
                return Material.FIREWORK_STAR;
            default:
                return Material.NAME_TAG;
        }
    }
    
    private String getColoredPrefix(String prefix) {
        // Apply rank-specific coloring to prefixes
        String upperRank = rank.toUpperCase();
        
        // Special coloring for rank-matching prefixes
        if (prefix.contains(upperRank)) {
            switch (upperRank) {
                case "SUPPORTER":
                    return "&a&l" + prefix;
                case "PATRON":
                    return "&d&l" + prefix;
                case "DEVOTED":
                    return "&5&l" + prefix;
                case "VIP":
                    return "&6&l" + prefix;
                case "PREMIUM":
                    return "&b&l" + prefix;
            }
        }
        
        // Default coloring based on tier
        if (isSpecialPrefix(prefix.replace("[", "").replace("]", ""))) {
            return "&6&l" + prefix;
        }
        
        return "&f&l" + prefix;
    }
    
    private boolean isSpecialPrefix(String prefix) {
        return Arrays.asList("LEGEND", "MYTHIC", "HERO", "CHAMPION", "★", "✦")
            .contains(prefix);
    }
    
    private boolean canUseCustomTags() {
        // TODO: Check with ConfigManager when fully implemented
        return rank.equalsIgnoreCase("devoted") || 
               rank.equalsIgnoreCase("premium");
    }
}