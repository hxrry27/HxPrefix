package dev.hxrry.hxprefix.commands;

import dev.hxrry.hxcore.utils.Log;

import dev.hxrry.hxprefix.HxPrefix;
import dev.hxrry.hxprefix.api.models.PlayerCustomization;

import io.papermc.paper.command.brigadier.Commands;

import com.mojang.brigadier.arguments.StringArgumentType;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;

/**
 * Admin command for managing the plugin
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
                // set subcommand
                .then(Commands.literal("set")
                    .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                            return builder.buildFuture();
                        })
                        .then(Commands.literal("prefix")
                            .then(Commands.argument("value", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    String player = ctx.getArgument("player", String.class);
                                    String value = ctx.getArgument("value", String.class);
                                    setPlayerData(ctx.getSource().getSender(), player, "prefix", value);
                                    return 1;
                                })
                            )
                        )
                        .then(Commands.literal("suffix")
                            .then(Commands.argument("value", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    String player = ctx.getArgument("player", String.class);
                                    String value = ctx.getArgument("value", String.class);
                                    setPlayerData(ctx.getSource().getSender(), player, "suffix", value);
                                    return 1;
                                })
                            )
                        )
                        .then(Commands.literal("nickname")
                            .then(Commands.argument("value", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    String player = ctx.getArgument("player", String.class);
                                    String value = ctx.getArgument("value", String.class);
                                    setPlayerData(ctx.getSource().getSender(), player, "nickname", value);
                                    return 1;
                                })
                            )
                        )
                        .then(Commands.literal("namecolour")
                            .then(Commands.argument("value", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    String player = ctx.getArgument("player", String.class);
                                    String value = ctx.getArgument("value", String.class);
                                    setPlayerData(ctx.getSource().getSender(), player, "namecolour", value);
                                    return 1;
                                })
                            )
                        )
                    )
                )
                // clear subcommand
                .then(Commands.literal("clear")
                    .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                            return builder.buildFuture();
                        })
                        .then(Commands.literal("prefix")
                            .executes(ctx -> {
                                String player = ctx.getArgument("player", String.class);
                                clearPlayerData(ctx.getSource().getSender(), player, "prefix");
                                return 1;
                            })
                        )
                        .then(Commands.literal("suffix")
                            .executes(ctx -> {
                                String player = ctx.getArgument("player", String.class);
                                clearPlayerData(ctx.getSource().getSender(), player, "suffix");
                                return 1;
                            })
                        )
                        .then(Commands.literal("nickname")
                            .executes(ctx -> {
                                String player = ctx.getArgument("player", String.class);
                                clearPlayerData(ctx.getSource().getSender(), player, "nickname");
                                return 1;
                            })
                        )
                        .then(Commands.literal("namecolour")
                            .executes(ctx -> {
                                String player = ctx.getArgument("player", String.class);
                                clearPlayerData(ctx.getSource().getSender(), player, "namecolour");
                                return 1;
                            })
                        )
                        .then(Commands.literal("all")
                            .executes(ctx -> {
                                String player = ctx.getArgument("player", String.class);
                                clearPlayerData(ctx.getSource().getSender(), player, "all");
                                return 1;
                            })
                        )
                    )
                )
                // info subcommand
                .then(Commands.literal("info")
                    .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
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
                .build()
        );
    }
    
    /**
     * Show admin help
     */
    private void showHelp(@NotNull CommandSender sender) {
        send(sender, "<gold>==== <white>HxPrefix Admin <gold>====");
        send(sender, "<yellow>/hxprefix reload <gray>- reload configuration");
        send(sender, "");
        send(sender, "<yellow>/hxprefix set <player> prefix <text> <gray>- set prefix");
        send(sender, "<yellow>/hxprefix set <player> suffix <text> <gray>- set suffix");
        send(sender, "<yellow>/hxprefix set <player> nickname <text> <gray>- set nickname");
        send(sender, "<yellow>/hxprefix set <player> namecolour <text> <gray>- set name colour");
        send(sender, "");
        send(sender, "<yellow>/hxprefix clear <player> <type> <gray>- clear data");
        send(sender, "<gray>  types: prefix, suffix, nickname, namecolour, all");
        send(sender, "");
        send(sender, "<yellow>/hxprefix info <player> <gray>- view player data");
        send(sender, "<yellow>/hxprefix cache <gray>- view cache statistics");
        send(sender, "<yellow>/hxprefix cache clear <gray>- clear cache");
    }
    
    /**
     * Reload the plugin
     */
    private void reloadPlugin(@NotNull CommandSender sender) {
        send(sender, "<yellow>Reloading HxPrefix configuration...");
        
        try {
            plugin.reload();
            sendSuccess(sender, "Configuration reloaded successfully");
            
            // Show stats
            send(sender, "<gray>Loaded:");
            send(sender, "<gray>  • " + plugin.getConfigManager().getStyleConfig().getColourCount() + " colours");
            send(sender, "<gray>  • " + plugin.getConfigManager().getStyleConfig().getPrefixCount() + " prefixes");
            send(sender, "<gray>  • " + plugin.getConfigManager().getStyleConfig().getSuffixCount() + " suffixes");
            
        } catch (Exception e) {
            sendError(sender, "Failed to reload: " + e.getMessage());
            Log.error("Reload failed", e);
        }
    }
    
    /**
     * Set player data
     */
    private void setPlayerData(@NotNull CommandSender sender, @NotNull String playerName, 
                              @NotNull String dataType, @NotNull String value) {
        Player target = Bukkit.getPlayer(playerName);
        
        if (target == null) {
            sendError(sender, "Player not online");
            return;
        }
        
        PlayerCustomization data = plugin.getAPI().getPlayerData(target);
        if (data == null) {
            data = plugin.getDataCache().getOrCreatePlayerData(target.getUniqueId());
        }
        
        switch (dataType.toLowerCase()) {
            case "prefix" -> {
                data.setPrefix(value);
                sendSuccess(sender, "Set prefix for " + playerName + " to: " + value);
            }
            case "suffix" -> {
                data.setSuffix(value);
                sendSuccess(sender, "Set suffix for " + playerName + " to: " + value);
            }
            case "nickname" -> {
                data.setNickname(value);
                sendSuccess(sender, "Set nickname for " + playerName + " to: " + value);
            }
            case "namecolour", "namecolor" -> {
                data.setNameColour(value);
                sendSuccess(sender, "Set name colour for " + playerName + " to: " + value);
            }
            default -> {
                sendError(sender, "Invalid type. Use: prefix, suffix, nickname, namecolour");
                return;
            }
        }
        
        // Save and update
        plugin.getDataCache().savePlayerData(data);
        
        send(target, "<green>✓ Your " + dataType + " has been updated by an admin");
    }
    
    /**
     * Clear player data
     */
    private void clearPlayerData(@NotNull CommandSender sender, @NotNull String playerName, 
                                @NotNull String dataType) {
        Player target = Bukkit.getPlayer(playerName);
        
        if (target == null) {
            sendError(sender, "Player not online");
            return;
        }
        
        PlayerCustomization data = plugin.getAPI().getPlayerData(target);
        if (data == null) {
            sendError(sender, "No data found for " + playerName);
            return;
        }
        
        switch (dataType.toLowerCase()) {
            case "prefix" -> {
                data.setPrefix(null);
                sendSuccess(sender, "Cleared prefix for " + playerName);
            }
            case "suffix" -> {
                data.setSuffix(null);
                sendSuccess(sender, "Cleared suffix for " + playerName);
            }
            case "nickname" -> {
                data.setNickname(null);
                sendSuccess(sender, "Cleared nickname for " + playerName);
            }
            case "namecolour", "namecolor" -> {
                data.setNameColour(null);
                sendSuccess(sender, "Cleared name colour for " + playerName);
            }
            case "all" -> {
                data.clearAll();
                sendSuccess(sender, "Cleared all customizations for " + playerName);
            }
            default -> {
                sendError(sender, "Invalid type. Use: prefix, suffix, nickname, namecolour, all");
                return;
            }
        }
        
        // Save and update
        plugin.getDataCache().savePlayerData(data);
        
        send(target, "<yellow>⚠ Your " + dataType + " has been cleared by an admin");
    }
    
    /**
     * Show player info
     */
    private void showPlayerInfo(@NotNull CommandSender sender, @NotNull String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        
        if (target == null) {
            sendError(sender, "Player not found");
            return;
        }
        
        PlayerCustomization data = plugin.getAPI().getPlayerData(target);
        String rank = plugin.getLuckPermsHook() != null ? 
            plugin.getLuckPermsHook().getPrimaryGroup(target) : "unknown";
        
        send(sender, "<gold>==== <white>" + playerName + " <gold>====");
        send(sender, "<yellow>Rank: <white>" + rank);
        
        if (data != null) {
            send(sender, "<yellow>Nickname: <white>" + (data.getNickname() != null ? data.getNickname() : "none"));
            send(sender, "<yellow>Colour: <white>" + (data.getNameColour() != null ? data.getNameColour() : "none"));
            send(sender, "<yellow>Prefix: <white>" + (data.getPrefix() != null ? data.getPrefix() : "none"));
            send(sender, "<yellow>Suffix: <white>" + (data.getSuffix() != null ? data.getSuffix() : "none"));
            send(sender, "<yellow>Has customizations: <white>" + data.hasCustomizations());
        } else {
            send(sender, "<gray>No customization data");
        }
    }
    
    /**
     * Show cache info
     */
    private void showCacheInfo(@NotNull CommandSender sender) {
        int cached = plugin.getDataCache().getCacheSize();
        int online = Bukkit.getOnlinePlayers().size();
        
        send(sender, "<gold>==== <white>Cache Statistics <gold>====");
        send(sender, "<yellow>Cached players: <white>" + cached);
        send(sender, "<yellow>Online players: <white>" + online);
        send(sender, "<yellow>Hit rate: <white>" + String.format("%.1f%%", plugin.getDataCache().getHitRate()));
        send(sender, "<yellow>Hits: <white>" + plugin.getDataCache().getHits());
        send(sender, "<yellow>Misses: <white>" + plugin.getDataCache().getMisses());
        send(sender, "<yellow>Evictions: <white>" + plugin.getDataCache().getEvictions());
    }
    
    /**
     * Clear the cache
     */
    private void clearCache(@NotNull CommandSender sender) {
        send(sender, "<yellow>Clearing cache...");
        
        plugin.getDataCache().clearCache();
        sendSuccess(sender, "Cache cleared");
        
        // Reload online players
        for (Player online : Bukkit.getOnlinePlayers()) {
            plugin.getDataCache().loadPlayer(online.getUniqueId());
        }
        
        send(sender, "<gray>Reloaded " + Bukkit.getOnlinePlayers().size() + " online players");
    }
}