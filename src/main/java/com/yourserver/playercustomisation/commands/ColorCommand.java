package com.yourserver.playercustomisation.commands;

import com.yourserver.playercustomisation.PlayerCustomisation;
import com.yourserver.playercustomisation.utils.ColorUtils;
import com.yourserver.playercustomisation.utils.PermissionUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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

        // Check if player has permission
        if (!player.hasPermission("playercustomisation.color")) {
            String message = plugin.getConfig().getString("messages.no-permission", "&cYou don't have permission to use this command!");
            player.sendMessage(ColorUtils.colorize(message));
            return true;
        }

        // Check rank access
        String rank = PermissionUtils.getPlayerRank(player);
        if (!PermissionUtils.hasColorAccess(rank)) {
            String message = plugin.getConfig().getString("messages.no-permission", "&cYou don't have permission to use this command!");
            player.sendMessage(ColorUtils.colorize(message));
            return true;
        }

        // Open DeluxeMenus color GUI based on rank
        if (PermissionUtils.hasGradientAccess(rank)) {
            // Patron or Devoted - open enhanced menu
            player.performCommand("dm open color_patron");
        } else {
            // Supporter - open basic menu
            player.performCommand("dm open color_supporter");
        }
        return true;
    }
}