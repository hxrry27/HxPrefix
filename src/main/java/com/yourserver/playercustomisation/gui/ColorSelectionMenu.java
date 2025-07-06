package com.yourserver.playercustomisation.gui;

import com.yourserver.playercustomisation.PlayerCustomisation;
import com.yourserver.playercustomisation.models.PlayerData;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Menu for selecting name colors
 * Ported from color_patron.yml DeluxeMenus configuration
 */
public class ColorSelectionMenu extends AbstractMenu {
    // Define the layout positions
    private static final int SOLID_TITLE_SLOT = 4;
    private static final int GRADIENT_TITLE_SLOT = 31;
    private static final int RESET_SLOT = 49;
    
    // Solid color slots (row 2)
    private static final int[] SOLID_COLOR_SLOTS = {10, 11, 12, 13, 14, 15, 16};
    
    // Gradient slots (row 5)
    private static final int[] GRADIENT_SLOTS = {37, 38, 39, 40};
    
    // Rainbow slot
    private static final int RAINBOW_SLOT = 41;
    
    // Animation task
    private BukkitRunnable animationTask;
    
    public ColorSelectionMenu(PlayerCustomisation plugin, Player player, String rank) {
        super(plugin, player, rank, "&6&lName Color Selection &7(" + rank + ")", 54);
    }
    
    @Override
    protected void build() {
        // Add section titles
        setItem(SOLID_TITLE_SLOT, createItem(Material.NAME_TAG, "&6&lSolid Colors", 
            Arrays.asList("&7Choose a solid color for your name")));
        
        // Add solid colors (everyone gets these if they have color permission)
        addSolidColors();
        
        // Check if they can use gradients
        if (canUseGradients()) {
            setItem(GRADIENT_TITLE_SLOT, createItem(Material.FIREWORK_STAR, "&d&lGradient Colors", 
                Arrays.asList("&7Available for " + rank + " rank and above")));
            addGradientColors();
        }
        
        // Check if they can use rainbow
        if (canUseRainbow()) {
            addRainbowOption();
        }
        
        // Add reset button
        setItem(RESET_SLOT, createResetButton("Color"), (Runnable) () -> {
            resetColor();
        });
        
        // Fill empty slots
        fillEmpty();
        
        // Start animations
        startAnimations();
    }
    
    private void addSolidColors() {
    Map<String, String> solidColors = plugin.getConfigManager().getSolidColors();
    
        int index = 0;
        for (Map.Entry<String, String> entry : solidColors.entrySet()) {
            if (index >= SOLID_COLOR_SLOTS.length) break;
            
            String colorName = entry.getKey();
            String hexValue = entry.getValue();
            int slot = SOLID_COLOR_SLOTS[index++];
            
            // Determine material based on color
            Material material = MenuUtils.getMaterialForColor(colorName, false, false);
            
            // Create color value for storage
            String colorValue = MenuUtils.hexToMiniMessage(hexValue);
            String preview = MenuUtils.createPreview(colorValue, player.getName());
            
            List<String> lore = Arrays.asList(
                "&7Preview: " + preview,
                "",
                "&eClick to apply!"
            );
            
            // Use the color name as display
            String displayName = MenuUtils.colorize(MenuUtils.toBirdflop(hexValue) + "&l" + colorName);
            
            setItem(slot, createItem(material, displayName, lore), (Runnable) () -> {
                applyColor(colorName, colorValue);
            });
        }
    }
    
    private void addGradientColors() {
    Map<String, List<String>> gradients = plugin.getConfigManager().getGradients();
    
        int index = 0;
        for (Map.Entry<String, List<String>> entry : gradients.entrySet()) {
            if (index >= GRADIENT_SLOTS.length) break;
            
            String gradientName = entry.getKey();
            List<String> colors = entry.getValue();
            int slot = GRADIENT_SLOTS[index++];
            
            // Create gradient value
            String gradientValue = MenuUtils.gradientToMiniMessage(colors.toArray(new String[0]));
            String preview = MenuUtils.createPreview(gradientValue, player.getName());
            
            List<String> lore = Arrays.asList(
                "&7Gradient: " + String.join(" &7→ ", colors),
                "&7Preview: " + preview,
                "",
                "&eClick to apply!"
            );
            
            // Material based on gradient type
            Material material = Material.FIREWORK_STAR;
            
            setItem(slot, createGlowingItem(material, "&d&l" + gradientName, lore), (Runnable) () -> {
                applyColor(gradientName, gradientValue);
            });
        }
    }
    
    private void addRainbowOption() {
        List<String> lore = Arrays.asList(
            "&7Special rainbow gradient!",
            "&7Preview: &c&lR&6&la&e&li&a&ln&b&lb&9&lo&d&lw",
            "",
            "&eClick to apply!"
        );
        
        // Rainbow always glows
        setItem(RAINBOW_SLOT, createGlowingItem(Material.NETHER_STAR, 
            "&c&lR&6&la&e&li&a&ln&b&lb&9&lo&d&lw", lore), (Runnable) () -> {
            applyColor("Rainbow", MenuUtils.rainbowTag());
        });
    }
    
    private void applyColor(String colorName, String colorValue) {
    plugin.getPlayerDataManager().getPlayerData(player.getUniqueId())
        .thenAccept(data -> {
            if (data == null) {
                data = new PlayerData(player.getUniqueId(), player.getName());
            }
            
            data.setNameColor(colorValue);
            
            plugin.getPlayerDataManager().savePlayerData(data).thenRun(() -> {
                // Play sound
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                
                // Use ConfigManager for message
                String message = plugin.getConfigManager().getMessage("color-changed");
                player.sendMessage(MenuUtils.colorize(message));
                
                // Close menu
                player.closeInventory();
            });
        });
    }
    
    private void resetColor() {
        plugin.getPlayerDataManager().getPlayerData(player.getUniqueId())
            .thenAccept(data -> {
                if (data != null) {
                    data.setNameColor(null);
                    
                    plugin.getPlayerDataManager().savePlayerData(data).thenRun(() -> {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                        player.sendMessage(MenuUtils.colorize("&8[&bCustom&8] &aYour name color has been reset!"));
                        player.closeInventory();
                    });
                }
            });
    }
    
    private void startAnimations() {
        // Animate the rainbow option if present
        if (canUseRainbow()) {
            animationTask = new BukkitRunnable() {
                int frame = 0;
                String[] rainbowFrames = {
                    "&c&lR&6&la&e&li&a&ln&b&lb&9&lo&d&lw",
                    "&6&lR&e&la&a&li&b&ln&9&lb&d&lo&c&lw",
                    "&e&lR&a&la&b&li&9&ln&d&lb&c&lo&6&lw",
                    "&a&lR&b&la&9&li&d&ln&c&lb&6&lo&e&lw",
                    "&b&lR&9&la&d&li&c&ln&6&lb&e&lo&a&lw",
                    "&9&lR&d&la&c&li&6&ln&e&lb&a&lo&b&lw",
                    "&d&lR&c&la&6&li&e&ln&a&lb&b&lo&9&lw"
                };
                
                @Override
                public void run() {
                    ItemStack rainbow = inventory.getItem(RAINBOW_SLOT);
                    if (rainbow != null && rainbow.getType() == Material.NETHER_STAR) {
                        ItemMeta meta = rainbow.getItemMeta();
                        meta.setDisplayName(
                            MenuUtils.colorize(rainbowFrames[frame % rainbowFrames.length])
                        );
                        rainbow.setItemMeta(meta);
                        frame++;
                    }
                }
            };
            animationTask.runTaskTimer(plugin, 0L, 5L); // Update every 5 ticks
            
            // Register with menu manager
            plugin.getMenuManager().registerAnimation(player, animationTask);
        }
    }
    
    @Override
    public void onClose() {
        // Cancel animations
        if (animationTask != null) {
            animationTask.cancel();
        }
    }
    
    // Helper methods
    private boolean canUseGradients() {
    return plugin.getConfigManager().canUseGradients(rank);
}

    private boolean canUseRainbow() {
        return plugin.getConfigManager().canUseRainbow(rank);
    }
    
}