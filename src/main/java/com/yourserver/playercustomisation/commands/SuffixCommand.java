package com.yourserver.playercustomisation.commands;

import com.yourserver.playercustomisation.PlayerCustomisation;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * New command for suffix selection
 */
public class SuffixCommand implements CommandExecutor {
    private final PlayerCustomisation plugin;

    public SuffixCommand(PlayerCustomisation plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        // Open the suffix selection menu using our GUI system
        plugin.getMenuManager().openSuffixMenu(player);
        
        return true;
    }
}