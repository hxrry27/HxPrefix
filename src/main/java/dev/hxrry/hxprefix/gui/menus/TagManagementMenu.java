package dev.hxrry.hxprefix.gui.menus;

import dev.hxrry.hxcore.utils.Scheduler;

import dev.hxrry.hxgui.components.Pagination;
import dev.hxrry.hxgui.core.MenuItem;

import dev.hxrry.hxprefix.HxPrefix;
import dev.hxrry.hxprefix.api.models.CustomTagRequest;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * admin menu for managing tag requests
 */
public class TagManagementMenu {
    private final HxPrefix plugin;
    private final Player admin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private Pagination pagination;
    
    public TagManagementMenu(@NotNull HxPrefix plugin, @NotNull Player admin) {
        this.plugin = plugin;
        this.admin = admin;
    }
    
    /**
     * open the menu
     */
    public void open() {
        loadAndDisplay();
    }
    
    /**
     * load requests and display menu
     */
    private void loadAndDisplay() {
        // show loading message
        admin.sendMessage(mm.deserialize("<gray>Loading tag requests..."));
        
        CompletableFuture.supplyAsync(() ->
            plugin.getDatabaseManager().getPendingTagRequests()
        ).thenAccept(requests -> {
            Scheduler.runTask(() -> {
                if (requests.isEmpty()) {
                    // show empty menu
                    showEmptyMenu();
                } else {
                    // create pagination menu with requests
                    showRequestsMenu(requests);
                }
            });
        }).exceptionally(ex -> {
            Scheduler.runTask(() -> {
                admin.sendMessage(mm.deserialize("<red>Failed to load tag requests!"));
                plugin.getLogger().severe("Failed to load tag requests: " + ex.getMessage());
            });
            return null;
        });
    }
    
    /**
     * show menu when no requests exist
     */
    private void showEmptyMenu() {
        // create a simple menu showing no requests
        pagination = new Pagination(
            Component.text("Tag Request Management", NamedTextColor.GOLD, TextDecoration.BOLD),
            3 // small menu for empty state
        );
        
        // add empty message item
        ItemStack emptyItem = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta meta = emptyItem.getItemMeta();
        
        meta.displayName(mm.deserialize("<green><bold>No Pending Requests!")
            .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(List.of(
            mm.deserialize("<gray>All tag requests have been reviewed")
                .decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            mm.deserialize("<gray>New requests will appear here")
                .decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            mm.deserialize("<yellow>Click to refresh")
                .decoration(TextDecoration.ITALIC, false)
        ));
        
        meta.setEnchantmentGlintOverride(true);
        emptyItem.setItemMeta(meta);
        
        // add the empty message as a clickable item that refreshes
        pagination.addItem(new MenuItem(emptyItem, event -> {
            event.setCancelled(true);
            Player p = (Player) event.getWhoClicked();
            p.closeInventory();
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            loadAndDisplay(); // Refresh
        }));
        
        // add refresh button
        ItemStack refreshItem = createRefreshItem();
        pagination.addItem(new MenuItem(refreshItem, event -> {
            event.setCancelled(true);
            Player p = (Player) event.getWhoClicked();
            p.closeInventory();
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            loadAndDisplay();
        }));
        
        pagination.open(admin);
    }
    
    /**
     * show menu with a/some requests
     */
    private void showRequestsMenu(@NotNull List<CustomTagRequest> requests) {
        // create pagination with appropriate size
        int rows = Math.min(6, Math.max(3, (requests.size() / 7) + 2)); // Dynamic sizing
        pagination = new Pagination(
            Component.text("Tag Request Management", NamedTextColor.GOLD, TextDecoration.BOLD),
            rows
        );
        
        // configure pagination areas
        pagination.contentArea(0, (rows - 1) * 9 - 1); // All but bottom row
        pagination.navigationSlots(rows * 9 - 6, rows * 9 - 4, rows * 9 - 5); // Bottom row positions
        
        // add header info item first
        ItemStack headerItem = createHeaderItem(requests.size());
        pagination.addItem(new MenuItem(headerItem));
        
        // add all request items
        for (CustomTagRequest request : requests) {
            pagination.addItem(createRequestItem(request));
        }
        
        // add control items at the end
        pagination.addItem(new MenuItem(createRefreshItem(), event -> {
            event.setCancelled(true);
            Player p = (Player) event.getWhoClicked();
            p.closeInventory();
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            loadAndDisplay();
        }));
        
        pagination.addItem(new MenuItem(createAutoExpireItem(), event -> {
            event.setCancelled(true);
            Player p = (Player) event.getWhoClicked();
            p.playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);
            expireOldRequests();
        }));
        
        pagination.open(admin);
    }
    
    /**
     * create header info item
     */
    private ItemStack createHeaderItem(int requestCount) {
        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(mm.deserialize("<gold><bold>Pending Tag Requests")
            .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(List.of(
            mm.deserialize("<gray>Total pending: <white>" + requestCount)
                .decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            mm.deserialize("<green>Left click to approve")
                .decoration(TextDecoration.ITALIC, false),
            mm.deserialize("<red>Right click to deny")
                .decoration(TextDecoration.ITALIC, false),
            mm.deserialize("<yellow>Middle click for details")
                .decoration(TextDecoration.ITALIC, false)
        ));
        
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * create refresh button item
     */
    private ItemStack createRefreshItem() {
        ItemStack item = new ItemStack(Material.LIME_DYE);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(mm.deserialize("<green><bold>Refresh")
            .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(List.of(
            mm.deserialize("<gray>Reload pending requests")
                .decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            mm.deserialize("<yellow>Click to refresh!")
                .decoration(TextDecoration.ITALIC, false)
        ));
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * create auto-expire button item
     */
    private ItemStack createAutoExpireItem() {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(mm.deserialize("<yellow><bold>Auto-Expire Old")
            .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(List.of(
            mm.deserialize("<gray>Expire requests older than 30 days")
                .decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            mm.deserialize("<yellow>Click to run!")
                .decoration(TextDecoration.ITALIC, false)
        ));
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * create item for a tag request
     */
    private MenuItem createRequestItem(@NotNull CustomTagRequest request) {
        // create player head
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        
        // set owner
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(request.getPlayerUuid()));
        
        // set display name
        meta.displayName(mm.deserialize("<yellow>" + request.getPlayerName())
            .decoration(TextDecoration.ITALIC, false));
        
        // set lore
        List<Component> lore = new ArrayList<>();
        lore.add(mm.deserialize("<gray>Requested tag: <white>" + request.getRequestedTag())
            .decoration(TextDecoration.ITALIC, false));
        lore.add(mm.deserialize("<gray>Age: <white>" + request.getAgeInDays() + " days")
            .decoration(TextDecoration.ITALIC, false));
        lore.add(mm.deserialize("<gray>Request ID: <white>#" + request.getId())
            .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        
        // add age warning if old
        if (request.getAgeInDays() > 7) {
            lore.add(mm.deserialize("<red>⚠ Old request")
                .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
        }
        
        lore.add(mm.deserialize("<green>Left click to approve")
            .decoration(TextDecoration.ITALIC, false));
        lore.add(mm.deserialize("<red>Right click to deny")
            .decoration(TextDecoration.ITALIC, false));
        lore.add(mm.deserialize("<yellow>Middle click for details")
            .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        
        // create menu item with click handlers
        return new MenuItem(item, event -> {
            event.setCancelled(true);
            Player p = (Player) event.getWhoClicked();
            
            switch (event.getClick()) {
                case LEFT -> {
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_YES, 1.0f, 1.0f);
                    approveRequest(request);
                }
                case RIGHT -> {
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    denyRequest(request);
                }
                case MIDDLE -> {
                    p.closeInventory();
                    showDetails(request);
                }
                default -> {} // ignore other clicks
            }
        });
    }
    
    /**
     * approve a request
     */
    private void approveRequest(@NotNull CustomTagRequest request) {
        admin.sendMessage(mm.deserialize("<yellow>Approving tag request #" + request.getId() + "..."));
        
        // approve in database
        request.approve(admin.getUniqueId(), admin.getName());
        
        CompletableFuture.runAsync(() -> {
            if (plugin.getDatabaseManager().updateTagRequest(request)) {
                // apply to player if they online
                Player target = Bukkit.getPlayer(request.getPlayerUuid());
                if (target != null) {
                    Scheduler.runTask(() -> {
                        plugin.getAPI().setPrefix(target, request.getFormattedTag());
                        target.sendMessage(mm.deserialize("<green>✓ Your custom tag has been approved!"));
                        target.playSound(target.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                    });
                }
                
                Scheduler.runTask(() -> {
                    admin.sendMessage(mm.deserialize("<green>✓ Approved tag request #" + request.getId()));
                    admin.closeInventory();
                    loadAndDisplay(); // refresh menu
                });
            } else {
                Scheduler.runTask(() -> {
                    admin.sendMessage(mm.deserialize("<red>Failed to approve request!"));
                });
            }
        });
    }
    
    /**
     * deny a request
     */
    private void denyRequest(@NotNull CustomTagRequest request) {
        // for simplicity, using a default reason
        // TODO: could implement an anvil GUI for custom reason
        String reason = "Inappropriate or already exists";
        
        admin.sendMessage(mm.deserialize("<yellow>Denying tag request #" + request.getId() + "..."));
        
        request.deny(admin.getUniqueId(), admin.getName(), reason);
        
        CompletableFuture.runAsync(() -> {
            if (plugin.getDatabaseManager().updateTagRequest(request)) {
                // notify player if they online
                Player target = Bukkit.getPlayer(request.getPlayerUuid());
                if (target != null) {
                    Scheduler.runTask(() -> {
                        target.sendMessage(mm.deserialize("<red>✗ Your custom tag request was denied"));
                        target.sendMessage(mm.deserialize("<gray>Reason: " + reason));
                        target.playSound(target.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    });
                }
                
                Scheduler.runTask(() -> {
                    admin.sendMessage(mm.deserialize("<red>✗ Denied tag request #" + request.getId()));
                    admin.closeInventory();
                    loadAndDisplay(); // refresh the menu
                });
            } else {
                Scheduler.runTask(() -> {
                    admin.sendMessage(mm.deserialize("<red>Failed to deny request!"));
                });
            }
        });
    }
    
    /**
     * show request details
     */
    private void showDetails(@NotNull CustomTagRequest request) {
        admin.sendMessage(mm.deserialize("<gold>==== Tag Request #" + request.getId() + " ===="));
        admin.sendMessage(mm.deserialize("<yellow>Player: <white>" + request.getPlayerName()));
        admin.sendMessage(mm.deserialize("<yellow>UUID: <gray>" + request.getPlayerUuid()));
        admin.sendMessage(mm.deserialize("<yellow>Requested Tag: <white>" + request.getRequestedTag()));
        admin.sendMessage(mm.deserialize("<yellow>Age: <white>" + request.getAgeInDays() + " days"));
        admin.sendMessage(mm.deserialize("<yellow>Status: <white>" + request.getStatus().getValue()));
        
        // check if player is online
        Player target = Bukkit.getPlayer(request.getPlayerUuid());
        if (target != null) {
            admin.sendMessage(mm.deserialize("<green>Player is currently online"));
        } else {
            admin.sendMessage(mm.deserialize("<gray>Player is offline"));
        }
    }
    
    /**
     * expire old requests
     */
    private void expireOldRequests() {
        admin.sendMessage(mm.deserialize("<yellow>Checking for old requests..."));
        
        CompletableFuture.supplyAsync(() ->
            plugin.getDatabaseManager().getPendingTagRequests()
        ).thenAccept(requests -> {
            int expired = 0;
            
            for (CustomTagRequest request : requests) {
                if (request.shouldExpire(30)) {
                    request.expire();
                    plugin.getDatabaseManager().updateTagRequest(request);
                    expired++;
                }
            }
            
            final int expiredCount = expired;
            Scheduler.runTask(() -> {
                if (expiredCount > 0) {
                    admin.sendMessage(mm.deserialize("<yellow>Expired " + expiredCount + " old requests"));
                    admin.closeInventory();
                    loadAndDisplay(); // Refresh
                } else {
                    admin.sendMessage(mm.deserialize("<gray>No requests old enough to expire"));
                }
            });
        });
    }
}