package com.prefix27.customization.commands;

import com.prefix27.customization.PlayerCustomizationPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SimplePrefixCommand implements CommandExecutor {
    
    private final PlayerCustomizationPlugin plugin;
    
    public SimplePrefixCommand(PlayerCustomizationPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Handle subcommands
        if (args.length > 0 && args[0].equalsIgnoreCase("request")) {
            handleCustomPrefixRequest(player);
            return true;
        }
        
        // Open the prefix GUI
        plugin.getSimpleGUIManager().openPrefixGUI(player);
        return true;
    }
    
    private void handleCustomPrefixRequest(Player player) {
        if (!plugin.getPlayerDataManager().canUseCustomPrefix(player.getUniqueId())) {
            player.sendMessage("§cYou need Devoted rank to request custom prefixes!");
            return;
        }
        
        // Start the custom prefix request process
        plugin.getChatInputManager().requestCustomPrefixInput(player);
    }
}