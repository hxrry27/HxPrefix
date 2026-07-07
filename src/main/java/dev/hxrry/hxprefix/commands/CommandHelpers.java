package dev.hxrry.hxprefix.commands;

import dev.hxrry.hxprefix.HxPrefix;
import dev.hxrry.hxprefix.config.MessagesConfig;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;

public abstract class CommandHelpers {
    protected final HxPrefix plugin;
    protected final MiniMessage mm = MiniMessage.miniMessage();
    
    protected CommandHelpers(@NotNull HxPrefix plugin) {
        this.plugin = plugin;
    }
    
    //message
    protected void send(@NotNull CommandSender sender, @NotNull String message) {
        sender.sendMessage(mm.deserialize(message));
    }
    
    //component
    protected void send(@NotNull CommandSender sender, @NotNull Component component) {
        sender.sendMessage(component);
    }
    
    //config message
    protected void sendMessage(@NotNull CommandSender sender, @NotNull String key, String... replacements) {
        MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();
        String message = messages.getMessage(key, replacements);
        send(sender, message);
    }
    
    protected void sendError(@NotNull CommandSender sender, @NotNull String message) {
        send(sender, "<red>✗ " + message);
    }
    
    protected void sendSuccess(@NotNull CommandSender sender, @NotNull String message) {
        send(sender, "<green>✓ " + message);
    }
    
    protected void sendPlayerOnly(@NotNull CommandSender sender) {
        sendMessage(sender, "error.player-only");
    }
    
    protected boolean hasFeaturePermission(@NotNull Player player, @NotNull String feature) {
        return plugin.getConfigManager().getPermissionConfig()
            .hasPermission(player, feature);
    }
    
    @NotNull
    protected String getPlayerRank(@NotNull Player player) {
        if (plugin.getLuckPermsHook() != null) {
            return plugin.getLuckPermsHook().getPrimaryGroup(player);
        }
        return "default";
    }
    
    protected boolean isValidNickname(@NotNull String input) {
        // use config pattern
        String pattern = plugin.getConfigManager().getMainConfig()
            .getString("nickname.pattern", "^[a-zA-Z0-9_]{3,16}$");
        return input.matches(pattern);
    }
}