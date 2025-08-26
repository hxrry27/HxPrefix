package dev.hxrry.hxprefix.commands;

import dev.hxrry.hxprefix.HxPrefix;
import dev.hxrry.hxprefix.config.MessagesConfig;

import io.papermc.paper.command.brigadier.Commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * base class for all hxprefix commands
 */
public abstract class BaseCommand {
    protected final HxPrefix plugin;
    protected final String name;
    protected final String permission;
    protected final boolean playerOnly;
    protected final MiniMessage mm = MiniMessage.miniMessage();
    
    protected BaseCommand(@NotNull HxPrefix plugin, @NotNull String name, 
                         @Nullable String permission, boolean playerOnly) {
        this.plugin = plugin;
        this.name = name;
        this.permission = permission;
        this.playerOnly = playerOnly;
    }
    
    /**
     * register this command with paper
     */
    public abstract void register(@NotNull Commands commands);
    
    /**
     * get the command name
     */
    @NotNull
    public String getName() {
        return name;
    }
    
    /**
     * get the command permission
     */
    @Nullable
    public String getPermission() {
        return permission;
    }
    
    /**
     * check if sender has permission
     */
    protected boolean hasPermission(@NotNull CommandSender sender) {
        if (permission == null) return true;
        return sender.hasPermission(permission);
    }
    
    /**
     * check if sender is a player (for player-only commands)
     */
    protected boolean isPlayer(@NotNull CommandSender sender) {
        return sender instanceof Player;
    }
    
    /**
     * send a message to the sender
     */
    protected void send(@NotNull CommandSender sender, @NotNull String message) {
        sender.sendMessage(mm.deserialize(message));
    }
    
    /**
     * send a component to the sender
     */
    protected void send(@NotNull CommandSender sender, @NotNull Component component) {
        sender.sendMessage(component);
    }
    
    /**
     * send a config message to the sender
     */
    protected void sendMessage(@NotNull CommandSender sender, @NotNull String key, String... replacements) {
        MessagesConfig messages = plugin.getConfigManager().getMessagesConfig();
        String message = messages.getMessage(key, replacements);
        send(sender, message);
    }
    
    /**
     * send an error message
     */
    protected void sendError(@NotNull CommandSender sender, @NotNull String message) {
        send(sender, "<red>✗ " + message);
    }
    
    /**
     * send a success message
     */
    protected void sendSuccess(@NotNull CommandSender sender, @NotNull String message) {
        send(sender, "<green>✓ " + message);
    }
    
    /**
     * send no permission message
     */
    protected void sendNoPermission(@NotNull CommandSender sender) {
        sendMessage(sender, "error.no-permission");
    }
    
    /**
     * send player only message
     */
    protected void sendPlayerOnly(@NotNull CommandSender sender) {
        sendMessage(sender, "error.player-only");
    }
    
    /**
     * check permission and player status
     * @return true if checks pass
     */
    protected boolean checkSender(@NotNull CommandSender sender) {
        // check permission first
        if (!hasPermission(sender)) {
            sendNoPermission(sender);
            return false;
        }
        
        // check if player-only
        if (playerOnly && !isPlayer(sender)) {
            sendPlayerOnly(sender);
            return false;
        }
        
        return true;
    }
    
    /**
     * get tab completion suggestions
     */
    @NotNull
    protected List<String> getTabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return List.of(); // default no suggestions
    }
    
    /**
     * reload permissions for this command
     */
    public void reloadPermissions() {
        // override in subclasses if needed
    }
    
    /**
     * get the player from sender (assumes already checked)
     */
    @NotNull
    protected Player getPlayer(@NotNull CommandSender sender) {
        return (Player) sender;
    }
    
    /**
     * check if a player has a specific feature permission
     */
    protected boolean hasFeaturePermission(@NotNull Player player, @NotNull String feature) {
        return plugin.getConfigManager().getPermissionConfig()
            .hasPermission(player, feature);
    }
    
    /**
     * get player's rank
     */
    @NotNull
    protected String getPlayerRank(@NotNull Player player) {
        if (plugin.getLuckPermsHook() != null) {
            return plugin.getLuckPermsHook().getPrimaryGroup(player);
        }
        return "default";
    }
    
    /**
     * format a message with the plugin prefix
     */
    @NotNull
    protected String withPrefix(@NotNull String message) {
        String prefix = plugin.getConfigManager().getMessagesConfig().getPrefix();
        return prefix + message;
    }
    
    /**
     * check if a string is a valid colour code
     */
    protected boolean isValidColour(@NotNull String input) {
        // check if it's a valid hex colour
        if (input.matches("^#[A-Fa-f0-9]{6}$")) {
            return true;
        }
        
        // check if it's a named colour
        return input.matches("^(red|blue|green|yellow|gold|aqua|pink|gray|grey|white|black)$");
    }
    
    /**
     * check if a string is a valid nickname
     */
    protected boolean isValidNickname(@NotNull String input) {
        // use config pattern
        String pattern = plugin.getConfigManager().getMainConfig()
            .getString("nickname.pattern", "^[a-zA-Z0-9_]{3,16}$");
        return input.matches(pattern);
    }
}