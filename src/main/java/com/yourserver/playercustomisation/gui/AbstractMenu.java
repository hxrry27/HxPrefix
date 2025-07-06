package com.yourserver.playercustomisation.gui;

import com.yourserver.playercustomisation.PlayerCustomisation;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Base class for all GUI menus
 * All materials and configurations are loaded from config files
 */
public abstract class AbstractMenu {
    protected final PlayerCustomisation plugin;
    protected final Player player;
    protected final String rank;
    protected final Inventory inventory;
    protected final Map<Integer, Consumer<ClickType>> clickHandlers = new HashMap<>();
    
    public AbstractMenu(PlayerCustomisation plugin, Player player, String rank, String title, int size) {
        this.plugin = plugin;
        this.player = player;
        this.rank = rank;
        this.inventory = Bukkit.createInventory(null, size, MenuUtils.colorize(title));
    }
    
    /**
     * Opens the menu for the player
     */
    public void open() {
        build();
        player.openInventory(inventory);
    }
    
    /**
     * Builds the menu contents
     */
    protected abstract void build();
    
    /**
     * Handles a click in the menu
     */
    public void handleClick(int slot, ClickType clickType) {
        Consumer<ClickType> handler = clickHandlers.get(slot);
        if (handler != null) {
            handler.accept(clickType);
        }
    }
    
    /**
     * Called when the menu is closed
     */
    public void onClose() {
        // Override in subclasses if needed
    }
    
    /**
     * Sets an item in the inventory with a click handler
     */
    protected void setItem(int slot, ItemStack item, Consumer<ClickType> clickHandler) {
        inventory.setItem(slot, item);
        if (clickHandler != null) {
            clickHandlers.put(slot, clickHandler);
        }
    }
    
    /**
     * Sets an item with a simple click handler (ignores click type)
     */
    protected void setItem(int slot, ItemStack item, Runnable clickHandler) {
        setItem(slot, item, clickHandler != null ? (clickType) -> clickHandler.run() : null);
    }
    
    /**
     * Sets an item without any click handler (decoration only)
     */
    protected void setItem(int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }
    
    /**
     * Fills a range of slots with an item
     */
    protected void fillSlots(int start, int end, ItemStack item) {
        for (int i = start; i <= end; i++) {
            inventory.setItem(i, item);
        }
    }
    
    /**
     * Creates a basic item
     */
    protected ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (name != null) {
            meta.setDisplayName(MenuUtils.colorize(name));
        }
        
        if (lore != null && !lore.isEmpty()) {
            meta.setLore(lore.stream()
                .map(MenuUtils::colorize)
                .collect(Collectors.toList()));
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Creates an item with a glow effect
     */
    protected ItemStack createGlowingItem(Material material, String name, List<String> lore) {
        ItemStack item = createItem(material, name, lore);
        addGlow(item);
        return item;
    }
    
    /**
     * Adds a glow effect to an item
     */
    protected void addGlow(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(Enchantment.MENDING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
    }
    
    /**
     * Creates a reset button
     * Material should be configured in each menu's config
     */
    protected ItemStack createResetButton(String text) {
        // This is a generic implementation - each menu should override with its own configured material
        return createItem(Material.BARRIER, "&c&lReset " + text, Arrays.asList(
            "&7Remove your " + text.toLowerCase(),
            "",
            "&cClick to reset!"
        ));
    }
    
    /**
     * Creates a back button
     * Material should be configured if used
     */
    protected ItemStack createBackButton(Material material) {
        return createItem(material, "&7« Back", Arrays.asList(
            "&7Return to previous menu"
        ));
    }
    
    /**
     * Gets the inventory
     */
    public Inventory getInventory() {
        return inventory;
    }
    
    /**
     * Gets the player
     */
    public Player getPlayer() {
        return player;
    }
    
    /**
     * Gets the player's rank
     */
    public String getRank() {
        return rank;
    }
    
    /**
     * Gets the plugin instance
     */
    public PlayerCustomisation getPlugin() {
        return plugin;
    }
}