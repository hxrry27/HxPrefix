package com.prefix27.customization.commands;

import com.prefix27.customization.PlayerCustomizationPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ColorCommand implements CommandExecutor {
    
    private final PlayerCustomizationPlugin plugin;
    
    public ColorCommand(PlayerCustomizationPlugin plugin) {
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
        
        // Open the color selection GUI
        plugin.getGUIManager().openColorSelectionGUI(player);
        
        return true;
    }
}