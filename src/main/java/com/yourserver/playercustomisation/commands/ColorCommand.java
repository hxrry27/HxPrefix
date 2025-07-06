package com.yourserver.playercustomisation.commands;

import com.yourserver.playercustomisation.PlayerCustomisation;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Updated ColorCommand that uses the internal GUI system
 * No longer depends on DeluxeMenus
 */
public class ColorCommand implements CommandExecutor {
    private final PlayerCustomisation plugin;

    public ColorCommand(PlayerCustomisation plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        // Open the color selection menu using our GUI system
        plugin.getMenuManager().openColorMenu(player);
        
        return true;
    }
}