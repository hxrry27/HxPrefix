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

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Paginated menu for selecting suffixes
 */
public class SuffixSelectionMenu {
    private final HxPrefix plugin;
    private final Player player;
    private final String rank;
    private final MiniMessage mm = MiniMessage.miniMessage();
    
    private Pagination pagination;
    
    public SuffixSelectionMenu(@NotNull HxPrefix plugin, @NotNull Player player) {
        this.plugin = plugin;
        this.player = player;
        this.rank = plugin.getLuckPermsHook() != null ?
            plugin.getLuckPermsHook().getPrimaryGroup(player) : "default";
    }
    
    /**
     * Open the menu
     */
    public void open() {
        // Get available suffixes for rank
        List<StyleOption> suffixes = plugin.getConfigManager().getStyleConfig()
            .getAvailableSuffixes(rank);
        
        // Separate symbols and text suffixes
        List<StyleOption> symbols = new ArrayList<>();
        List<StyleOption> textSuffixes = new ArrayList<>();
        
        for (StyleOption suffix : suffixes) {
            String value = stripColours(suffix.getValue());
            if (value.length() <= 2 && !value.matches("[a-zA-Z]+")) {
                symbols.add(suffix);
            } else {
                textSuffixes.add(suffix);
            }
        }
        
        // Calculate rows needed
        int totalItems = symbols.size() + textSuffixes.size();
        int rows = GUIConstants.calculateRows(totalItems, 3, 6);
        
        // Create pagination
        pagination = new Pagination(
            Component.text("Suffix Selection", NamedTextColor.AQUA, TextDecoration.BOLD),
            rows
        );
        
        // Configure pagination areas
        pagination.contentArea(0, GUIConstants.getContentAreaEnd(rows));
        pagination.navigationSlots(
            GUIConstants.getPreviousPageSlot(rows),
            GUIConstants.getNextPageSlot(rows),
            GUIConstants.getPageIndicatorSlot(rows)
        );
        
        // Add header item
        pagination.addItem(new MenuItem(createHeaderItem(totalItems)));
        
        // Add symbol suffixes section
        if (!symbols.isEmpty()) {
            pagination.addItem(new MenuItem(createSectionHeader("Symbol Suffixes", Material.GOLD_NUGGET)));
            for (StyleOption suffix : symbols) {
                pagination.addItem(createSuffixItem(suffix));
            }
        }
        
        // Add text suffixes section
        if (!textSuffixes.isEmpty()) {
            pagination.addItem(new MenuItem(createSectionHeader("Text Suffixes", Material.NAME_TAG)));
            for (StyleOption suffix : textSuffixes) {
                pagination.addItem(createSuffixItem(suffix));
            }
        }
        
        // Add reset button
        pagination.addItem(createResetItem());
        
        // Open for player
        pagination.open(player);
    }
    
    /**
     * Create header info item
     */
    private ItemStack createHeaderItem(int suffixCount) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(mm.deserialize("<gold><bold>Available Suffixes")
            .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(List.of(
            mm.deserialize("<gray>Rank: <white>" + rank)
                .decoration(TextDecoration.ITALIC, false),
            mm.deserialize("<gray>Available suffixes: <white>" + suffixCount)
                .decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            mm.deserialize("<yellow>Click a suffix to apply!")
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
     * Create suffix selection item
     */
    private MenuItem createSuffixItem(@NotNull StyleOption suffix) {
        ItemStack item = new ItemStack(suffix.getMaterial());
        ItemMeta meta = item.getItemMeta();
        
        // Set display name - show the actual suffix for symbols
        String displayValue = stripColours(suffix.getValue());
        Component displayName;
        
        if (displayValue.length() <= 2 && !displayValue.matches("[a-zA-Z]+")) {
            // For symbols, show the symbol as the name
            displayName = mm.deserialize(suffix.getValue() + " <white>" + suffix.getDisplayName());
        } else {
            // For text, show the display name
            displayName = mm.deserialize("<white>" + suffix.getDisplayName());
        }
        
        meta.displayName(displayName.decoration(TextDecoration.ITALIC, false));
        
        // Set lore
        List<Component> lore = new ArrayList<>();
        
        // Add preview
        String preview = player.getName() + " " + suffix.getValue();
        lore.add(mm.deserialize("<gray>Preview: <white>" + preview)
            .decoration(TextDecoration.ITALIC, false));
        
        // Add type indicator
        if (displayValue.length() <= 2 && !displayValue.matches("[a-zA-Z]+")) {
            lore.add(mm.deserialize("<dark_gray>Symbol suffix")
                .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(mm.deserialize("<dark_gray>Text suffix")
                .decoration(TextDecoration.ITALIC, false));
        }
        
        lore.add(Component.empty());
        lore.add(mm.deserialize("<yellow>Click to apply!")
            .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(lore);
        
        // Add glow if needed
        if (suffix.hasGlow()) {
            meta.setEnchantmentGlintOverride(true);
        }
        
        item.setItemMeta(meta);
        
        // Create menu item with click handler
        return new MenuItem(item, event -> {
            event.setCancelled(true);
            Player p = (Player) event.getWhoClicked();
            selectSuffix(p, suffix);
        });
    }
    
    /**
     * Create reset button item
     */
    private MenuItem createResetItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(mm.deserialize("<red><bold>Remove Suffix")
            .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(List.of(
            mm.deserialize("<gray>Remove your current suffix")
                .decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            mm.deserialize("<red>Click to remove!")
                .decoration(TextDecoration.ITALIC, false)
        ));
        
        item.setItemMeta(meta);
        
        return new MenuItem(item, event -> {
            event.setCancelled(true);
            Player p = (Player) event.getWhoClicked();
            resetSuffix(p);
        });
    }
    
    /**
     * Handle suffix selection
     */
    private void selectSuffix(@NotNull Player player, @NotNull StyleOption suffix) {
        if (plugin.getAPI().setSuffix(player, suffix.getValue())) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            
            String displayValue = stripColours(suffix.getValue());
            String message = plugin.getConfigManager().getMessagesConfig()
                .getMessage("suffix.changed", "{suffix}", displayValue);
            player.sendMessage(mm.deserialize(message));
            
            player.closeInventory();
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
    
    /**
     * Handle suffix reset
     */
    private void resetSuffix(@NotNull Player player) {
        if (plugin.getAPI().setSuffix(player, null)) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            
            Component message = mm.deserialize(
                plugin.getConfigManager().getMessagesConfig()
                    .getMessage("suffix.removed")
            );
            player.sendMessage(message);
            
            player.closeInventory();
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
    
    /**
     * Strip colour codes from a string
     */
    private String stripColours(@NotNull String input) {
        // Strip minimessage tags
        String stripped = input.replaceAll("<[^>]+>", "");
        // Strip legacy codes
        stripped = stripped.replaceAll("&[0-9a-fk-or]", "");
        stripped = stripped.replaceAll("§[0-9a-fk-or]", "");
        return stripped.trim();
    }
}