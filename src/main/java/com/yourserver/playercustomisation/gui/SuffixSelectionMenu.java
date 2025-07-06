package com.yourserver.playercustomisation.gui;

import com.yourserver.playercustomisation.PlayerCustomisation;
import com.yourserver.playercustomisation.models.PlayerData;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Menu for selecting suffixes
 * Shows symbols and short tags that appear after player names
 */
public class SuffixSelectionMenu extends AbstractMenu {
    private static final int RESET_SLOT = 49;
    
    // Store player's current color for preview
    private String playerColor = null;
    
    public SuffixSelectionMenu(PlayerCustomisation plugin, Player player, String rank) {
        super(plugin, player, rank, "&b&lSuffix Selection &7(" + rank + ")", 54);
        
        // Get player's current color for preview
        loadPlayerColor();
    }
    
    @Override
    protected void build() {
        // Get available suffixes for this rank
        List<String> availableSuffixes = plugin.getConfigManager().getAvailableSuffixes(rank);
        
        // Display suffixes in a grid
        int slot = 0;
        for (String suffix : availableSuffixes) {
            if (slot >= 45) break; // Leave bottom row for special items
            
            addSuffixOption(slot, suffix);
            slot++;
        }
        
        // Add reset button
        setItem(RESET_SLOT, createResetButton("Suffix"), (Runnable) () -> {
            resetSuffix();
        });
        
        // Fill empty slots
        fillEmpty();
    }
    
    private void addSuffixOption(int slot, String suffixText) {
        // Determine material based on suffix
        Material material = getSuffixMaterial(suffixText);
        
        // Create preview with player's color
        String preview;
        if (playerColor != null) {
            preview = "&f" + player.getName() + " " + MenuUtils.applyPlayerColor(playerColor, suffixText);
        } else {
            preview = "&f" + player.getName() + " " + suffixText;
        }
        
        // Style the suffix for display
        String displayName = getStyledSuffix(suffixText);
        
        List<String> lore = Arrays.asList(
            "&7Preview: " + preview,
            "",
            "&eClick to apply!"
        );
        
        // Make special suffixes glow
        ItemStack item;
        if (isSpecialSuffix(suffixText)) {
            item = createGlowingItem(material, displayName, lore);
        } else {
            item = createItem(material, displayName, lore);
        }
        
        setItem(slot, item, (Runnable) () -> {
            applySuffix(suffixText);
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
                    player.sendMessage(MenuUtils.colorize(
                        "&8[&bCustom&8] &aSuffix set to: " + preview
                    ));
                    
                    // Close menu
                    player.closeInventory();
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
                        player.sendMessage(MenuUtils.colorize("&8[&bCustom&8] &aYour suffix has been removed!"));
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
    
    private Material getSuffixMaterial(String suffix) {
        // Special materials for specific suffixes
        switch (suffix) {
            case "★":
                return Material.GOLD_NUGGET;
            case "✦":
            case "✯":
                return Material.FIREWORK_STAR;
            case "♦":
                return Material.DIAMOND;
            case "✓":
                return Material.LIME_DYE;
            case "✪":
                return Material.SUNFLOWER;
            case "⚡":
                return Material.BLAZE_ROD;
            case "♛":
                return Material.GOLDEN_HELMET;
            case "⚔":
                return Material.IRON_SWORD;
            case "☣":
                return Material.SPIDER_EYE;
            case "火": // Fire
                return Material.BLAZE_POWDER;
            case "水": // Water
                return Material.WATER_BUCKET;
            case "金": // Gold
                return Material.GOLD_INGOT;
            case "王": // King
                return Material.GOLDEN_HELMET;
            default:
                // For text suffixes
                if (suffix.matches("[IVX]+")) { // Roman numerals
                    return Material.BOOK;
                } else if (suffix.length() <= 3) { // Short tags
                    return Material.NAME_TAG;
                }
                return Material.PAPER;
        }
    }
    
    private String getStyledSuffix(String suffix) {
        // Apply colors and formatting to suffixes
        switch (suffix) {
            // Symbols get colored
            case "★":
                return "&6&l" + suffix;
            case "✦":
                return "&b&l" + suffix;
            case "♦":
                return "&3&l" + suffix;
            case "✓":
                return "&a&l" + suffix;
            case "✪":
                return "&e&l" + suffix;
            case "⚡":
                return "&6&l" + suffix;
            case "♛":
                return "&d&l" + suffix;
            case "⚔":
                return "&7&l" + suffix;
            case "☣":
                return "&2&l" + suffix;
            case "✧":
                return "&5&l" + suffix;
            
            // Special formatting for certain text
            case "PRO":
                return "&6&lPRO";
            case "GOD":
                return "&c&lGOD";
            case "MVP":
                return "&b&lMVP";
            case "ACE":
                return "&a&lACE";
            case "MAX":
                return "&4&lMAX";
            
            // Social media tags
            case "YT":
                return "&c&lYT";
            case "TV":
                return "&5&lTV";
            case "GG":
                return "&a&lGG";
            
            // Fun suffixes
            case "UwU":
                return "&d&lUwU";
            case "OwO":
                return "&d&lOwO";
            
            // Roman numerals
            case "I":
            case "II":
            case "III":
            case "IV":
            case "V":
            case "X":
                return "&e&l" + suffix;
            
            // Infinity
            case "∞":
                return "&b&l∞";
            
            // Asian characters
            case "火":
                return "&c&l火";
            case "水":
                return "&b&l水";
            case "金":
                return "&6&l金";
            case "王":
                return "&d&l王";
            
            // Bracketed versions
            default:
                if (suffix.startsWith("[") && suffix.endsWith("]")) {
                    return "&7&l" + suffix;
                } else if (suffix.startsWith("{") && suffix.endsWith("}")) {
                    return "&3&l" + suffix;
                } else if (suffix.startsWith("<") && suffix.endsWith(">")) {
                    return "&b&l" + suffix;
                } else if (suffix.startsWith("~") && suffix.endsWith("~")) {
                    return "&d&l" + suffix;
                }
                return "&f&l" + suffix;
        }
    }
    
    private boolean isSpecialSuffix(String suffix) {
        return Arrays.asList("★", "✦", "♦", "♛", "⚔", "∞", "GOD", "MVP", "王")
            .contains(suffix);
    }
}