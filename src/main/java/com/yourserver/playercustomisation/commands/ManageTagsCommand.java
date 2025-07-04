package com.yourserver.playercustomisation.commands;

import com.yourserver.playercustomisation.PlayerCustomisation;
import com.yourserver.playercustomisation.models.TagRequest;
import com.yourserver.playercustomisation.utils.ColorUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class ManageTagsCommand implements CommandExecutor {
    private final PlayerCustomisation plugin;

    public ManageTagsCommand(PlayerCustomisation plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("playercustomisation.admin.tags")) {
            String message = plugin.getConfig().getString("messages.no-permission", "&cYou don't have permission to use this command!");
            sender.sendMessage(ColorUtils.colorize(message));
            return true;
        }

        // For now, just list pending requests in chat
        // In a full implementation, this would open a GUI
        plugin.getPlayerDataManager().getPendingTagRequests().thenAccept(requests -> {
            if (requests.isEmpty()) {
                sender.sendMessage(ColorUtils.colorize("&aNo pending tag requests."));
                return;
            }

            sender.sendMessage(ColorUtils.colorize("&6&l=== Pending Tag Requests ==="));
            for (TagRequest request : requests) {
                sender.sendMessage(ColorUtils.colorize(String.format(
                    "&7[%d] &b%s &7requested: &f%s",
                    request.getId(),
                    request.getUsername(),
                    request.getRequestedTag()
                )));
            }
            sender.sendMessage(ColorUtils.colorize("&7Use a proper admin GUI to approve/deny these requests."));
        });

        return true;
    }
}