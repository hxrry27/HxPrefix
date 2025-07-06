package com.yourserver.playercustomisation.gui;

import com.yourserver.playercustomisation.PlayerCustomisation;
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
 * Menu for selecting name colors
 * All layout positions and materials are now configurable
 */
public class ColorSelectionMenu extends AbstractMenu {
    // Configuration-loaded slot positions
    private int solidTitleSlot = 4;
    private int gradientTitleSlot = 31;
    private int resetSlot = 49;
    private int rainbowSlot = 41;
    private List<Integer> colorSlots = new ArrayList<>();
    private List<Integer> gradientSlots = new ArrayList<>();
    
    // Animation task
    private BukkitRunnable animationTask;
    
    public ColorSelectionMenu(PlayerCustomisation plugin, Player player, String rank) {
        super(plugin, player, rank, getMenuTitle(plugin, rank), getMenuSize(plugin));
        
        // Load all slot positions from config
        loadSlotPositions();
    }
    
    private static String getMenuTitle(PlayerCustomisation plugin, String rank) {
        ConfigurationSection menuConfig = plugin.getConfigManager().getColorMenuConfig()
            .getConfigurationSection("menu");
        String titleFormat = menuConfig != null ? 
            menuConfig.getString("title", "&6&lName Color Selection &7({rank})") : 
            "&6&lName Color Selection &7({rank})";
        return titleFormat.replace("{rank}", rank);
    }
    
    private static int getMenuSize(PlayerCustomisation plugin) {
        ConfigurationSection menuConfig = plugin.getConfigManager().getColorMenuConfig()
            .getConfigurationSection("menu");
        return menuConfig != null ? menuConfig.getInt("size", 54) : 54;
    }
    
    private void loadSlotPositions() {
        ConfigurationSection colorConfig = plugin.getConfigManager().getColorMenuConfig();
        ConfigurationSection menuConfig = colorConfig.getConfigurationSection("menu");
        
        if (menuConfig != null) {
            // Load special slots
            ConfigurationSection specialSlots = menuConfig.getConfigurationSection("special-slots");
            if (specialSlots != null) {
                resetSlot = specialSlots.getInt("reset-button", 49);
            }
            
            // Load section header slots
            ConfigurationSection sections = menuConfig.getConfigurationSection("sections");
            if (sections != null) {
                ConfigurationSection solidTitle = sections.getConfigurationSection("solid-title");
                if (solidTitle != null) {
                    solidTitleSlot = solidTitle.getInt("slot", 4);
                }
            }
            
            // Load color slots
            colorSlots = menuConfig.getIntegerList("color-slots");
            if (colorSlots.isEmpty()) {
                // Default slots if not configured
                colorSlots = Arrays.asList(10, 11, 12, 13, 14, 15, 16);
            }
        }
        
        // Load gradient configuration
        ConfigurationSection gradientConfig = plugin.getConfigManager().getGradientMenuConfig();
        if (gradientConfig != null) {
            ConfigurationSection gradientMenu = gradientConfig.getConfigurationSection("menu");
            if (gradientMenu != null) {
                // Load gradient section header slot
                ConfigurationSection sections = gradientMenu.getConfigurationSection("sections");
                if (sections != null) {
                    ConfigurationSection gradientTitle = sections.getConfigurationSection("gradient-title");
                    if (gradientTitle != null) {
                        gradientTitleSlot = gradientTitle.getInt("slot", 31);
                    }
                }
                
                // Load gradient slots
                gradientSlots = gradientMenu.getIntegerList("gradient-slots");
                if (gradientSlots.isEmpty()) {
                    // Default slots if not configured
                    gradientSlots = Arrays.asList(37, 38, 39, 40, 41, 46, 47, 48);
                }
            }
            
            // Load rainbow slot
            ConfigurationSection special = gradientConfig.getConfigurationSection("special.rainbow");
            if (special != null) {
                rainbowSlot = special.getInt("slot", 41);
            }
        }
    }
    
    @Override
    protected void build() {
        // Add solid colors section title from config
        addSectionTitle("solid-title", solidTitleSlot);
        
        // Add solid colors
        addSolidColors();
        
        // Check if they can use gradients
        if (canUseGradients()) {
            addSectionTitle("gradient-title", gradientTitleSlot);
            addGradientColors();
        }
        
        // Check if they can use rainbow
        if (canUseRainbow()) {
            addRainbowOption();
        }
        
        // Add reset button
        addResetButton();
        
        // Fill empty slots
        fillEmptyWithConfiguredFiller();
        
        // Start animations
        startAnimations();
    }
    
    private void addSectionTitle(String sectionKey, int slot) {
        ConfigurationSection colorConfig = plugin.getConfigManager().getColorMenuConfig();
        ConfigurationSection section = colorConfig.getConfigurationSection("menu.sections." + sectionKey);
        
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
    
    private void addSolidColors() {
        Map<String, Map<String, Object>> solidColors = loadColorConfig();
        
        int slotIndex = 0;
        for (Map.Entry<String, Map<String, Object>> entry : solidColors.entrySet()) {
            String colorName = entry.getKey();
            Map<String, Object> colorData = entry.getValue();
            
            // Get configured slot or use next available
            int slot;
            if (colorData.containsKey("slot")) {
                slot = (int) colorData.get("slot");
            } else {
                if (slotIndex >= colorSlots.size()) break;
                slot = colorSlots.get(slotIndex++);
            }
            
            // Get material
            String materialName = (String) colorData.getOrDefault("material", "PAPER");
            Material material = Material.valueOf(materialName.toUpperCase());
            
            // Get hex value
            String hexValue = (String) colorData.get("hex");
            
            // Create color value for storage
            String colorValue = MenuUtils.hexToMiniMessage(hexValue);
            String preview = MenuUtils.createPreview(colorValue, player.getName());
            
            List<String> lore = Arrays.asList(
                "&7Preview: " + preview,
                "",
                "&eClick to apply!"
            );
            
            // Format display name
            String displayName = MenuUtils.colorize(MenuUtils.toBirdflop(hexValue) + "&l" + colorName.replace("_", " "));
            
            setItem(slot, createItem(material, displayName, lore), (Runnable) () -> {
                applyColor(colorName, colorValue);
            });
        }
    }
    
    private void addGradientColors() {
        Map<String, Map<String, Object>> gradients = loadGradientConfig();
        
        int slotIndex = 0;
        for (Map.Entry<String, Map<String, Object>> entry : gradients.entrySet()) {
            String gradientName = entry.getKey();
            Map<String, Object> gradientData = entry.getValue();
            
            // Skip special entries
            if (gradientName.equals("special")) continue;
            
            // Get configured slot or use next available
            int slot;
            if (gradientData.containsKey("slot")) {
                slot = (int) gradientData.get("slot");
            } else {
                if (slotIndex >= gradientSlots.size()) break;
                slot = gradientSlots.get(slotIndex++);
            }
            
            // Get material
            String materialName = (String) gradientData.getOrDefault("material", "FIREWORK_STAR");
            Material material = Material.valueOf(materialName.toUpperCase());
            
            // Get colors
            @SuppressWarnings("unchecked")
            List<String> colors = (List<String>) gradientData.get("colors");
            String description = (String) gradientData.get("description");
            
            // Create gradient value
            String gradientValue = MenuUtils.gradientToMiniMessage(colors.toArray(new String[0]));
            String preview = MenuUtils.createPreview(gradientValue, player.getName());
            
            List<String> lore = Arrays.asList(
                "&7Gradient: " + description,
                "&7Preview: " + preview,
                "",
                "&eClick to apply!"
            );
            
            String displayName = "&d&l" + gradientName.replace("_", " ");
            
            setItem(slot, createGlowingItem(material, displayName, lore), (Runnable) () -> {
                applyColor(gradientName, gradientValue);
            });
        }
    }
    
    private void addRainbowOption() {
        ConfigurationSection rainbowConfig = plugin.getConfigManager().getGradientMenuConfig()
            .getConfigurationSection("special.rainbow");
            
        if (rainbowConfig != null) {
            String materialName = rainbowConfig.getString("material");
            if (materialName == null) {
                plugin.getLogger().warning("No material specified for rainbow option");
                return;
            }
            
            try {
                Material material = Material.valueOf(materialName.toUpperCase());
                String name = rainbowConfig.getString("name", "&c&lR&6&la&e&li&a&ln&b&lb&9&lo&d&lw");
                String description = rainbowConfig.getString("description", "&7Special rainbow gradient!");
                
                List<String> lore = Arrays.asList(
                    description,
                    "&7Preview: " + name,
                    "",
                    "&eClick to apply!"
                );
                
                setItem(rainbowSlot, createGlowingItem(material, name, lore), (Runnable) () -> {
                    applyColor("Rainbow", MenuUtils.rainbowTag());
                });
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material " + materialName + " for rainbow option");
            }
        }
    }
    
    private void addResetButton() {
        // Get reset button configuration
        ConfigurationSection menuConfig = plugin.getConfigManager().getColorMenuConfig()
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
        
        setItem(resetSlot, createItem(material, "&c&lReset Color", Arrays.asList(
            "&7Remove your current name color",
            "",
            "&cClick to reset!"
        )), (Runnable) () -> {
            resetColor();
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
                    String message = plugin.getConfigManager().getMessage("color.changed");
                    player.sendMessage(message);
                    
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
                        player.sendMessage(plugin.getConfigManager().getMessage("color.reset"));
                        player.closeInventory();
                    });
                }
            });
    }
    
    private void fillEmptyWithConfiguredFiller() {
        // Get filler configuration
        ConfigurationSection colorConfig = plugin.getConfigManager().getColorMenuConfig();
        ConfigurationSection fillerConfig = colorConfig.getConfigurationSection("menu.filler");
        
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
        // Only animate rainbow if player can use it
        if (!canUseRainbow()) return;
        
        ConfigurationSection animConfig = plugin.getConfigManager().getGradientMenuConfig()
            .getConfigurationSection("menu.animation.rainbow");
            
        if (animConfig != null && animConfig.getBoolean("enabled", true)) {
            List<String> frames = animConfig.getStringList("frames");
            if (frames.isEmpty()) {
                // Use default frames
                frames = Arrays.asList(
                    "&c&lR&6&la&e&li&a&ln&b&lb&9&lo&d&lw",
                    "&6&lR&e&la&a&li&b&ln&9&lb&d&lo&c&lw",
                    "&e&lR&a&la&b&li&9&ln&d&lb&c&lo&6&lw",
                    "&a&lR&b&la&9&li&d&ln&c&lb&6&lo&e&lw",
                    "&b&lR&9&la&d&li&c&ln&6&lb&e&lo&a&lw",
                    "&9&lR&d&la&c&li&6&ln&e&lb&a&lo&b&lw",
                    "&d&lR&c&la&6&li&e&ln&a&lb&b&lo&9&lw"
                );
            }
            
            final List<String> animationFrames = frames;
            int speed = animConfig.getInt("speed", 5);
            
            animationTask = new BukkitRunnable() {
                int frame = 0;
                
                @Override
                public void run() {
                    ItemStack rainbow = inventory.getItem(rainbowSlot);
                    if (rainbow != null && rainbow.getType() != Material.AIR) {
                        ItemMeta meta = rainbow.getItemMeta();
                        meta.setDisplayName(
                            MenuUtils.colorize(animationFrames.get(frame % animationFrames.size()))
                        );
                        rainbow.setItemMeta(meta);
                        frame++;
                    }
                }
            };
            animationTask.runTaskTimer(plugin, 0L, speed);
            
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
    
    private Map<String, Map<String, Object>> loadColorConfig() {
        Map<String, Map<String, Object>> colors = new LinkedHashMap<>();
        ConfigurationSection colorsConfig = plugin.getConfigManager().getColorMenuConfig();
        ConfigurationSection colorsSection = colorsConfig.getConfigurationSection("colors");
        
        // Get default material from config
        String defaultMaterial = colorsConfig.getString("defaults.material", "PAPER");
            
        if (colorsSection != null) {
            for (String key : colorsSection.getKeys(false)) {
                ConfigurationSection colorSection = colorsSection.getConfigurationSection(key);
                if (colorSection != null) {
                    Map<String, Object> colorData = new HashMap<>();
                    colorData.put("hex", colorSection.getString("hex"));
                    colorData.put("material", colorSection.getString("material", defaultMaterial));
                    if (colorSection.contains("slot")) {
                        colorData.put("slot", colorSection.getInt("slot"));
                    }
                    colors.put(key, colorData);
                }
            }
        }
        
        return colors;
    }
    
    private Map<String, Map<String, Object>> loadGradientConfig() {
        Map<String, Map<String, Object>> gradients = new LinkedHashMap<>();
        ConfigurationSection gradientsConfig = plugin.getConfigManager().getGradientMenuConfig();
        ConfigurationSection gradientsSection = gradientsConfig.getConfigurationSection("gradients");
        
        // Get default material from config
        String defaultMaterial = gradientsConfig.getString("defaults.material", "FIREWORK_STAR");
            
        if (gradientsSection != null) {
            for (String key : gradientsSection.getKeys(false)) {
                ConfigurationSection gradientSection = gradientsSection.getConfigurationSection(key);
                if (gradientSection != null) {
                    Map<String, Object> gradientData = new HashMap<>();
                    gradientData.put("colors", gradientSection.getStringList("colors"));
                    gradientData.put("material", gradientSection.getString("material", defaultMaterial));
                    gradientData.put("description", gradientSection.getString("description", ""));
                    if (gradientSection.contains("slot")) {
                        gradientData.put("slot", gradientSection.getInt("slot"));
                    }
                    gradients.put(key, gradientData);
                }
            }
        }
        
        return gradients;
    }
}