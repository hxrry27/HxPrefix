package com.yourserver.playercustomisation.gui;

import com.yourserver.playercustomisation.PlayerCustomisation;
import com.yourserver.playercustomisation.config.ConfigManager;
import com.yourserver.playercustomisation.utils.PermissionUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages all GUI menus for the plugin
 */
public class MenuManager implements Listener {
    private final PlayerCustomisation plugin;
    private final Map<UUID, AbstractMenu> openMenus = new HashMap<>();
    private final Map<UUID, Runnable> menuAnimations = new HashMap<>();
    
    public MenuManager(PlayerCustomisation plugin) {
        this.plugin = plugin;
        // Register events
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Opens the color selection menu for a player
     */
    public void openColorMenu(Player player) {
        String rank = PermissionUtils.getPlayerRank(player);
        
        // Check if they can use colors
        if (!plugin.getConfigManager().canUseColors(rank)) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission-rank")
                .replace("{rank}", rank));
            return;
        }
        
        ColorSelectionMenu menu = new ColorSelectionMenu(plugin, player, rank);
        openMenu(player, menu);
    }
    
    /**
     * Opens the prefix selection menu for a player
     */
    public void openPrefixMenu(Player player) {
        String rank = PermissionUtils.getPlayerRank(player);
        
        // Check if they can use prefixes
        if (!plugin.getConfigManager().canUsePrefix(rank)) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission-rank")
                .replace("{rank}", rank));
            return;
        }
        
        PrefixSelectionMenu menu = new PrefixSelectionMenu(plugin, player, rank);
        openMenu(player, menu);
    }
    
    /**
     * Opens the suffix selection menu for a player
     */
    public void openSuffixMenu(Player player) {
        String rank = PermissionUtils.getPlayerRank(player);
        
        // Check if they can use suffixes
        if (!plugin.getConfigManager().canUseSuffix(rank)) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission-rank")
                .replace("{rank}", rank));
            return;
        }
        
        SuffixSelectionMenu menu = new SuffixSelectionMenu(plugin, player, rank);
        openMenu(player, menu);
    }
    
    /**
     * Internal method to open a menu
     */
    private void openMenu(Player player, AbstractMenu menu) {
        // Close any existing menu
        closeMenu(player);
        
        // Store reference
        openMenus.put(player.getUniqueId(), menu);
        
        // Open the menu
        menu.open();
    }
    
    /**
     * Closes the current menu for a player
     */
    public void closeMenu(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Cancel any animations
        Runnable animation = menuAnimations.remove(uuid);
        if (animation instanceof org.bukkit.scheduler.BukkitTask) {
            ((org.bukkit.scheduler.BukkitTask) animation).cancel();
        }
        
        // Remove from tracking
        AbstractMenu menu = openMenus.remove(uuid);
        if (menu != null) {
            menu.onClose();
        }
    }
    
    /**
     * Registers an animation task for a menu
     */
    public void registerAnimation(Player player, Runnable task) {
        menuAnimations.put(player.getUniqueId(), task);
    }
    
    // Event handlers
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        AbstractMenu menu = openMenus.get(player.getUniqueId());
        if (menu == null) return;
        
        // Check if clicking in the menu inventory
        if (event.getInventory() != menu.getInventory()) return;
        
        // Cancel the event
        event.setCancelled(true);
        
        // Handle the click
        menu.handleClick(event.getSlot(), event.getClick());
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        closeMenu(player);
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        closeMenu(event.getPlayer());
    }
    
    /**
     * Shuts down the menu manager
     */
    public void shutdown() {
        // Close all open menus
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (openMenus.containsKey(player.getUniqueId())) {
                player.closeInventory();
            }
        }
        openMenus.clear();
        menuAnimations.clear();
    }
}