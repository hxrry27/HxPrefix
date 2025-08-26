package dev.hxrry.hxprefix.commands;

import dev.hxrry.hxprefix.HxPrefix;
import dev.hxrry.hxprefix.api.models.CustomTagRequest;

import dev.hxrry.hxcore.utils.Scheduler;

import io.papermc.paper.command.brigadier.Commands;

import com.mojang.brigadier.arguments.StringArgumentType;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * command for requesting custom tags/prefixes
 */
public class TagCommand extends BaseCommand {
    
    public TagCommand(@NotNull HxPrefix plugin) {
        super(plugin, "tag", null, true);
    }
    
    @Override
    public void register(@NotNull Commands commands) {
        commands.register(
            Commands.literal(name)
                .executes(ctx -> {
                    // show status with no args
                    CommandSender sender = ctx.getSource().getSender();
                    if (!checkSender(sender)) return 0;
                    
                    Player player = getPlayer(sender);
                    if (!checkTagPermission(player)) return 0;
                    
                    showTagStatus(player);
                    return 1;
                })
                .then(Commands.argument("tag", StringArgumentType.greedyString())
                    .suggests((ctx, builder) -> {
                        // suggest some examples
                        builder.suggest("LEGEND");
                        builder.suggest("CHAMPION");
                        builder.suggest("ELITE");
                        builder.suggest("MASTER");
                        return builder.buildFuture();
                    })
                    .executes(ctx -> {
                        CommandSender sender = ctx.getSource().getSender();
                        if (!checkSender(sender)) return 0;
                        
                        Player player = getPlayer(sender);
                        if (!checkTagPermission(player)) return 0;
                        
                        String tag = ctx.getArgument("tag", String.class);
                        
                        // special case - cancel request
                        if (tag.equalsIgnoreCase("cancel")) {
                            cancelRequest(player);
                            return 1;
                        }
                        
                        // submit request
                        submitRequest(player, tag);
                        return 1;
                    })
                )
                .build()
        );
    }
    
    /**
     * check if player has permission to request custom tags
     */
    private boolean checkTagPermission(@NotNull Player player) {
        if (!hasFeaturePermission(player, "custom-tags")) {
            sendMessage(player, "error.no-tag-permission", 
                "{rank}", getPlayerRank(player));
            send(player, "<gray>custom tags are available for devoted rank and above");
            return false;
        }
        return true;
    }
    
    /**
     * show current tag request status
     */
    private void showTagStatus(@NotNull Player player) {
        // check for pending request async
        CompletableFuture.supplyAsync(() -> 
            plugin.getDatabaseManager().getPendingTagRequest(player.getUniqueId())
        ).thenAccept(request -> {
            if (request != null) {
                sendMessage(player, "tag.status.pending",
                    "{tag}", request.getRequestedTag(),
                    "{days}", String.valueOf(request.getAgeInDays()));
                
                // show how to cancel
                send(player, "<gray>use /hxtag cancel to cancel your request");
            } else {
                // check if they have an approved custom tag
                String currentPrefix = plugin.getAPI().getPrefix(player);
                if (currentPrefix != null && isCustomTag(currentPrefix)) {
                    sendMessage(player, "tag.status.active",
                        "{tag}", currentPrefix);
                } else {
                    sendMessage(player, "tag.status.none");
                    send(player, "<gray>use /hxtag <name> to request a custom tag");
                }
            }
        });
    }
    
    /**
     * submit a tag request
     */
    private void submitRequest(@NotNull Player player, @NotNull String tag) {
        // validate tag
        if (!validateTag(player, tag)) {
            return;
        }
        
        // check for existing request
        CompletableFuture.supplyAsync(() -> 
            plugin.getDatabaseManager().getPendingTagRequest(player.getUniqueId())
        ).thenAccept(existing -> {
            if (existing != null) {
                sendError(player, "you already have a pending request: " + existing.getRequestedTag());
                send(player, "<gray>use /hxtag cancel to cancel it first");
                return;
            }
            
            // check cooldown for denied requests
            CompletableFuture.supplyAsync(() ->
                plugin.getDatabaseManager().getLastDeniedRequest(player.getUniqueId())
            ).thenAccept(lastDenied -> {
                if (lastDenied != null) {
                    int cooldownDays = plugin.getConfigManager().getMainConfig()
                        .getInt("tags.denied-cooldown-days", 7);
                    
                    int daysSinceDenied = lastDenied.getAgeInDays();
                    if (daysSinceDenied < cooldownDays) {
                        int remaining = cooldownDays - daysSinceDenied;
                        sendError(player, "please wait " + remaining + " more days before requesting another tag");
                        send(player, "<gray>your last request was denied " + daysSinceDenied + " days ago");
                        return;
                    }
                }
                
                // create the request
                CustomTagRequest request = new CustomTagRequest(
                    player.getUniqueId(),
                    player.getName(),
                    tag
                );
                
                // save to database
                CompletableFuture.runAsync(() -> {
                    if (plugin.getDatabaseManager().createTagRequest(request)) {
                        Scheduler.runTask(() -> {
                            sendSuccess(player, "tag request submitted: " + tag);
                            sendMessage(player, "tag.submitted",
                                "{tag}", tag);
                            
                            // notify staff
                            notifyStaff(player, tag);
                        });
                    } else {
                        Scheduler.runTask(() ->  
                            sendError(player, "failed to submit request - please try again")
                        );
                    }
                });
            });
        });
    }
    
    /**
     * cancel pending request
     */
    private void cancelRequest(@NotNull Player player) {
        CompletableFuture.supplyAsync(() ->
            plugin.getDatabaseManager().getPendingTagRequest(player.getUniqueId())
        ).thenAccept(request -> {
            if (request == null) {
                sendError(player, "you don't have a pending request");
                return;
            }
            
            // cancel it
            CompletableFuture.runAsync(() -> {
                if (plugin.getDatabaseManager().cancelTagRequest(request.getId())) {
                    Scheduler.runTask(() -> {
                        sendSuccess(player, "tag request cancelled");
                        sendMessage(player, "tag.cancelled",
                            "{tag}", request.getRequestedTag());
                    });
                } else {
                    Scheduler.runTask(() -> 
                        sendError(player, "failed to cancel request")
                    );
                }
            });
        });
    }
    
    /**
     * validate a tag name
     */
    private boolean validateTag(@NotNull Player player, @NotNull String tag) {
        // length check
        int minLength = plugin.getConfigManager().getMainConfig()
            .getInt("tags.min-length", 2);
        int maxLength = plugin.getConfigManager().getMainConfig()
            .getInt("tags.max-length", 12);
        
        if (tag.length() < minLength || tag.length() > maxLength) {
            sendError(player, "tag must be " + minLength + "-" + maxLength + " characters");
            return false;
        }
        
        // format check - allow letters, numbers, spaces
        if (!tag.matches("^[a-zA-Z0-9 ]+$")) {
            sendError(player, "tag can only contain letters, numbers, and spaces");
            return false;
        }
        
        // check blocked words
        String lower = tag.toLowerCase();
        for (String blocked : plugin.getConfigManager().getMainConfig()
                .getStringList("tags.blocked-words")) {
            if (lower.contains(blocked.toLowerCase())) {
                sendError(player, "that tag contains blocked content");
                return false;
            }
        }
        
        // check if it matches existing rank names
        if (isExistingRank(tag)) {
            sendError(player, "that tag matches an existing rank name");
            return false;
        }
        
        return true;
    }
    
    /**
     * check if tag matches an existing rank
     */
    private boolean isExistingRank(@NotNull String tag) {
        String lower = tag.toLowerCase().replace(" ", "");
        for (String rank : plugin.getConfigManager().getPermissionConfig().getRankNames()) {
            if (rank.toLowerCase().replace(" ", "").equals(lower)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * check if a prefix is a custom tag
     */
    private boolean isCustomTag(@NotNull String prefix) {
        // custom tags are not in the standard prefix list
        return plugin.getConfigManager().getStyleConfig()
            .getAvailablePrefixes("devoted").stream()
            .noneMatch(opt -> opt.getValue().equals(prefix));
    }
    
    /**
     * notify staff of new tag request
     */
    private void notifyStaff(@NotNull Player requester, @NotNull String tag) {
        String message = "<gold>⚡ <yellow>New tag request from <white>" + 
                        requester.getName() + "<yellow>: <white>" + tag;
        
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (online.hasPermission("hxprefix.admin.tags")) {
                send(online, message);
                send(online, "<gray>use /hxadmin tags to review");
            }
        }
    }
}