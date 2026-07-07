package dev.hxrry.hxprefix.commands;

import dev.hxrry.hxcore.commands.HxCommand;
import static dev.hxrry.hxcore.commands.HxCommand.*;
import static dev.hxrry.hxcore.commands.HxCommand.PermDefault.*;
import dev.hxrry.hxcore.utils.Log;

import dev.hxrry.hxprefix.HxPrefix;
import dev.hxrry.hxprefix.api.models.PlayerCustomization;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Admin command for managing the plugin
 */
public class AdminCommand extends BaseCommand {
    
    public AdminCommand(@NotNull HxPrefix plugin) {
        super(plugin, "hxprefix", "hxprefix.admin", false); // console allowed
    }
    
    public void register(HxPrefix plugin) {
        var admin = perm("hxprefix.admin", OP);
        var playerArg = arg("player", sender -> onlineNames());
        
        HxCommand.create("hxprefix")
            .permission(admin)

            .executes(sender -> showHelp(sender))
            
                .sub("reload", admin, sender -> reloadPlugin(sender))
                
                .sub("info", admin, playerArg,
                    (sender, player) -> showPlayerInfo(sender, player))
                
                .sub("setprefix", admin, args(playerArg, greedyArg("value")),
                    (sender, bag) -> setPlayerData(sender, bag.get("player"), "prefix", bag.get("value")))

                .sub("clearprefix", admin, playerArg,
                    (sender, player) -> clearPlayerData(sender, player, "prefix"))

                .sub("setsuffix", admin, args(playerArg, greedyArg("value")),
                    (sender, bag) -> setPlayerData(sender, bag.get("player"), "suffix", bag.get("value")))

                .sub("clearsuffix", admin, playerArg,
                    (sender, player) -> clearPlayerData(sender, player, "suffix"))

                .sub("setnick", admin, args(playerArg, greedyArg("value")),
                    (sender, bag) -> setPlayerData(sender, bag.get("player"), "nickname", bag.get("value")))

                .sub("clearnick", admin, playerArg,
                    (sender, player) -> clearPlayerData(sender, player, "nickname"))

                .sub("setcolour", admin, args(playerArg, greedyArg("value")),
                    (sender, bag) -> setPlayerData(sender, bag.get("player"), "namecolour", bag.get("value")))

                .sub("clearcolour", admin, playerArg,
                    (sender, player) -> clearPlayerData(sender, player, "namecolour"))

                .sub("clearall", admin, playerArg,
                    (sender, player) -> clearPlayerData(sender, player, "all"))

                .register(plugin);
    }
    
    /**
     * Show admin help
     */
    private void showHelp(@NotNull CommandSender sender) {
        send(sender, "<gold>==== <white>HxPrefix Admin <gold>====");
        send(sender, "<yellow>/hxprefix reload <gray>- reload configuration");
        send(sender, "");
        send(sender, "<yellow>/hxprefix setprefix <player> <text> <gray>- set prefix");
        send(sender, "<yellow>/hxprefix setsuffix <player> <text> <gray>- set suffix");
        send(sender, "<yellow>/hxprefix setnick <player> <text> <gray>- set nickname");
        send(sender, "<yellow>/hxprefix setcolour <player> <text> <gray>- set name colour");
        send(sender, "");
        send(sender, "<yellow>/hxprefix clearprefix|clearsuffix|clearnick|clearcolour <player>");
        send(sender, "<yellow>/hxprefix clearall <player> <gray>- clear everything");
        send(sender, "");
        send(sender, "<yellow>/hxprefix info <player> <gray>- view player data");
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

    @SuppressWarnings("null")
    private List<String> onlineNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
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
}