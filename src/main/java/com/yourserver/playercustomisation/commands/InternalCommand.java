package com.yourserver.playercustomisation.commands;

import com.yourserver.playercustomisation.PlayerCustomisation;
import com.yourserver.playercustomisation.models.PlayerData;
import com.yourserver.playercustomisation.utils.ColorUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class InternalCommand implements CommandExecutor {
    private final PlayerCustomisation plugin;

    public InternalCommand(PlayerCustomisation plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }

        if (args.length < 1) {
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "setcolor":
                if (args.length >= 2) {
                    String colorValue = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                    setPlayerColor(player, colorValue);
                }
                break;
            case "setprefix":
                if (args.length >= 2) {
                    String prefixValue = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                    setPlayerPrefix(player, prefixValue);
                }
                break;
            case "reset":
                if (args.length >= 2) {
                    switch (args[1].toLowerCase()) {
                        case "color":
                            resetPlayerColor(player);
                            break;
                        case "prefix":
                            resetPlayerPrefix(player);
                            break;
                    }
                }
                break;
        }

        return true;
    }

    private void setPlayerColor(Player player, String colorValue) {
        plugin.getPlayerDataManager().getPlayerData(player.getUniqueId())
            .thenAccept(data -> {
                if (data == null) {
                    data = new PlayerData(player.getUniqueId(), player.getName());
                }
                data.setNameColor(colorValue);
                plugin.getPlayerDataManager().savePlayerData(data).thenRun(() -> {
                    String message = plugin.getConfig().getString("messages.color-changed", "&aYour name color has been updated!");
                    player.sendMessage(ColorUtils.colorize(message));
                });
            });
    }

    private void setPlayerPrefix(Player player, String prefixValue) {
        plugin.getPlayerDataManager().getPlayerData(player.getUniqueId())
            .thenAccept(data -> {
                if (data == null) {
                    data = new PlayerData(player.getUniqueId(), player.getName());
                }
                
                // Store the prefix WITH its color/formatting
                data.setPrefixStyle(prefixValue);
                
                plugin.getPlayerDataManager().savePlayerData(data).thenRun(() -> {
                    // Use the updated getMessage that doesn't have [Custom]
                    String message = plugin.getConfigManager().getMessage("prefix-changed");
                    message = message.replace("{value}", ColorUtils.colorize(prefixValue));
                    player.sendMessage(ColorUtils.colorize(message));
                });
            });
    }

    private void resetPlayerColor(Player player) {
        plugin.getPlayerDataManager().getPlayerData(player.getUniqueId())
            .thenAccept(data -> {
                if (data != null) {
                    data.setNameColor(null);
                    plugin.getPlayerDataManager().savePlayerData(data).thenRun(() -> {
                        player.sendMessage(ColorUtils.colorize("&aYour name color has been reset!"));
                    });
                }
            });
    }

    private void resetPlayerPrefix(Player player) {
        plugin.getPlayerDataManager().getPlayerData(player.getUniqueId())
            .thenAccept(data -> {
                if (data != null) {
                    data.setPrefixStyle(null);
                    plugin.getPlayerDataManager().savePlayerData(data).thenRun(() -> {
                        player.sendMessage(ColorUtils.colorize("&aYour prefix has been reset!"));
                    });
                }
            });
    }
}