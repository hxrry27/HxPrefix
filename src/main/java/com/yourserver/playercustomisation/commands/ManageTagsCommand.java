package com.yourserver.playercustomisation.commands;

import com.yourserver.playercustomisation.PlayerCustomisation;
import com.yourserver.playercustomisation.models.TagRequest;
import com.yourserver.playercustomisation.utils.ColorUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;


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
                sender.sendMessage(plugin.getConfigManager().getMessage("tags.no-pending"));
                return;
            }

            sender.sendMessage(plugin.getConfigManager().getMessage("tags.pending-header"));
            for (TagRequest request : requests) {
                String format = plugin.getConfigManager().getMessage("tags.pending-format");
                format = format.replace("{id}", String.valueOf(request.getId()))
                            .replace("{player}", request.getUsername())
                            .replace("{tag}", request.getRequestedTag());
                sender.sendMessage(format);
            }
            sender.sendMessage(plugin.getConfigManager().getMessage("tags.pending-footer"));
        });

        return true;
    }
}