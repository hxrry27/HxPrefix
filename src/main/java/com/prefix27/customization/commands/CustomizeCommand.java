package com.prefix27.customization.commands;

import com.prefix27.customization.PlayerCustomizationPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CustomizeCommand implements CommandExecutor, TabCompleter {
    
    private final PlayerCustomizationPlugin plugin;
    
    public CustomizeCommand(PlayerCustomizationPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("customization.use")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }
        
        // Open the main customization GUI
        plugin.getGUIManager().openMainCustomizationGUI(player);
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}