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
            handleCustomPrefixRequest(player, args);
            return true;
        }
        
        // Open the prefix GUI
        plugin.getSimpleGUIManager().openPrefixGUI(player);
        return true;
    }
    
    private void handleCustomPrefixRequest(Player player, String[] args) {
        if (!plugin.getPlayerDataManager().canUseCustomPrefix(player.getUniqueId())) {
            player.sendMessage("§cYou need Devoted rank to request custom prefixes!");
            return;
        }
        
        // Check if we have the 3 required arguments: /prefix request LOYAL #FF0000 #DR9889
        if (args.length != 4) {
            player.sendMessage("§cUsage: /prefix request <prefix_text> <start_color> <end_color>");
            player.sendMessage("§cExample: /prefix request LOYAL #FF0000 #00FF00");
            return;
        }
        
        String prefixText = args[1];
        String startColor = args[2];
        String endColor = args[3];
        
        // Validate prefix text
        if (prefixText.length() > 16 || !prefixText.matches("[A-Z0-9]+")) {
            player.sendMessage("§cPrefix must be uppercase letters/numbers only and max 16 characters!");
            return;
        }
        
        // Validate colors (basic hex validation)
        if (!isValidHexColor(startColor) || !isValidHexColor(endColor)) {
            player.sendMessage("§cColors must be valid hex codes (e.g., #FF0000)!");
            return;
        }
        
        // Submit the request
        String requestData = String.format("%s|%s|%s", prefixText, startColor, endColor);
        plugin.getDatabaseManager().insertCustomPrefixRequest(player.getUniqueId(), requestData);
        
        player.sendMessage("§aCustom prefix request submitted!");
        player.sendMessage("§7Prefix: §b" + prefixText + " §7with gradient from §b" + startColor + " §7to §b" + endColor);
        player.sendMessage("§7Staff will review your request soon.");
    }
    
    private boolean isValidHexColor(String color) {
        return color.matches("^#[0-9A-Fa-f]{6}$");
    }
}