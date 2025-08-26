package dev.hxrry.hxprefix.commands;

import dev.hxrry.hxcore.utils.Log;
import dev.hxrry.hxcore.utils.Scheduler;

import dev.hxrry.hxprefix.HxPrefix;
import dev.hxrry.hxprefix.api.models.CustomTagRequest;
import dev.hxrry.hxprefix.api.models.PlayerCustomization;
import dev.hxrry.hxprefix.gui.menus.TagManagementMenu;

import io.papermc.paper.command.brigadier.Commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * admin command for managing the plugin
 */
public class AdminCommand extends BaseCommand {
    
    public AdminCommand(@NotNull HxPrefix plugin) {
        super(plugin, "hxprefix", "hxprefix.admin", false); // console allowed
    }
    
    @Override
    public void register(@NotNull Commands commands) {
        commands.register(
            Commands.literal(name)
                .requires(source -> source.getSender().hasPermission(permission))
                .executes(ctx -> {
                    // show help with no args
                    showHelp(ctx.getSource().getSender());
                    return 1;
                })
                // reload subcommand
                .then(Commands.literal("reload")
                    .executes(ctx -> {
                        reloadPlugin(ctx.getSource().getSender());
                        return 1;
                    })
                )
                // tags subcommand (manage tag requests)
                .then(Commands.literal("tags")
                    .executes(ctx -> {
                        CommandSender sender = ctx.getSource().getSender();
                        if (sender instanceof Player player) {
                            openTagManagement(player);
                        } else {
                            listPendingTags(sender);
                        }
                        return 1;
                    })
                    .then(Commands.literal("approve")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                            .executes(ctx -> {
                                int id = ctx.getArgument("id", Integer.class);
                                approveTag(ctx.getSource().getSender(), id);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("deny")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                            .then(Commands.argument("reason", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    int id = ctx.getArgument("id", Integer.class);
                                    String reason = ctx.getArgument("reason", String.class);
                                    denyTag(ctx.getSource().getSender(), id, reason);
                                    return 1;
                                })
                            )
                        )
                    )
                )
                // reset subcommand (clear player data)
                .then(Commands.literal("reset")
                    .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            // suggest online players
                            Bukkit.getOnlinePlayers().forEach(p -> 
                                builder.suggest(p.getName())
                            );
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            String playerName = ctx.getArgument("player", String.class);
                            resetPlayer(ctx.getSource().getSender(), playerName);
                            return 1;
                        })
                    )
                )
                // info subcommand (view player data)
                .then(Commands.literal("info")
                    .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            // suggest online players
                            Bukkit.getOnlinePlayers().forEach(p -> 
                                builder.suggest(p.getName())
                            );
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            String playerName = ctx.getArgument("player", String.class);
                            showPlayerInfo(ctx.getSource().getSender(), playerName);
                            return 1;
                        })
                    )
                )
                // cache subcommand
                .then(Commands.literal("cache")
                    .executes(ctx -> {
                        showCacheInfo(ctx.getSource().getSender());
                        return 1;
                    })
                    .then(Commands.literal("clear")
                        .executes(ctx -> {
                            clearCache(ctx.getSource().getSender());
                            return 1;
                        })
                    )
                )
                // migrate subcommand (from old plugin)
                .then(Commands.literal("migrate")
                    .executes(ctx -> {
                        startMigration(ctx.getSource().getSender());
                        return 1;
                    })
                )
                .build()
        );
    }
    
    /**
     * show admin help
     */
    private void showHelp(@NotNull CommandSender sender) {
        send(sender, "<gold>==== <white>HxPrefix Admin Commands <gold>====");
        send(sender, "<yellow>/hxadmin reload <gray>- reload configuration");
        send(sender, "<yellow>/hxadmin tags <gray>- manage tag requests");
        send(sender, "<yellow>/hxadmin tags approve <id> <gray>- approve a tag");
        send(sender, "<yellow>/hxadmin tags deny <id> <reason> <gray>- deny a tag");
        send(sender, "<yellow>/hxadmin reset <player> <gray>- reset player's customizations");
        send(sender, "<yellow>/hxadmin info <player> <gray>- view player's data");
        send(sender, "<yellow>/hxadmin cache <gray>- view cache statistics");
        send(sender, "<yellow>/hxadmin cache clear <gray>- clear the cache");
        send(sender, "<yellow>/hxadmin migrate <gray>- migrate from old plugin");
    }
    
    /**
     * reload the plugin
     */
    private void reloadPlugin(@NotNull CommandSender sender) {
        send(sender, "<yellow>reloading hxprefix configuration...");
        
        try {
            plugin.reload();
            sendSuccess(sender, "configuration reloaded successfully");
            
            // show stats
            send(sender, "<gray>loaded:");
            send(sender, "<gray>  • " + plugin.getConfigManager().getStyleConfig().getColourCount() + " colours");
            send(sender, "<gray>  • " + plugin.getConfigManager().getStyleConfig().getPrefixCount() + " prefixes");
            send(sender, "<gray>  • " + plugin.getConfigManager().getStyleConfig().getSuffixCount() + " suffixes");
            
        } catch (Exception e) {
            sendError(sender, "failed to reload: " + e.getMessage());
            Log.error("reload failed", e);
        }
    }
    
    /**
     * open tag management gui
     */
    private void openTagManagement(@NotNull Player player) {
        new TagManagementMenu(plugin, player).open(); // Remove player parameter
    }
    
    /**
     * list pending tags in console
     */
    private void listPendingTags(@NotNull CommandSender sender) {
        CompletableFuture.supplyAsync(() ->
            plugin.getDatabaseManager().getPendingTagRequests()
        ).thenAccept(requests -> {
            if (requests.isEmpty()) {
                send(sender, "<yellow>no pending tag requests");
                return;
            }
            
            send(sender, "<gold>==== <white>Pending Tag Requests <gold>====");
            for (CustomTagRequest request : requests) {
                send(sender, "<yellow>#" + request.getId() + " <white>" + 
                    request.getPlayerName() + " <gray>→ <white>" + 
                    request.getRequestedTag() + " <gray>(" + 
                    request.getAgeInDays() + " days old)");
            }
            send(sender, "<gray>use /hxadmin tags approve/deny <id> to manage");
        });
    }
    
    /**
     * approve a tag request
     */
    private void approveTag(@NotNull CommandSender sender, int id) {
        CompletableFuture.supplyAsync(() ->
            plugin.getDatabaseManager().getTagRequest(id)
        ).thenAccept(request -> {
            if (request == null) {
                sendError(sender, "tag request #" + id + " not found");
                return;
            }
            
            if (!request.isPending()) {
                sendError(sender, "tag request #" + id + " is already " + request.getStatus().getValue());
                return;
            }
            
            // approve it
            UUID approverUuid = sender instanceof Player ? 
                ((Player) sender).getUniqueId() : null;
            String approverName = sender.getName();
            
            request.approve(approverUuid, approverName);
            
            CompletableFuture.runAsync(() -> {
                if (plugin.getDatabaseManager().updateTagRequest(request)) {
                    // apply the tag to the player
                    Player target = Bukkit.getPlayer(request.getPlayerUuid());
                    if (target != null) {
                        plugin.getAPI().setPrefix(target, request.getFormattedTag());
                        send(target, "<green>✓ your custom tag has been approved!");
                    }
                    
                    Scheduler.runTask(() -> {
                        sendSuccess(sender, "approved tag request #" + id);
                        send(sender, "<gray>tag: " + request.getRequestedTag() + " for " + request.getPlayerName());
                    });
                } else {
                    Scheduler.runTask(() ->
                        sendError(sender, "failed to update tag request")
                    );
                }
            });
        });
    }
    
    /**
     * deny a tag request
     */
    private void denyTag(@NotNull CommandSender sender, int id, @NotNull String reason) {
        CompletableFuture.supplyAsync(() ->
            plugin.getDatabaseManager().getTagRequest(id)
        ).thenAccept(request -> {
            if (request == null) {
                sendError(sender, "tag request #" + id + " not found");
                return;
            }
            
            if (!request.isPending()) {
                sendError(sender, "tag request #" + id + " is already " + request.getStatus().getValue());
                return;
            }
            
            // deny it
            UUID denierUuid = sender instanceof Player ? 
                ((Player) sender).getUniqueId() : null;
            String denierName = sender.getName();
            
            request.deny(denierUuid, denierName, reason);
            
            CompletableFuture.runAsync(() -> {
                if (plugin.getDatabaseManager().updateTagRequest(request)) {
                    // notify the player if online
                    Player target = Bukkit.getPlayer(request.getPlayerUuid());
                    if (target != null) {
                        send(target, "<red>✗ your custom tag request was denied");
                        send(target, "<gray>reason: " + reason);
                    }
                    
                    Scheduler.runTask(() -> {
                        sendSuccess(sender, "denied tag request #" + id);
                        send(sender, "<gray>reason: " + reason);
                    });
                } else {
                    Scheduler.runTask(() ->
                        sendError(sender, "failed to update tag request")
                    );
                }
            });
        });
    }
    
    /**
     * reset a player's customizations
     */
    private void resetPlayer(@NotNull CommandSender sender, @NotNull String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        
        if (target != null) {
            // online player
            PlayerCustomization data = plugin.getAPI().getPlayerData(target);
            if (data != null) {
                data.clearAll();
                plugin.getDataCache().savePlayerData(data);
                
                sendSuccess(sender, "reset all customizations for " + playerName);
                send(target, "<yellow>⚠ your customizations have been reset by an admin");
                
                // update nametag
                if (plugin.getNametagManager() != null) {
                    plugin.getNametagManager().updatePlayer(target);
                }
            } else {
                sendError(sender, "no data found for " + playerName);
            }
        } else {
            sendError(sender, "player not online - offline reset not yet implemented");
        }
    }
    
    /**
     * show player info
     */
    private void showPlayerInfo(@NotNull CommandSender sender, @NotNull String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        
        if (target == null) {
            sendError(sender, "player not found");
            return;
        }
        
        PlayerCustomization data = plugin.getAPI().getPlayerData(target);
        String rank = plugin.getLuckPermsHook() != null ? 
            plugin.getLuckPermsHook().getPrimaryGroup(target) : "unknown";
        
        send(sender, "<gold>==== <white>" + playerName + " <gold>====");
        send(sender, "<yellow>rank: <white>" + rank);
        
        if (data != null) {
            send(sender, "<yellow>nickname: <white>" + (data.getNickname() != null ? data.getNickname() : "none"));
            send(sender, "<yellow>colour: <white>" + (data.getNameColour() != null ? data.getNameColour() : "none"));
            send(sender, "<yellow>prefix: <white>" + (data.getPrefix() != null ? data.getPrefix() : "none"));
            send(sender, "<yellow>suffix: <white>" + (data.getSuffix() != null ? data.getSuffix() : "none"));
            send(sender, "<yellow>has customizations: <white>" + data.hasCustomizations());
            send(sender, "<yellow>pending tag: <white>" + (data.hasPendingTagRequest() ? "yes" : "no"));
        } else {
            send(sender, "<gray>no customization data");
        }
    }
    
    /**
     * show cache info
     */
    private void showCacheInfo(@NotNull CommandSender sender) {
        int cached = plugin.getDataCache().getCacheSize();
        int online = Bukkit.getOnlinePlayers().size();
        
        send(sender, "<gold>==== <white>Cache Statistics <gold>====");
        send(sender, "<yellow>cached players: <white>" + cached);
        send(sender, "<yellow>online players: <white>" + online);
        send(sender, "<yellow>cache hit rate: <white>" + plugin.getDataCache().getHitRate() + "%");
        send(sender, "<yellow>memory usage: <white>~" + (cached * 256) + " bytes");
    }
    
    /**
     * clear the cache
     */
    private void clearCache(@NotNull CommandSender sender) {
        plugin.getDataCache().clearCache();
        sendSuccess(sender, "cache cleared");
        
        // reload online players
        for (Player online : Bukkit.getOnlinePlayers()) {
            plugin.getDataCache().loadPlayer(online.getUniqueId());
        }
        
        send(sender, "<gray>reloaded " + Bukkit.getOnlinePlayers().size() + " online players");
    }
    
    /**
     * start migration from old plugin
     */
    private void startMigration(@NotNull CommandSender sender) {
        send(sender, "<yellow>starting migration from playercustomisation...");
        
        // this would run the migration helper
        CompletableFuture.runAsync(() -> {
            try {
                int migrated = plugin.getMigrationHelper().migrate();
                Scheduler.runTask(() -> {
                    sendSuccess(sender, "migrated " + migrated + " players");
                });
            } catch (Exception e) {
                Scheduler.runTask(() -> {
                    sendError(sender, "migration failed: " + e.getMessage());
                });
            }
        });
    }
}