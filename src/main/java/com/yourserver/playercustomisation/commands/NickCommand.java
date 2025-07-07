package com.yourserver.playercustomisation.commands;

import com.yourserver.playercustomisation.PlayerCustomisation;
import com.yourserver.playercustomisation.models.PlayerData;
import com.yourserver.playercustomisation.utils.ColorUtils;
import com.yourserver.playercustomisation.utils.PermissionUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class NickCommand implements CommandExecutor {
    private final PlayerCustomisation plugin;

    public NickCommand(PlayerCustomisation plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        // Check if player has permission
        if (!player.hasPermission("playercustomisation.nick")) {
            String message = plugin.getConfig().getString("messages.no-permission", "&cYou don't have permission to use this command!");
            player.sendMessage(ColorUtils.colorize(message));
            return true;
        }

        // Check rank access
        String rank = PermissionUtils.getPlayerRank(player);
        if (!PermissionUtils.hasNickAccess(rank)) {
            String message = plugin.getConfig().getString("messages.no-permission", "&cYou don't have permission to use this command!");
            player.sendMessage(ColorUtils.colorize(message));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ColorUtils.colorize("&cUsage: /nick <nickname|off>"));
            return true;
        }

        String nickname = args[0];

        // Handle reset
        if (nickname.equalsIgnoreCase("off") || nickname.equalsIgnoreCase("reset")) {
            plugin.getPlayerDataManager().getPlayerData(player.getUniqueId())
                .thenAccept(data -> {
                    if (data != null) {
                        data.setNickname(null);
                        plugin.getPlayerDataManager().savePlayerData(data).thenRun(() -> {
                            String message = plugin.getConfig().getString("messages.nickname-reset", "&aYour nickname has been reset!");
                            player.sendMessage(ColorUtils.colorize(message));
                            plugin.getNametagManager().updateNametag(player);
                        });
                    }
                });
            return true;
        }

        // Validate nickname
        if (!ColorUtils.isValidNickname(nickname)) {
            String message = plugin.getConfig().getString("messages.invalid-nickname", "&cNickname must be 3-16 characters and alphanumeric!");
            player.sendMessage(ColorUtils.colorize(message));
            return true;
        }

        // Set nickname
        plugin.getPlayerDataManager().getPlayerData(player.getUniqueId())
            .thenAccept(data -> {
                if (data == null) {
                    data = new PlayerData(player.getUniqueId(), player.getName());
                }
                data.setNickname(nickname);
                plugin.getPlayerDataManager().savePlayerData(data).thenRun(() -> {
                    String message = plugin.getConfig().getString("messages.nickname-changed", "&aYour nickname has been changed to: &f%nickname%");
                    message = message.replace("%nickname%", nickname);
                    player.sendMessage(ColorUtils.colorize(message));
                });
            });

        return true;
    }
}