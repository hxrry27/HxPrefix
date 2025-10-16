package dev.hxrry.hxprefix.gui.menus;

import dev.hxrry.hxgui.components.Pagination;
import dev.hxrry.hxgui.core.MenuItem;

import dev.hxrry.hxprefix.HxPrefix;
import dev.hxrry.hxprefix.api.models.StyleOption;
import dev.hxrry.hxprefix.gui.GUIConstants;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Paginated menu for selecting name colours
 */
public class ColourSelectionMenu {
    private final HxPrefix plugin;
    private final Player player;
    private final String rank;
    private final MiniMessage mm = MiniMessage.miniMessage();
    
    private Pagination pagination;
    private BukkitRunnable animationTask;
    
    public ColourSelectionMenu(@NotNull HxPrefix plugin, @NotNull Player player) {
        this.plugin = plugin;
        this.player = player;
        this.rank = plugin.getLuckPermsHook() != null ? 
            plugin.getLuckPermsHook().getPrimaryGroup(player) : "default";
    }
    
    /**
     * Open the menu
     */
    public void open() {
        // Get available colours for rank
        List<StyleOption> colours = plugin.getConfigManager().getStyleConfig()
            .getAvailableColours(rank);
        
        // Calculate rows needed
        int rows = GUIConstants.calculateRows(colours.size(), 3, 6);
        
        // Create pagination
        pagination = new Pagination(
            Component.text("Colour Selection", NamedTextColor.GOLD, TextDecoration.BOLD),
            rows
        );
        
        // Configure pagination areas
        pagination.contentArea(0, GUIConstants.getContentAreaEnd(rows));
        pagination.navigationSlots(
            GUIConstants.getPreviousPageSlot(rows),
            GUIConstants.getNextPageSlot(rows),
            GUIConstants.getPageIndicatorSlot(rows)
        );
        
        // Separate colours by type
        List<StyleOption> solidColours = new ArrayList<>();
        List<StyleOption> gradientColours = new ArrayList<>();
        List<StyleOption> specialColours = new ArrayList<>();
        
        for (StyleOption colour : colours) {
            switch (colour.getType()) {
                case COLOUR_SOLID -> solidColours.add(colour);
                case COLOUR_GRADIENT -> gradientColours.add(colour);
                case COLOUR_SPECIAL -> specialColours.add(colour);
                default -> throw new IllegalArgumentException("Unexpected colour type: " + colour.getType());
            }
        }
        
        // Add header item
        pagination.addItem(new MenuItem(createHeaderItem(colours.size())));
        
        // Add section header for solid colours
        if (!solidColours.isEmpty()) {
            pagination.addItem(new MenuItem(createSectionHeader("Solid Colours", Material.NAME_TAG)));
            for (StyleOption colour : solidColours) {
                pagination.addItem(createColourItem(colour));
            }
        }
        
        // Add section header for gradient colours
        if (!gradientColours.isEmpty()) {
            pagination.addItem(new MenuItem(createSectionHeader("Gradient Colours", Material.FIREWORK_STAR)));
            for (StyleOption colour : gradientColours) {
                pagination.addItem(createColourItem(colour));
            }
        }
        
        // Add section header for special colours
        if (!specialColours.isEmpty()) {
            pagination.addItem(new MenuItem(createSectionHeader("Special Colours", Material.NETHER_STAR)));
            for (StyleOption colour : specialColours) {
                pagination.addItem(createColourItem(colour));
            }
        }
        
        // Add reset button
        pagination.addItem(createResetItem());
        
        // Open for player
        pagination.open(player);
        
        // Start animations if there are special colours
        if (!specialColours.isEmpty()) {
            startAnimations(specialColours);
        }
    }
    
    /**
     * Create header info item
     */
    private ItemStack createHeaderItem(int colourCount) {
        ItemStack item = new ItemStack(Material.PAINTING);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(mm.deserialize("<gold><bold>Available Colours")
            .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(List.of(
            mm.deserialize("<gray>Rank: <white>" + rank)
                .decoration(TextDecoration.ITALIC, false),
            mm.deserialize("<gray>Available colours: <white>" + colourCount)
                .decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            mm.deserialize("<yellow>Click a colour to apply!")
                .decoration(TextDecoration.ITALIC, false)
        ));
        
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Create section header item
     */
    private ItemStack createSectionHeader(@NotNull String title, @NotNull Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(mm.deserialize("<gold><bold>" + title)
            .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(List.of(
            mm.deserialize("<gray>Choose from the options below")
                .decoration(TextDecoration.ITALIC, false)
        ));
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Create colour selection item
     */
    private MenuItem createColourItem(@NotNull StyleOption colour) {
        ItemStack item = new ItemStack(colour.getMaterial());
        ItemMeta meta = item.getItemMeta();
        
        // Set display name
        Component displayName = mm.deserialize(colour.getDisplayName());
        if (colour.getType() == StyleOption.Type.COLOUR_SOLID) {
            // Apply the colour to the name
            displayName = mm.deserialize(colour.getValue() + colour.getDisplayName());
        }
        meta.displayName(displayName.decoration(TextDecoration.ITALIC, false));
        
        // Set lore
        List<Component> lore = new ArrayList<>();
        
        // Add description if present
        if (colour.getDescription() != null) {
            for (String line : colour.getDescription()) {
                lore.add(mm.deserialize("<gray>" + line)
                    .decoration(TextDecoration.ITALIC, false));
            }
        }
        
        // Add preview
        String preview = plugin.getConfigManager().getStyleConfig()
            .formatWithColour(colour.getValue(), player.getName());
        lore.add(Component.empty());
        lore.add(mm.deserialize("<gray>Preview: " + preview)
            .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(mm.deserialize("<yellow>Click to apply!")
            .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(lore);
        
        // Add glow if needed
        if (colour.hasGlow()) {
            meta.setEnchantmentGlintOverride(true);
        }
        
        item.setItemMeta(meta);
        
        // Create menu item with click handler
        return new MenuItem(item, event -> {
            event.setCancelled(true);
            Player p = (Player) event.getWhoClicked();
            selectColour(p, colour);
        });
    }
    
    /**
     * Create reset button item
     */
    private MenuItem createResetItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(mm.deserialize("<red><bold>Reset Colour")
            .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(List.of(
            mm.deserialize("<gray>Remove your current colour")
                .decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            mm.deserialize("<red>Click to reset!")
                .decoration(TextDecoration.ITALIC, false)
        ));
        
        item.setItemMeta(meta);
        
        return new MenuItem(item, event -> {
            event.setCancelled(true);
            Player p = (Player) event.getWhoClicked();
            resetColour(p);
        });
    }
    
    /**
     * Handle colour selection
     */
    private void selectColour(@NotNull Player player, @NotNull StyleOption colour) {
        if (plugin.getAPI().setNameColour(player, colour.getValue())) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            
            Component message = mm.deserialize(
                plugin.getConfigManager().getMessagesConfig()
                    .getMessage("colour.changed")
            );
            player.sendMessage(message);
            
            player.closeInventory();
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
    
    /**
     * Handle colour reset
     */
    private void resetColour(@NotNull Player player) {
        if (plugin.getAPI().setNameColour(player, null)) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            
            Component message = mm.deserialize(
                plugin.getConfigManager().getMessagesConfig()
                    .getMessage("colour.removed")
            );
            player.sendMessage(message);
            
            player.closeInventory();
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
    
    /**
     * Start animations for special colours
     */
    private void startAnimations(@NotNull List<StyleOption> specialColours) {
        // Find animated colours
        List<AnimatedColour> animated = new ArrayList<>();
        
        for (StyleOption colour : specialColours) {
            if (colour.hasAnimation()) {
                animated.add(new AnimatedColour(colour));
            }
        }
        
        if (animated.isEmpty()) return;
        
        // Start animation task
        animationTask = new BukkitRunnable() {
            int frame = 0;
            
            @Override
            public void run() {
                for (AnimatedColour anim : animated) {
                    anim.nextFrame(frame);
                }
                frame++;
            }
        };
        
        animationTask.runTaskTimer(plugin, 
            GUIConstants.ANIMATION_INITIAL_DELAY, 
            GUIConstants.ANIMATION_UPDATE_TICKS);
    }
    
    /**
     * Stop animations when menu closes
     */
    public void onClose() {
        if (animationTask != null) {
            animationTask.cancel();
            animationTask = null;
        }
    }
    
    /**
     * Helper class for animated colours
     */
    private class AnimatedColour {
        @SuppressWarnings("unused")
        private final StyleOption colour;
        private final List<String> frames;
        private final int speed;
        
        AnimatedColour(StyleOption colour) {
            this.colour = colour;
            this.frames = colour.getAnimationFrames();
            this.speed = colour.getAnimationSpeed();
        }
        
        void nextFrame(int tick) {
            if (frames == null || frames.isEmpty()) return;
            if (tick % speed != 0) return;
            
            int frameIndex = (tick / speed) % frames.size();
            String frameText = frames.get(frameIndex);
            
            // Update would happen here in SharedMenu
            // For Pagination-based menu, animations are more complex
            // Consider removing animations or implementing differently
        }
    }
}