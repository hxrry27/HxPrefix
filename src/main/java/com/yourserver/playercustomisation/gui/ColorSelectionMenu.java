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
        // Define solid colors matching your DeluxeMenus setup
        Map<String, ColorData> solidColors = new LinkedHashMap<>();
        solidColors.put("red", new ColorData("&c&lRed", "&c", Material.RED_DYE, "#FF0000"));
        solidColors.put("blue", new ColorData("&9&lBlue", "&9", Material.BLUE_DYE, "#0000FF"));
        solidColors.put("green", new ColorData("&a&lGreen", "&a", Material.GREEN_DYE, "#00FF00"));
        solidColors.put("aqua", new ColorData("&b&lAqua", "&b", Material.CYAN_DYE, "#00FFFF"));
        solidColors.put("pink", new ColorData("&d&lPink", "&d", Material.PINK_DYE, "#FF55FF"));
        solidColors.put("yellow", new ColorData("&e&lYellow", "&e", Material.YELLOW_DYE, "#FFFF00"));
        solidColors.put("white", new ColorData("&f&lWhite", "&f", Material.WHITE_DYE, "#FFFFFF"));
        
        int index = 0;
        for (Map.Entry<String, ColorData> entry : solidColors.entrySet()) {
            if (index >= SOLID_COLOR_SLOTS.length) break;
            
            ColorData color = entry.getValue();
            int slot = SOLID_COLOR_SLOTS[index++];
            
            List<String> lore = Arrays.asList(
                "&7Preview: " + color.legacyCode + player.getName(),
                "",
                "&eClick to apply!"
            );
            
            setItem(slot, createItem(color.material, color.displayName, lore), (Runnable) () -> {
                applyColor(color.displayName.replace("&l", ""), MenuUtils.hexToMiniMessage(color.hexValue));
            });
        }
    }
    
    private void addGradientColors() {
        // Define gradients matching your config
        Map<String, GradientData> gradients = new LinkedHashMap<>();
        gradients.put("fire", new GradientData("&c&lFire Gradient", 
            Arrays.asList("#FF0000", "#FFFF00"), Material.BLAZE_POWDER));
        gradients.put("ocean", new GradientData("&9&lOcean Gradient", 
            Arrays.asList("#0080FF", "#00FFFF"), Material.PRISMARINE_CRYSTALS));
        gradients.put("nature", new GradientData("&a&lNature Gradient", 
            Arrays.asList("#00FF00", "#FFFF00"), Material.EMERALD));
        gradients.put("galaxy", new GradientData("&5&lGalaxy Gradient", 
            Arrays.asList("#8B00FF", "#FF00FF"), Material.ENDER_EYE));
        
        int index = 0;
        for (Map.Entry<String, GradientData> entry : gradients.entrySet()) {
            if (index >= GRADIENT_SLOTS.length) break;
            
            GradientData gradient = entry.getValue();
            int slot = GRADIENT_SLOTS[index++];
            
            // Create gradient preview
            String gradientValue = MenuUtils.gradientToMiniMessage(
                gradient.colors.toArray(new String[0]));
            String preview = MenuUtils.createPreview(gradientValue, player.getName());
            
            List<String> lore = Arrays.asList(
                "&7Gradient: " + getGradientDescription(gradient),
                "&7Preview: " + preview,
                "",
                "&eClick to apply!"
            );
            
            // Make gradient items glow
            setItem(slot, createGlowingItem(gradient.material, gradient.displayName, lore), (Runnable) () -> {
                applyColor(gradient.displayName.replace("&l", ""), gradientValue);
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
                    
                    // Send message
                    String preview = MenuUtils.createPreview(colorValue, player.getName());
                    player.sendMessage(MenuUtils.colorize(
                        "&8[&bCustom&8] &aColor changed to: " + preview
                    ));
                    
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
        // TODO: Check with ConfigManager when implemented
        // For now, check rank name
        return rank.equalsIgnoreCase("patron") || 
               rank.equalsIgnoreCase("devoted") || 
               rank.equalsIgnoreCase("premium");
    }
    
    private boolean canUseRainbow() {
        // TODO: Check with ConfigManager when implemented
        // For now, check rank name
        return rank.equalsIgnoreCase("devoted") || 
               rank.equalsIgnoreCase("premium");
    }
    
    private String getGradientDescription(GradientData gradient) {
        // Convert hex colors to legacy format for description
        StringBuilder desc = new StringBuilder();
        for (int i = 0; i < gradient.colors.size(); i++) {
            if (i > 0) desc.append(" &7→ ");
            String hex = gradient.colors.get(i);
            // Map to approximate legacy color
            if (hex.startsWith("#FF0000")) desc.append("&cRed");
            else if (hex.startsWith("#FFFF00")) desc.append("&eYellow");
            else if (hex.startsWith("#0080FF")) desc.append("&9Blue");
            else if (hex.startsWith("#00FFFF")) desc.append("&bCyan");
            else if (hex.startsWith("#00FF00")) desc.append("&aGreen");
            else if (hex.startsWith("#8B00FF")) desc.append("&5Purple");
            else if (hex.startsWith("#FF00FF")) desc.append("&dMagenta");
            else desc.append("&f").append(hex);
        }
        return desc.toString();
    }
    
    // Data classes
    private static class ColorData {
        final String displayName;
        final String legacyCode;
        final Material material;
        final String hexValue;
        
        ColorData(String displayName, String legacyCode, Material material, String hexValue) {
            this.displayName = displayName;
            this.legacyCode = legacyCode;
            this.material = material;
            this.hexValue = hexValue;
        }
    }
    
    private static class GradientData {
        final String displayName;
        final List<String> colors;
        final Material material;
        
        GradientData(String displayName, List<String> colors, Material material) {
            this.displayName = displayName;
            this.colors = colors;
            this.material = material;
        }
    }
}