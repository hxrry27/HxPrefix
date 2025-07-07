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
            player.sendMessage(plugin.getConfigManager().getMessage("permissions.no-permission"));
            return true;
        }

        // Check rank access
        String rank = PermissionUtils.getPlayerRank(player);
        if (!PermissionUtils.hasNickAccess(rank)) {
            player.sendMessage(plugin.getConfigManager().getMessage("permissions.no-permission-rank")
                .replace("{rank}", rank));
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
                            player.sendMessage(plugin.getConfigManager().getMessage("nickname.removed"));
                            plugin.getNametagManager().updateNametag(player);
                        });
                    }
                });
            return true;
        }

        // Validate nickname
        if (!ColorUtils.isValidNickname(nickname)) {
            String message = plugin.getConfigManager().getMessage("nickname.invalid");
            message = message
                .replace("{min}", String.valueOf(plugin.getConfig().getInt("nickname.min-length", 3)))
                .replace("{max}", String.valueOf(plugin.getConfig().getInt("nickname.max-length", 16)));
            player.sendMessage(message);
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
                    String message = plugin.getConfigManager().getMessage("nickname.changed");
                    message = message.replace("{value}", nickname);
                    player.sendMessage(message);
                });
            });

        return true;
    }
}