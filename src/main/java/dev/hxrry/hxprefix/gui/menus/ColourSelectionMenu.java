package dev.hxrry.hxprefix.gui.menus;

import dev.hxrry.hxgui.core.MenuItem;
import dev.hxrry.hxgui.core.SharedMenu;

import dev.hxrry.hxprefix.HxPrefix;
import dev.hxrry.hxprefix.api.models.StyleOption;

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
 * menu for selecting name colours
 */
public class ColourSelectionMenu extends SharedMenu {
    private final HxPrefix plugin;
    private final Player player;
    private final String rank;
    private final MiniMessage mm = MiniMessage.miniMessage();
    
    // animation task
    private BukkitRunnable animationTask;
    
    public ColourSelectionMenu(@NotNull HxPrefix plugin, @NotNull Player player) {
        super(Component.text("Colour Selection", NamedTextColor.GOLD, TextDecoration.BOLD), 
              6);
        
        this.plugin = plugin;
        this.player = player;
        this.rank = plugin.getLuckPermsHook() != null ? 
            plugin.getLuckPermsHook().getPrimaryGroup(player) : "default";
        
        build();
    }
    
    // build the menu contents
    private void build() {
        // get available colours for rank
        List<StyleOption> colours = plugin.getConfigManager().getStyleConfig()
            .getAvailableColours(rank);
        
        // separate by type
        List<StyleOption> solidColours = new ArrayList<>();
        List<StyleOption> gradientColours = new ArrayList<>();
        List<StyleOption> specialColours = new ArrayList<>();
        
        for (StyleOption colour : colours) {
            switch (colour.getType()) {
                case COLOUR_SOLID -> solidColours.add(colour);
                case COLOUR_GRADIENT -> gradientColours.add(colour);
                case COLOUR_SPECIAL -> specialColours.add(colour);
                default -> throw new IllegalArgumentException("Unexpected value: " + colour.getType());
            }
        }
        
        // add section headers
        if (!solidColours.isEmpty()) {
            addSectionHeader(4, "Solid Colours", Material.NAME_TAG);
            displayColours(solidColours, 10, 16);
        }
        
        if (!gradientColours.isEmpty()) {
            addSectionHeader(31, "Gradient Colours", Material.FIREWORK_STAR);
            displayColours(gradientColours, 37, 43);
        }
        
        if (!specialColours.isEmpty()) {
            // special colours get special slots
            for (StyleOption special : specialColours) {
                if (special.getId().contains("rainbow")) {
                    addColourOption(22, special); // center slot
                } else {
                    addColourOption(40, special);
                }
            }
        }
        
        // add reset button
        addResetButton(49);
        
        // fill empty slots
        fillEmpty();
        
        // start animations
        startAnimations(specialColours);
    }

    // display colours in a range
    private void displayColours(@NotNull List<StyleOption> colours, int startSlot, int endSlot) {
        int slot = startSlot;
        
        for (StyleOption colour : colours) {
            if (slot > endSlot) break;
            
            addColourOption(slot, colour);
            slot++;
            
            // skip to next row if needed
            if ((slot + 1) % 9 == 0) {
                slot += 2; // skip first and last column of next row
            }
        }
    }

    // add colour option to menu
    private void addColourOption(int slot, @NotNull StyleOption colour) {
        // create item
        ItemStack item = new ItemStack(colour.getMaterial());
        ItemMeta meta = item.getItemMeta();
        
        // set display name
        Component displayName = mm.deserialize(colour.getDisplayName());
        if (colour.getType() == StyleOption.Type.COLOUR_SOLID) {
            // apply the colour to the name
            displayName = mm.deserialize(colour.getValue() + colour.getDisplayName());
        }
        meta.displayName(displayName.decoration(TextDecoration.ITALIC, false));
        
        // set lore
        List<Component> lore = new ArrayList<>();
        
        // add description if present
        if (colour.getDescription() != null) {
            for (String line : colour.getDescription()) {
                lore.add(mm.deserialize("<gray>" + line)
                    .decoration(TextDecoration.ITALIC, false));
            }
        }
        
        // add preview
        String preview = plugin.getConfigManager().getStyleConfig()
            .formatWithColour(colour.getValue(), player.getName());
        lore.add(Component.empty());
        lore.add(mm.deserialize("<gray>preview: " + preview)
            .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(mm.deserialize("<yellow>click to apply!")
            .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(lore);
        
        // add glow if needed
        if (colour.hasGlow()) {
            meta.setEnchantmentGlintOverride(true);
        }
        
        item.setItemMeta(meta);
        
        // create menu item with click handler
        setItem(slot, new MenuItem(item, event -> {
            event.setCancelled(true); // Prevent item pickup
            Player p = (Player) event.getWhoClicked();
            selectColour(p, colour);
        }));
    }

    // add section header
    private void addSectionHeader(int slot, @NotNull String title, @NotNull Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(mm.deserialize("<gold><bold>" + title)
            .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(List.of(
            mm.deserialize("<gray>choose from the options below")
                .decoration(TextDecoration.ITALIC, false)
        ));
        
        item.setItemMeta(meta);
        
        setItem(slot, new MenuItem(item));
    }

    // add reset button
    private void addResetButton(int slot) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(mm.deserialize("<red><bold>reset colour")
            .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(List.of(
            mm.deserialize("<gray>remove your current colour")
                .decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            mm.deserialize("<red>click to reset!")
                .decoration(TextDecoration.ITALIC, false)
        ));
        
        item.setItemMeta(meta);
        
        setItem(slot, new MenuItem(item, event -> {
            event.setCancelled(true); // prevent item pickup
            Player p = (Player) event.getWhoClicked();
            resetColour(p);
        }));
    }

    // fill empties w glass
    private void fillEmpty() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.displayName(Component.empty());
        filler.setItemMeta(meta);
        
        for (int i = 0; i < getSize(); i++) {
            if (getItem(i) == null) {
                setItem(i, new MenuItem(filler));
            }
        }
    }

    // handle colour selection
    private void selectColour(@NotNull Player player, @NotNull StyleOption colour) {
        // apply the colour
        if (plugin.getAPI().setNameColour(player, colour.getValue())) {
            // success
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            
            Component message = mm.deserialize(
                plugin.getConfigManager().getMessagesConfig()
                    .getMessage("colour.changed")
            );
            player.sendMessage(message);
            
            player.closeInventory();
        } else {
            // failed
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    // handle colour reset
    private void resetColour(@NotNull Player player) {
        if (plugin.getAPI().setNameColour(player, null)) {
            // success
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            
            Component message = mm.deserialize(
                plugin.getConfigManager().getMessagesConfig()
                    .getMessage("colour.removed")
            );
            player.sendMessage(message);
            
            player.closeInventory();
        } else {
            // failed
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
    
    // anims for special colours
    private void startAnimations(@NotNull List<StyleOption> specialColours) {
        if (specialColours.isEmpty()) return;
        
        // find animated colours
        List<AnimatedColour> animated = new ArrayList<>();
        
        for (StyleOption colour : specialColours) {
            if (colour.hasAnimation()) {
                int slot = colour.getId().contains("rainbow") ? 22 : 40;
                animated.add(new AnimatedColour(slot, colour));
            }
        }
        
        if (animated.isEmpty()) return;
        
        // start animation task
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
        
        animationTask.runTaskTimer(plugin, 0L, 5L); // update every 5 ticks
    }
    
    @Override
    public void onClose(@NotNull Player player) {
        // cancel animations
        if (animationTask != null) {
            animationTask.cancel();
            animationTask = null;
        }
    }
    
    // helper class for anim colours
    private class AnimatedColour {
        private final int slot;
        @SuppressWarnings("unused")
        private final StyleOption colour;
        private final List<String> frames;
        private final int speed;
        
        AnimatedColour(int slot, StyleOption colour) {
            this.slot = slot;
            this.colour = colour;
            this.frames = colour.getAnimationFrames();
            this.speed = colour.getAnimationSpeed();
        }
        
        void nextFrame(int tick) {
            if (frames == null || frames.isEmpty()) return;
            if (tick % speed != 0) return;
            
            int frameIndex = (tick / speed) % frames.size();
            String frameText = frames.get(frameIndex);
            
            // update item display name
            MenuItem item = getItem(slot);
            if (item != null) {
                ItemStack stack = item.getItem();
                ItemMeta meta = stack.getItemMeta();
                meta.displayName(mm.deserialize(frameText)
                    .decoration(TextDecoration.ITALIC, false));
                stack.setItemMeta(meta);
            }
        }
    }
}