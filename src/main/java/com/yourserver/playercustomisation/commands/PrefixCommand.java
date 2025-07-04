package com.yourserver.playercustomisation.commands;

import com.yourserver.playercustomisation.PlayerCustomisation;
import com.yourserver.playercustomisation.utils.ColorUtils;
import com.yourserver.playercustomisation.utils.PermissionUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PrefixCommand implements CommandExecutor {
    private final PlayerCustomisation plugin;

    public PrefixCommand(PlayerCustomisation plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        // Check if player has permission
        if (!player.hasPermission("playercustomisation.prefix")) {
            String message = plugin.getConfig().getString("messages.no-permission", "&cYou don't have permission to use this command!");
            player.sendMessage(ColorUtils.colorize(message));
            return true;
        }

        // Check rank access
        String rank = PermissionUtils.getPlayerRank(player);
        if (!PermissionUtils.hasPrefixAccess(rank)) {
            String message = plugin.getConfig().getString("messages.no-permission", "&cYou don't have permission to use this command!");
            player.sendMessage(ColorUtils.colorize(message));
            return true;
        }

        // Open DeluxeMenus prefix GUI based on rank
        switch (rank.toLowerCase()) {
            case "supporter":
                player.performCommand("dm open prefix_supporter");
                break;
            case "patron":
                player.performCommand("dm open prefix_patron");
                break;
            case "devoted":
                player.performCommand("dm open prefix_devoted");
                break;
            default:
                player.sendMessage(ColorUtils.colorize("&cYour rank doesn't have access to prefixes!"));
                break;
        }
        return true;
    }
}