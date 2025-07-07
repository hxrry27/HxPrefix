package com.yourserver.playercustomisation.gui;

import com.yourserver.playercustomisation.PlayerCustomisation;
import com.yourserver.playercustomisation.config.ConfigManager;
import com.yourserver.playercustomisation.models.PlayerData;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Menu for selecting name colors using the grouped color system
 */
public class ColorSelectionMenu extends AbstractMenu {
    private int resetSlot = 49;
    private int rainbowSlot = 41;
    
    private BukkitRunnable animationTask;
    private Map<Integer, ConfigManager.ColorOption> animatedSlots = new HashMap<>();
    
    public ColorSelectionMenu(PlayerCustomisation plugin, Player player, String rank) {
        super(plugin, player, rank, getMenuTitle(plugin, rank), getMenuSize(plugin));
        loadSlotPositions();
    }
    
    private static String getMenuTitle(PlayerCustomisation plugin, String rank) {
        return plugin.getConfigManager().getColorMenuTitle(rank);
    }
    
    private static int getMenuSize(PlayerCustomisation plugin) {
        return plugin.getConfigManager().getColorMenuSize();
    }
    
    private void loadSlotPositions() {
        ConfigurationSection menuConfig = plugin.getConfigManager().getColorMenuConfig()
            .getConfigurationSection("menu");
        
        if (menuConfig != null) {
            ConfigurationSection specialSlots = menuConfig.getConfigurationSection("special-slots");
            if (specialSlots != null) {
                resetSlot = specialSlots.getInt("reset-button", 49);
            }
        }
        
        // Load rainbow slot from special section
        ConfigurationSection special = plugin.getConfigManager().getColorMenuConfig()
            .getConfigurationSection("special.rainbow");
        if (special != null) {
            rainbowSlot = special.getInt("slot", 41);
        }
    }
    
    @Override
    protected void build() {
        // Get available colors for this player
        List<ConfigManager.ColorOption> availableColors = plugin.getConfigManager().getAvailableColors(player);
        
        plugin.getLogger().info("Building color menu for " + player.getName() + " - found " + availableColors.size() + " available colors");
        
        // Group colors by type if configured
        String groupBy = plugin.getConfigManager().getColorMenuConfig()
            .getString("menu.group-by", "type");
        
        if (groupBy.equals("type")) {
            // Separate by type
            List<ConfigManager.ColorOption> solidColors = new ArrayList<>();
            List<ConfigManager.ColorOption> gradientColors = new ArrayList<>();
            List<ConfigManager.ColorOption> specialColors = new ArrayList<>();
            
            for (ConfigManager.ColorOption color : availableColors) {
                switch (color.type) {
                    case "solid":
                        solidColors.add(color);
                        break;
                    case "gradient":
                        gradientColors.add(color);
                        break;
                    case "special":
                        specialColors.add(color);
                        break;
                }
            }
            
            // Add section headers and colors
            if (!solidColors.isEmpty()) {
                addSectionTitle("solid-title", 4);
                displayColors(solidColors, 10); // Start at slot 10
            }
            
            if (!gradientColors.isEmpty()) {
                addSectionTitle("gradient-title", 31);
                displayColors(gradientColors, 37); // Start at slot 37
            }
            
            if (!specialColors.isEmpty()) {
                displaySpecialColors(specialColors);
            }
        } else {
            // Display all colors mixed together
            displayColors(availableColors, 10);
        }
        
        // Add reset button
        addResetButton();
        
        // Fill empty slots
        fillEmptyWithConfiguredFiller();
        
        // Start animations for special colors
        startAnimations();
    }
    
    private void displayColors(List<ConfigManager.ColorOption> colors, int startSlot) {
        int slot = startSlot;
        int maxSlot = 44; // Don't go into bottom row
        
        for (ConfigManager.ColorOption color : colors) {
            if (slot > maxSlot) break;
            
            // Skip slots that would interfere with section headers
            if (slot == 4 || slot == 31) {
                slot++;
            }
            
            displayColorOption(slot, color);
            slot++;
            
            // Skip to next row if at end of current row
            if ((slot + 1) % 9 == 0) {
                slot++; // Skip the last column
            }
        }
    }
    
    private void displaySpecialColors(List<ConfigManager.ColorOption> specialColors) {
        int slot = 46; // Start special colors in bottom area
        
        for (ConfigManager.ColorOption color : specialColors) {
            // Rainbow gets special treatment
            if (color.name.equalsIgnoreCase("Rainbow")) {
                displayColorOption(rainbowSlot, color);
            } else {
                displayColorOption(slot, color);
                slot++;
            }
        }
    }
    
    private void displayColorOption(int slot, ConfigManager.ColorOption color) {
        // Get the MiniMessage color value
        String colorValue = ConfigManager.getColorValue(color);
        
        // Create preview
        String preview = MenuUtils.createPreview(colorValue, player.getName());
        
        List<String> lore = new ArrayList<>();
        if (color.description != null) {
            lore.add(color.description);
        }
        lore.add("&7Preview: " + preview);
        lore.add("");
        lore.add("&eClick to apply!");
        
        // Format display name based on type
        String displayName;
        if (color.type.equals("solid")) {
            // Apply the color to the name itself
            displayName = MenuUtils.colorize(MenuUtils.toBirdflop((String) color.value) + "&l" + 
                color.name.replace("_", " "));
        } else {
            displayName = "&d&l" + color.name.replace("_", " ");
        }
        
        // Create item
        ItemStack item = color.glow ? 
            createGlowingItem(color.material, displayName, lore) : 
            createItem(color.material, displayName, lore);
        
        // Set click handler
        setItem(slot, item, (Runnable) () -> {
            applyColor(color.name, colorValue);
        });
        
        // Store animation info if needed
        if (color.animation != null && color.animation.containsKey("enabled") && 
            (Boolean) color.animation.get("enabled")) {
            animatedSlots.put(slot, color);
        }
    }
    
    private void addSectionTitle(String sectionKey, int slot) {
        ConfigurationSection nameColorsConfig = plugin.getConfigManager().getColorMenuConfig();
        ConfigurationSection section = nameColorsConfig.getConfigurationSection("menu.sections." + sectionKey);
        
        if (section != null) {
            String materialName = section.getString("material");
            if (materialName == null) {
                plugin.getLogger().warning("No material specified for section " + sectionKey);
                return;
            }
            
            try {
                Material material = Material.valueOf(materialName.toUpperCase());
                String name = section.getString("name");
                List<String> lore = section.getStringList("lore");
                
                // Replace placeholders in lore
                List<String> processedLore = new ArrayList<>();
                for (String line : lore) {
                    processedLore.add(line.replace("{rank}", rank));
                }
                
                setItem(slot, createItem(material, name, processedLore));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material " + materialName + " for section " + sectionKey);
            }
        }
    }
    
    private void addResetButton() {
        ConfigurationSection nameColorsConfig = plugin.getConfigManager().getColorMenuConfig();
        ConfigurationSection menuConfig = nameColorsConfig.getConfigurationSection("menu");
        Material material = Material.BARRIER;
        String name = "&c&lReset Color";
        List<String> lore = Arrays.asList(
            "&7Remove your current name color",
            "",
            "&cClick to reset!"
        );
        
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
                
                name = resetSection.getString("name", name);
                lore = resetSection.getStringList("lore");
            }
        }
        
        setItem(resetSlot, createItem(material, name, lore), (Runnable) () -> {
            resetColor();
        });
    }
    
    private void fillEmptyWithConfiguredFiller() {
        ConfigurationSection nameColorsConfig = plugin.getConfigManager().getColorMenuConfig();
        ConfigurationSection fillerConfig = nameColorsConfig.getConfigurationSection("menu.filler");
        
        if (fillerConfig == null) {
            plugin.getLogger().warning("No filler configuration found for color menu");
            return;
        }
        
        String materialName = fillerConfig.getString("material");
        if (materialName == null) {
            plugin.getLogger().warning("No filler material specified for color menu");
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
    
    private void startAnimations() {
        if (animatedSlots.isEmpty()) return;
        
        animationTask = new BukkitRunnable() {
            private Map<Integer, Integer> frameCounters = new HashMap<>();
            
            @Override
            public void run() {
                for (Map.Entry<Integer, ConfigManager.ColorOption> entry : animatedSlots.entrySet()) {
                    int slot = entry.getKey();
                    ConfigManager.ColorOption color = entry.getValue();
                    
                    if (color.animation == null) continue;
                    
                    @SuppressWarnings("unchecked")
                    List<String> frames = (List<String>) color.animation.get("frames");
                    if (frames == null || frames.isEmpty()) continue;
                    
                    int frameIndex = frameCounters.getOrDefault(slot, 0);
                    
                    ItemStack item = inventory.getItem(slot);
                    if (item != null && item.getType() != Material.AIR) {
                        ItemMeta meta = item.getItemMeta();
                        meta.setDisplayName(MenuUtils.colorize(frames.get(frameIndex % frames.size())));
                        item.setItemMeta(meta);
                    }
                    
                    frameCounters.put(slot, frameIndex + 1);
                }
            }
        };
        
        // Get animation speed
        int speed = 5;
        for (ConfigManager.ColorOption color : animatedSlots.values()) {
            if (color.animation != null && color.animation.containsKey("speed")) {
                speed = (Integer) color.animation.get("speed");
                break;
            }
        }
        
        animationTask.runTaskTimer(plugin, 0L, speed);
        plugin.getMenuManager().registerAnimation(player, animationTask);
    }
    
    private void applyColor(String colorName, String colorValue) {
        plugin.getPlayerDataManager().getPlayerData(player.getUniqueId())
            .thenAccept(data -> {
                if (data == null) {
                    data = new PlayerData(player.getUniqueId(), player.getName());
                }
                
                data.setNameColor(colorValue);
                
                plugin.getPlayerDataManager().savePlayerData(data).thenRun(() -> {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    
                    String message = plugin.getConfigManager().getMessage("color.changed");
                    player.sendMessage(message);
                    
                    // Update nametag if enabled
                    if (plugin.getNametagManager() != null) {
                        plugin.getNametagManager().updateNametag(player);
                    }
                    
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
                        player.sendMessage(plugin.getConfigManager().getMessage("color.reset"));
                        
                        // Update nametag if enabled
                        if (plugin.getNametagManager() != null) {
                            plugin.getNametagManager().updateNametag(player);
                        }
                        
                        player.closeInventory();
                    });
                }
            });
    }
    
    @Override
    public void onClose() {
        // Cancel animations
        if (animationTask != null) {
            animationTask.cancel();
        }
    }
}