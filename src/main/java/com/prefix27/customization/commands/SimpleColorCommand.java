package com.prefix27.customization.commands;

import com.prefix27.customization.PlayerCustomizationPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SimpleColorCommand implements CommandExecutor {
    
    private final PlayerCustomizationPlugin plugin;
    
    public SimpleColorCommand(PlayerCustomizationPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check if they have permission
        if (!plugin.getPlayerDataManager().canUseColorFeature(player.getUniqueId())) {
            player.sendMessage("§cYou need Supporter rank or higher to use name colors!");
            return true;
        }
        
        // Open the color GUI
        plugin.getSimpleGUIManager().openColorGUI(player);
        return true;
    }
}