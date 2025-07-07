package com.yourserver.playercustomisation.nametags;

import com.yourserver.playercustomisation.PlayerCustomisation;
import com.yourserver.playercustomisation.models.PlayerData;
import com.yourserver.playercustomisation.utils.ColorUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.UUID;

/**
 * Manages nametags above player heads using scoreboard teams
 */
public class NametagManager {
    private final PlayerCustomisation plugin;
    private final Scoreboard scoreboard;
    private static final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacySection();
    
    public NametagManager(PlayerCustomisation plugin) {
        this.plugin = plugin;
        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    }
    
    /**
     * Update a player's nametag
     */
    public void updateNametag(Player player) {
        plugin.getPlayerDataManager().getPlayerData(player.getUniqueId())
            .thenAccept(data -> {
                if (data == null) {
                    // No custom data, just use default
                    removeFromTeam(player);
                    return;
                }
                
                // Get or create team for this player
                Team team = getOrCreateTeam(player);
                
                // Set prefix if they have one
                if (data.getPrefixStyle() != null && !data.getPrefixStyle().isEmpty()) {
                    String prefix = ColorUtils.colorize(data.getPrefixStyle()) + " ";
                    // Limit to 64 characters (Minecraft limit)
                    if (prefix.length() > 64) {
                        prefix = prefix.substring(0, 64);
                    }
                    team.setPrefix(prefix);
                } else {
                    team.setPrefix("");
                }
                
                // Set suffix if they have one
                if (data.getSuffix() != null && !data.getSuffix().isEmpty()) {
                    String suffix = " " + ColorUtils.colorize(data.getSuffix());
                    // Limit to 64 characters
                    if (suffix.length() > 64) {
                        suffix = suffix.substring(0, 64);
                    }
                    team.setSuffix(suffix);
                } else {
                    team.setSuffix("");
                }
                
                // Set name color
                if (data.getNameColor() != null && !data.getNameColor().isEmpty()) {
                    // Extract main color for the name
                    ChatColor color = extractMainColor(data.getNameColor());
                    if (color != null) {
                        team.setColor(color);
                    }
                }
                
                // Make sure player is in the team
                if (!team.hasEntry(player.getName())) {
                    team.addEntry(player.getName());
                }
            });
    }
    
    /**
     * Remove a player's custom nametag
     */
    public void removeNametag(Player player) {
        removeFromTeam(player);
    }
    
    /**
     * Get or create a team for a player
     */
    private Team getOrCreateTeam(Player player) {
        // Use a unique team name based on player UUID (limited to 16 chars)
        String teamName = "pc_" + player.getUniqueId().toString().substring(0, 13);
        
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }
        
        return team;
    }
    
    /**
     * Remove player from their team
     */
    private void removeFromTeam(Player player) {
        String teamName = "pc_" + player.getUniqueId().toString().substring(0, 13);
        Team team = scoreboard.getTeam(teamName);
        
        if (team != null) {
            team.removeEntry(player.getName());
            // If team is empty, remove it
            if (team.getEntries().isEmpty()) {
                team.unregister();
            }
        }
    }
    
    /**
     * Extract the main ChatColor from a color string
     */
    private ChatColor extractMainColor(String colorString) {
        // Handle hex colors
        if (colorString.contains("#")) {
            // For hex/gradients, default to white
            // You could enhance this to convert hex to nearest ChatColor
            return ChatColor.WHITE;
        }
        
        // Handle MiniMessage format
        if (colorString.startsWith("<") && colorString.contains(">")) {
            String colorName = colorString.substring(1, colorString.indexOf(">"));
            switch (colorName.toLowerCase()) {
                case "red": return ChatColor.RED;
                case "dark_red": return ChatColor.DARK_RED;
                case "blue": return ChatColor.BLUE;
                case "dark_blue": return ChatColor.DARK_BLUE;
                case "green": return ChatColor.GREEN;
                case "dark_green": return ChatColor.DARK_GREEN;
                case "yellow": return ChatColor.YELLOW;
                case "gold": return ChatColor.GOLD;
                case "aqua": return ChatColor.AQUA;
                case "dark_aqua": return ChatColor.DARK_AQUA;
                case "light_purple": return ChatColor.LIGHT_PURPLE;
                case "dark_purple": return ChatColor.DARK_PURPLE;
                case "white": return ChatColor.WHITE;
                case "gray": return ChatColor.GRAY;
                case "dark_gray": return ChatColor.DARK_GRAY;
                case "black": return ChatColor.BLACK;
                default: return ChatColor.WHITE;
            }
        }
        
        // Handle legacy color codes
        if (colorString.startsWith("&")) {
            char code = colorString.charAt(1);
            ChatColor color = ChatColor.getByChar(code);
            return color != null ? color : ChatColor.WHITE;
        }
        
        return ChatColor.WHITE;
    }
    
    /**
     * Update all online players' nametags
     */
    public void updateAllNametags() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateNametag(player);
        }
    }
    
    /**
     * Clean up on shutdown
     */
    public void shutdown() {
        // Remove all custom teams
        for (Team team : scoreboard.getTeams()) {
            if (team.getName().startsWith("pc_")) {
                team.unregister();
            }
        }
    }
}