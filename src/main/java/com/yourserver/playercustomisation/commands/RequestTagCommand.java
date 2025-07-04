package com.yourserver.playercustomisation.commands;

import com.yourserver.playercustomisation.PlayerCustomisation;
import com.yourserver.playercustomisation.models.TagRequest;
import com.yourserver.playercustomisation.utils.ColorUtils;
import com.yourserver.playercustomisation.utils.PermissionUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RequestTagCommand implements CommandExecutor {
    private final PlayerCustomisation plugin;

    public RequestTagCommand(PlayerCustomisation plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        // Check if player has permission
        if (!player.hasPermission("playercustomisation.customtag")) {
            String message = plugin.getConfig().getString("messages.no-permission", "&cYou don't have permission to use this command!");
            player.sendMessage(ColorUtils.colorize(message));
            return true;
        }

        // Check rank access
        String rank = PermissionUtils.getPlayerRank(player);
        if (!PermissionUtils.hasCustomTagAccess(rank)) {
            String message = plugin.getConfig().getString("messages.no-permission", "&cYou don't have permission to use this command!");
            player.sendMessage(ColorUtils.colorize(message));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ColorUtils.colorize("&cUsage: /requesttag <tag>"));
            return true;
        }

        String requestedTag = String.join(" ", args);

        // Basic validation
        if (requestedTag.length() > 20 || requestedTag.length() < 2) {
            player.sendMessage(ColorUtils.colorize("&cTag must be between 2-20 characters!"));
            return true;
        }

        // Create tag request
        TagRequest request = new TagRequest(player.getUniqueId(), player.getName(), requestedTag);
        plugin.getPlayerDataManager().createTagRequest(request).thenRun(() -> {
            String message = plugin.getConfig().getString("messages.tag-requested", "&aYour custom tag request has been submitted for review!");
            player.sendMessage(ColorUtils.colorize(message));
        });

        return true;
    }
}