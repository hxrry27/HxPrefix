package com.yourserver.playercustomisation.commands;

import com.yourserver.playercustomisation.PlayerCustomisation;
import com.yourserver.playercustomisation.models.PlayerData;
import com.yourserver.playercustomisation.utils.ColorUtils;

import org.bukkit.Sound;
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
                        case "suffix":
                            resetPlayerSuffix(player);
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
                    
                    // ADD THIS: Update nametag after color change
                    if (plugin.getNametagManager() != null) {
                        plugin.getNametagManager().updateNametag(player);
                    }
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
                    
                    // ADD THIS: Update nametag after prefix change
                    if (plugin.getNametagManager() != null) {
                        plugin.getNametagManager().updateNametag(player);
                    }
                });
            });
    }

    private void setPlayerSuffix(Player player, String suffixValue) {
        plugin.getPlayerDataManager().getPlayerData(player.getUniqueId())
            .thenAccept(data -> {
                if (data == null) {
                    data = new PlayerData(player.getUniqueId(), player.getName());
                }
                
                // Store the suffix
                data.setSuffix(suffixValue);
                
                plugin.getPlayerDataManager().savePlayerData(data).thenRun(() -> {
                    // Play sound
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    
                    // Send message
                    String message = plugin.getConfigManager().getMessage("suffix.changed");
                    message = message.replace("{value}", ColorUtils.colorize(suffixValue));
                    player.sendMessage(message);
                    
                    // Update nametag HERE!
                    if (plugin.getNametagManager() != null) {
                        plugin.getNametagManager().updateNametag(player);
                    }
                });
            });
    }

    private void resetPlayerSuffix(Player player) {
        plugin.getPlayerDataManager().getPlayerData(player.getUniqueId())
            .thenAccept(data -> {
                if (data != null) {
                    data.setSuffix(null);
                    
                    plugin.getPlayerDataManager().savePlayerData(data).thenRun(() -> {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                        player.sendMessage(plugin.getConfigManager().getMessage("suffix.removed"));
                        
                        // Update nametag HERE too!
                        if (plugin.getNametagManager() != null) {
                            plugin.getNametagManager().updateNametag(player);
                        }
                    });
                }
            });
    }

    private void resetPlayerColor(Player player) {
        plugin.getPlayerDataManager().getPlayerData(player.getUniqueId())
            .thenAccept(data -> {
                if (data != null) {
                    data.setNameColor(null);
                    plugin.getPlayerDataManager().savePlayerData(data).thenRun(() -> {
                        player.sendMessage(ColorUtils.colorize("&aYour name color has been reset!"));
                        
                        // ADD THIS: Update nametag after color reset
                        if (plugin.getNametagManager() != null) {
                            plugin.getNametagManager().updateNametag(player);
                        }
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
                        
                        // ADD THIS: Update nametag after prefix reset
                        if (plugin.getNametagManager() != null) {
                            plugin.getNametagManager().updateNametag(player);
                        }
                    });
                }
            });
    }
}