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

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * menu for selecting prefixes
 */
public class PrefixSelectionMenu extends SharedMenu {
    private final HxPrefix plugin;
    private final Player player;
    private final String rank;
    private final MiniMessage mm = MiniMessage.miniMessage();
    
    public PrefixSelectionMenu(@NotNull HxPrefix plugin, @NotNull Player player) {
        super(Component.text("Prefix Selection", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD),
              6);
        
        this.plugin = plugin;
        this.player = player;
        this.rank = plugin.getLuckPermsHook() != null ?
            plugin.getLuckPermsHook().getPrimaryGroup(player) : "default";
        
        build();
    }
    
    /**
     * build the menu contents
     */
    private void build() {
        // get available prefixes for rank
        List<StyleOption> prefixes = plugin.getConfigManager().getStyleConfig()
            .getAvailablePrefixes(rank);
        
        // filter out seasonal prefixes that aren't active atm
        prefixes = prefixes.stream()
            .filter(this::isAvailable)
            .collect(Collectors.toList());
        
        // add title
        addTitle(4);
        
        // display prefixes in grid
        displayPrefixes(prefixes);
        
        // add custom tag info if player has permission
        if (canUseCustomTags()) {
            addCustomTagInfo(43);
        }
        
        // add reset button
        addResetButton(49);
        
        // fill empty slots
        fillEmpty();
    }
    
    /**
     * display prefixes in a grid
     */
    private void displayPrefixes(@NotNull List<StyleOption> prefixes) {
        int[] slots = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42
        };
        
        int index = 0;
        for (StyleOption prefix : prefixes) {
            if (index >= slots.length) break;
            
            addPrefixOption(slots[index], prefix);
            index++;
        }
    }
    
    /**
     * add a prefix option to the menu
     */
    private void addPrefixOption(int slot, @NotNull StyleOption prefix) {
        // create item
        ItemStack item = new ItemStack(prefix.getMaterial());
        ItemMeta meta = item.getItemMeta();
        
        // set display name
        Component displayName = mm.deserialize("<white>" + prefix.getDisplayName());
        meta.displayName(displayName.decoration(TextDecoration.ITALIC, false));
        
        // set lore
        List<Component> lore = new ArrayList<>();
        
        // add preview
        String formattedPrefix = mm.serialize(mm.deserialize(prefix.getValue()));
        lore.add(mm.deserialize("<gray>preview: " + formattedPrefix + " <white>" + 
                               player.getName() + " <gray>» <white>hello!")
            .decoration(TextDecoration.ITALIC, false));
        
        // add seasonal tag if applicable
        if (prefix.isSeasonal()) {
            lore.add(Component.empty());
            lore.add(mm.deserialize("<aqua>✦ seasonal prefix ✦")
                .decoration(TextDecoration.ITALIC, false));
        }
        
        lore.add(Component.empty());
        lore.add(mm.deserialize("<yellow>click to apply!")
            .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(lore);
        
        // add glow if needed
        if (prefix.hasGlow()) {
            meta.setEnchantmentGlintOverride(true);
        }
        
        item.setItemMeta(meta);
        
        // create menu item with click handler
        setItem(slot, new MenuItem(item, event -> {
            event.setCancelled(true);
            Player p = (Player) event.getWhoClicked();
            selectPrefix(p, prefix);
        }));
    }
    
    /**
     * add title header
     */
    private void addTitle(int slot) {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(mm.deserialize("<gold><bold>available prefixes")
            .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(List.of(
            mm.deserialize("<gray>rank: <white>" + rank)
                .decoration(TextDecoration.ITALIC, false),
            mm.deserialize("<gray>choose a prefix below")
                .decoration(TextDecoration.ITALIC, false)
        ));
        
        item.setItemMeta(meta);
        
        setItem(slot, new MenuItem(item));
    }
    
    /**
     * add custom tag info
     */
    private void addCustomTagInfo(int slot) {
        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(mm.deserialize("<yellow><bold>custom tags")
            .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(List.of(
            mm.deserialize("<gray>want a unique prefix?")
                .decoration(TextDecoration.ITALIC, false),
            mm.deserialize("<gray>use <white>/hxtag <name> <gray>to request one!")
                .decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            mm.deserialize("<gray>staff will review your request")
                .decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            mm.deserialize("<yellow>click for more info!")
                .decoration(TextDecoration.ITALIC, false)
        ));
        
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        
        setItem(slot, new MenuItem(item, event -> {
            event.setCancelled(true);
            Player p = (Player) event.getWhoClicked();
            p.closeInventory();
            p.sendMessage(mm.deserialize("<gray>use <white>/hxtag <name> <gray>to request a custom prefix!"));
            p.sendMessage(mm.deserialize("<gray>example: <white>/hxtag LEGEND"));
            p.sendMessage(mm.deserialize("<gray>staff will review and approve/deny your request"));
        }));
    }
    
    /**
     * add reset button
     */
    private void addResetButton(int slot) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(mm.deserialize("<red><bold>remove prefix")
            .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(List.of(
            mm.deserialize("<gray>remove your current prefix")
                .decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            mm.deserialize("<red>click to remove!")
                .decoration(TextDecoration.ITALIC, false)
        ));
        
        item.setItemMeta(meta);
        
        setItem(slot, new MenuItem(item, event -> {
            event.setCancelled(true);
            Player p = (Player) event.getWhoClicked();
            resetPrefix(p);
        }));
    }
    
    /**
     * fill empty slots with glass
     */
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
    
    /**
     * handle prefix selection
     */
    private void selectPrefix(@NotNull Player player, @NotNull StyleOption prefix) {
        // apply the prefix
        if (plugin.getAPI().setPrefix(player, prefix.getValue())) {
            // success
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            
            String message = plugin.getConfigManager().getMessagesConfig()
                .getMessage("prefix.changed", "{prefix}", prefix.getDisplayName());
            player.sendMessage(mm.deserialize(message));
            
            player.closeInventory();
        } else {
            // failed
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
    
    /**
     * handle prefix reset
     */
    private void resetPrefix(@NotNull Player player) {
        if (plugin.getAPI().setPrefix(player, null)) {
            // success
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            
            Component message = mm.deserialize(
                plugin.getConfigManager().getMessagesConfig()
                    .getMessage("prefix.removed")
            );
            player.sendMessage(message);
            
            player.closeInventory();
        } else {
            // failed
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
    
    /**
     * check if a prefix is available (not seasonal or season is active)
     */
    private boolean isAvailable(@NotNull StyleOption prefix) {
        if (!prefix.isSeasonal()) {
            return true;
        }
        
        // check if season is active
        if (prefix.getMetadata() == null) return true;
        
        Object seasonal = prefix.getMetadata().get("seasonal");
        if (!(seasonal instanceof String season)) return true;
        
        // check config for active seasons
        return plugin.getConfigManager().getMainConfig()
            .getBoolean("events." + season + ".active", false);
    }
    
    /**
     * check if player can even use custom tags
     */
    private boolean canUseCustomTags() {
        return plugin.getConfigManager().getPermissionConfig()
            .hasPermission(player, "custom-tags");
    }
}