package dev.hxrry.hxprefix.commands;

import dev.hxrry.hxcore.commands.HxCommand;
import static dev.hxrry.hxcore.commands.HxCommand.arg;
import dev.hxrry.hxprefix.HxPrefix;
import dev.hxrry.hxprefix.api.models.PlayerCustomization;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class NickCommand extends CommandHelpers {
    
    public NickCommand(@NotNull HxPrefix plugin) { super(plugin); }
    
    public void register(HxPrefix plugin) {
        HxCommand.create("nick")

            .executes(sender ->{
                if (!(sender instanceof Player player)) {
                    sendPlayerOnly(sender);
                    return;
                }
                if (!checkNicknamePermission(player)) return;
                showCurrentNickname(player);
            })

            .executes(arg("nick", List.of("off", "reset")),
                (sender, nickname) -> {
                    
                    if (!(sender instanceof Player player)) {
                        sendPlayerOnly(sender);
                        return;
                    }
                    
                    if (!checkNicknamePermission(player)) return;
                    
                    if (nickname.equalsIgnoreCase("off") || nickname.equalsIgnoreCase("reset") || nickname.equalsIgnoreCase("remove")) {
                        removeNickname(player);
                        return;
                    }

                    if (!checkCooldown(player)) {
                        return;
                    }

                    setNickname(player, nickname);
                })
                .register(plugin);
    }

    /**
     * check if player has permission to use nicknames
     */
    private boolean checkNicknamePermission(@NotNull Player player) {
        if (!hasFeaturePermission(player, "nickname")) {
            sendMessage(player, "error.no-nickname-permission", 
                "{rank}", getPlayerRank(player));
            return false;
        }
        return true;
    }
    
    /**
     * show the player's current nickname
     */
    private void showCurrentNickname(@NotNull Player player) {
        String current = plugin.getAPI().getNickname(player);
        if (current != null) {
            sendMessage(player, "nickname.current", 
                "{nickname}", current);
        } else {
            sendMessage(player, "nickname.not-set");
            send(player, "<gray>use /nick <name> to set a nickname");
        }
    }
    
    /**
     * set a nickname
     */
    private void setNickname(@NotNull Player player, @NotNull String nickname) {
        // validate length
        int minLength = plugin.getConfigManager().getMainConfig()
            .getInt("nickname.min-length", 3);
        int maxLength = plugin.getConfigManager().getMainConfig()
            .getInt("nickname.max-length", 16);
        
        if (nickname.length() < minLength || nickname.length() > maxLength) {
            sendError(player, "nickname must be " + minLength + "-" + maxLength + " characters");
            return;
        }
        
        // validate format
        if (!isValidNickname(nickname)) {
            sendError(player, "nickname can only contain letters, numbers, and underscores");
            return;
        }
        
        // check blocked names
        if (isBlocked(nickname)) {
            sendError(player, "that nickname is not allowed");
            return;
        }
        
        // check if already taken (optional feature)
        if (plugin.getConfigManager().getMainConfig().getBoolean("nickname.unique", false)) {
            if (isNicknameTaken(nickname, player.getUniqueId())) {
                sendError(player, "that nickname is already taken");
                return;
            }
        }
        
        // set the nickname
        if (plugin.getAPI().setNickname(player, nickname)) {
            sendMessage(player, "nickname.changed", 
                "{nickname}", nickname);
            updateCooldown(player);
        } else {
            sendError(player, "failed to set nickname");
        }
    }
    
    /**
     * remove player's nickname
     */
    private void removeNickname(@NotNull Player player) {
        String current = plugin.getAPI().getNickname(player);
        if (current == null) {
            sendError(player, "you don't have a nickname set");
            return;
        }
        
        if (plugin.getAPI().setNickname(player, null)) {
            sendMessage(player, "nickname.removed");
        } else {
            sendError(player, "failed to remove nickname");
        }
    }
    
    /**
     * check if nickname is blocked
     */
    private boolean isBlocked(@NotNull String nickname) {
        List<String> blocked = plugin.getConfigManager().getMainConfig()
            .getStringList("nickname.blocked");
        
        String lower = nickname.toLowerCase();
        for (String block : blocked) {
            if (lower.contains(block.toLowerCase())) {
                return true;
            }
        }
        
        // also block staff names
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("hxprefix.staff") && 
                online.getName().equalsIgnoreCase(nickname)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * check if nickname is already taken
     */
    private boolean isNicknameTaken(@NotNull String nickname, @NotNull java.util.UUID excludePlayer) {
        // check online players first
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(excludePlayer)) continue;
            
            String theirNick = plugin.getAPI().getNickname(online);
            if (theirNick != null && theirNick.equalsIgnoreCase(nickname)) {
                return true;
            }
            
            // also check against real names
            if (online.getName().equalsIgnoreCase(nickname)) {
                return true;
            }
        }
        
        // would need database check for offline players
        // keeping it simple for now
        return false;
    }
    
    /**
     * check cooldown for nickname changes (now persistent!)
     */
    private boolean checkCooldown(@NotNull Player player) {
        // bypass for staff
        if (player.hasPermission("hxprefix.bypass.cooldown")) {
            return true;
        }
        
        int cooldownSeconds = plugin.getConfigManager().getMainConfig()
            .getInt("nickname.cooldown", 300); // 5 minutes default
        
        if (cooldownSeconds <= 0) {
            return true; // no cooldown
        }
        
        // Get player data from cache/database
        PlayerCustomization data = plugin.getAPI().getPlayerData(player);
        if (data == null) {
            return true; // shouldn't happen, but allow if no data
        }
        
        // Check if on cooldown using the persistent timestamp
        if (data.isOnNicknameCooldown(cooldownSeconds)) {
            int remaining = data.getRemainingCooldown(cooldownSeconds);
            int minutes = remaining / 60;
            int seconds = remaining % 60;
            
            String timeLeft = minutes > 0 ? 
                minutes + "m " + seconds + "s" : 
                seconds + "s";
                
            sendError(player, "please wait " + timeLeft + " before changing your nickname again");
            return false;
        }
        
        return true;
    }
    
    /**
     * update cooldown timestamp (now persistent!)
     */
    private void updateCooldown(@NotNull Player player) {
        PlayerCustomization data = plugin.getAPI().getPlayerData(player);
        if (data != null) {
            data.setLastNicknameChange(System.currentTimeMillis());
            plugin.getDataCache().savePlayerData(data);
        }
    }
}