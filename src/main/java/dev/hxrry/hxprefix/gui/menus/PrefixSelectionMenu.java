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
import java.util.stream.Collectors;

/**
 * Paginated menu for selecting prefixes
 */
public class PrefixSelectionMenu {
    private final HxPrefix plugin;
    private final Player player;
    private final String rank;
    private final MiniMessage mm = MiniMessage.miniMessage();
    
    private Pagination pagination;
    
    public PrefixSelectionMenu(@NotNull HxPrefix plugin, @NotNull Player player) {
        this.plugin = plugin;
        this.player = player;
        this.rank = plugin.getLuckPermsHook() != null ?
            plugin.getLuckPermsHook().getPrimaryGroup(player) : "default";
    }
    
    /**
     * Open the menu
     */
    public void open() {
        // Get available prefixes for rank
        List<StyleOption> prefixes = plugin.getConfigManager().getStyleConfig()
            .getAvailablePrefixes(rank);
        
        // DEBUG: Log what we found
        plugin.getLogger().info("DEBUG: Opening prefix menu for player: " + player.getName());
        plugin.getLogger().info("DEBUG: Player rank: " + rank);
        plugin.getLogger().info("DEBUG: Found " + prefixes.size() + " prefixes for rank");
        for (StyleOption prefix : prefixes) {
            plugin.getLogger().info("  - " + prefix.getId() + " (ranks: " + prefix.getAllowedRanks() + ")");
        }
        
        // Filter out seasonal prefixes that aren't active
        prefixes = prefixes.stream()
            .filter(this::isAvailable)
            .collect(Collectors.toList());
        
        plugin.getLogger().info("DEBUG: After filtering seasonal: " + prefixes.size() + " prefixes");
        
        // Calculate rows needed
        int rows = GUIConstants.calculateRows(prefixes.size(), 3, 6);
        
        // Create pagination
        pagination = new Pagination(
            Component.text("Prefix Selection", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD),
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
        pagination.addItem(new MenuItem(createHeaderItem(prefixes.size())));
        
        // Add all prefix items
        for (StyleOption prefix : prefixes) {
            pagination.addItem(createPrefixItem(prefix));
        }
        
        // Add custom tag info if player has permission
        if (canUseCustomTags()) {
            pagination.addItem(createCustomTagInfoItem());
        }
        
        // Add reset button
        pagination.addItem(createResetItem());
        
        // Open for player
        pagination.open(player);
    }
    
    /**
     * Create header info item
     */
    private ItemStack createHeaderItem(int prefixCount) {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(mm.deserialize("<gold><bold>Available Prefixes")
            .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(List.of(
            mm.deserialize("<gray>Rank: <white>" + rank)
                .decoration(TextDecoration.ITALIC, false),
            mm.deserialize("<gray>Available prefixes: <white>" + prefixCount)
                .decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            mm.deserialize("<yellow>Click a prefix to apply!")
                .decoration(TextDecoration.ITALIC, false)
        ));
        
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Create prefix selection item
     */
    private MenuItem createPrefixItem(@NotNull StyleOption prefix) {
        ItemStack item = new ItemStack(prefix.getMaterial());
        ItemMeta meta = item.getItemMeta();
        
        // Set display name
        Component displayName = mm.deserialize("<white>" + prefix.getDisplayName());
        meta.displayName(displayName.decoration(TextDecoration.ITALIC, false));
        
        // Set lore
        List<Component> lore = new ArrayList<>();
        
        // Add preview
        String formattedPrefix = mm.serialize(mm.deserialize(prefix.getValue()));
        lore.add(mm.deserialize("<gray>Preview: " + formattedPrefix + " <white>" + 
                               player.getName() + " <gray>» <white>hello!")
            .decoration(TextDecoration.ITALIC, false));
        
        // Add seasonal tag if applicable
        if (prefix.isSeasonal()) {
            lore.add(Component.empty());
            lore.add(mm.deserialize("<aqua>✦ Seasonal Prefix ✦")
                .decoration(TextDecoration.ITALIC, false));
        }
        
        lore.add(Component.empty());
        lore.add(mm.deserialize("<yellow>Click to apply!")
            .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(lore);
        
        // Add glow if needed
        if (prefix.hasGlow()) {
            meta.setEnchantmentGlintOverride(true);
        }
        
        item.setItemMeta(meta);
        
        // Create menu item with click handler
        return new MenuItem(item, event -> {
            event.setCancelled(true);
            Player p = (Player) event.getWhoClicked();
            selectPrefix(p, prefix);
        });
    }
    
    /**
     * Create custom tag info item
     */
    private MenuItem createCustomTagInfoItem() {
        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(mm.deserialize("<yellow><bold>Custom Tags")
            .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(List.of(
            mm.deserialize("<gray>Want a unique prefix?")
                .decoration(TextDecoration.ITALIC, false),
            mm.deserialize("<gray>Use <white>/hxtag <name> <gray>to request one!")
                .decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            mm.deserialize("<gray>Staff will review your request")
                .decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            mm.deserialize("<yellow>Click for more info!")
                .decoration(TextDecoration.ITALIC, false)
        ));
        
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        
        return new MenuItem(item, event -> {
            event.setCancelled(true);
            Player p = (Player) event.getWhoClicked();
            p.closeInventory();
            p.sendMessage(mm.deserialize("<gray>Use <white>/hxtag <name> <gray>to request a custom prefix!"));
            p.sendMessage(mm.deserialize("<gray>Example: <white>/hxtag LEGEND"));
            p.sendMessage(mm.deserialize("<gray>Staff will review and approve/deny your request"));
        });
    }
    
    /**
     * Create reset button item
     */
    private MenuItem createResetItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(mm.deserialize("<red><bold>Remove Prefix")
            .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(List.of(
            mm.deserialize("<gray>Remove your current prefix")
                .decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            mm.deserialize("<red>Click to remove!")
                .decoration(TextDecoration.ITALIC, false)
        ));
        
        item.setItemMeta(meta);
        
        return new MenuItem(item, event -> {
            event.setCancelled(true);
            Player p = (Player) event.getWhoClicked();
            resetPrefix(p);
        });
    }
    
    /**
     * Handle prefix selection
     */
    private void selectPrefix(@NotNull Player player, @NotNull StyleOption prefix) {
        if (plugin.getAPI().setPrefix(player, prefix.getValue())) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            
            String message = plugin.getConfigManager().getMessagesConfig()
                .getMessage("prefix.changed", "{prefix}", prefix.getDisplayName());
            player.sendMessage(mm.deserialize(message));
            
            player.closeInventory();
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
    
    /**
     * Handle prefix reset
     */
    private void resetPrefix(@NotNull Player player) {
        if (plugin.getAPI().setPrefix(player, null)) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            
            Component message = mm.deserialize(
                plugin.getConfigManager().getMessagesConfig()
                    .getMessage("prefix.removed")
            );
            player.sendMessage(message);
            
            player.closeInventory();
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
    
    /**
     * Check if a prefix is available (not seasonal or season is active)
     */
    private boolean isAvailable(@NotNull StyleOption prefix) {
        if (!prefix.isSeasonal()) {
            return true;
        }
        
        // Check if season is active
        if (prefix.getMetadata() == null) return true;
        
        Object seasonal = prefix.getMetadata().get("seasonal");
        if (!(seasonal instanceof String season)) return true;
        
        // Check config for active seasons
        return plugin.getConfigManager().getMainConfig()
            .getBoolean("events." + season + ".active", false);
    }
    
    /**
     * Check if player can use custom tags
     */
    private boolean canUseCustomTags() {
        return plugin.getConfigManager().getPermissionConfig()
            .hasPermission(player, "custom-tags");
    }
}