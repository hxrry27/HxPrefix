package com.prefix27.customization.commands;

import com.prefix27.customization.PlayerCustomizationPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CustomizationAdminCommand implements CommandExecutor, TabCompleter {
    
    private final PlayerCustomizationPlugin plugin;
    
    public CustomizationAdminCommand(PlayerCustomizationPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("customization.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }
        
        if (args.length == 0) {
            sender.sendMessage("§cUsage: /customization <admin|reload|reset>");
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "admin":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cOnly players can use the admin GUI!");
                    return true;
                }
                
                Player player = (Player) sender;
                plugin.getGUIManager().openAdminGUI(player);
                break;
                
            case "reload":
                plugin.reloadPluginConfig();
                sender.sendMessage("§aPlugin configuration reloaded!");
                break;
                
            case "reset":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /customization reset <player>");
                    return true;
                }
                
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("§cPlayer not found!");
                    return true;
                }
                
                plugin.getPlayerDataManager().resetPlayerData(target.getUniqueId());
                sender.sendMessage("§aReset customization data for " + target.getName());
                target.sendMessage("§aYour customization data has been reset by an administrator.");
                break;
                
            default:
                sender.sendMessage("§cUsage: /customization <admin|reload|reset>");
                break;
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("admin", "reload", "reset"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
            // Add online player names
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        }
        
        return completions;
    }
}