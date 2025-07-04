package com.prefix27.customization.commands;

import com.prefix27.customization.PlayerCustomizationPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GradientCommand implements CommandExecutor {
    
    private final PlayerCustomizationPlugin plugin;
    
    public GradientCommand(PlayerCustomizationPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("customization.patron")) {
            player.sendMessage("§cYou need Patron rank or higher to use gradient colors!");
            return true;
        }
        
        // Open the gradient builder GUI
        plugin.getGUIManager().openGradientBuilderGUI(player);
        
        return true;
    }
}