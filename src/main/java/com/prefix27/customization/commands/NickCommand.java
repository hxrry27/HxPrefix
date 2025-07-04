package com.prefix27.customization.commands;

import com.prefix27.customization.PlayerCustomizationPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class NickCommand implements CommandExecutor, TabCompleter {
    
    private final PlayerCustomizationPlugin plugin;
    
    public NickCommand(PlayerCustomizationPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("customization.nick")) {
            player.sendMessage("§cYou don't have permission to set nicknames!");
            return true;
        }
        
        if (args.length == 0) {
            player.sendMessage("§cUsage: /nick <name> or /nick reset");
            return true;
        }
        
        if (args[0].equalsIgnoreCase("reset")) {
            plugin.getPlayerDataManager().setNickname(player.getUniqueId(), null);
            player.sendMessage("§aYour nickname has been reset!");
            return true;
        }
        
        String nickname = String.join(" ", args);
        
        // Validate nickname
        if (nickname.length() > plugin.getConfig().getInt("nicknames.max_length", 16)) {
            player.sendMessage("§cNickname is too long! Maximum length is " + 
                plugin.getConfig().getInt("nicknames.max_length", 16) + " characters.");
            return true;
        }
        
        if (nickname.length() < plugin.getConfig().getInt("nicknames.min_length", 3)) {
            player.sendMessage("§cNickname is too short! Minimum length is " + 
                plugin.getConfig().getInt("nicknames.min_length", 3) + " characters.");
            return true;
        }
        
        // Set nickname
        plugin.getPlayerDataManager().setNickname(player.getUniqueId(), nickname);
        player.sendMessage("§aYour nickname has been set to: " + nickname);
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("reset");
        }
        
        return completions;
    }
}