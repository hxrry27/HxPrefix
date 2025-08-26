package dev.hxrry.hxprefix.nametags;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;

import dev.hxrry.hxcore.utils.Log;

import dev.hxrry.hxprefix.HxPrefix;
import dev.hxrry.hxprefix.api.models.PlayerCustomization;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * manages player nametags above their heads
 */
public class NametagManager {
    private final HxPrefix plugin;
    private final ProtocolManager protocolManager;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final GsonComponentSerializer gson = GsonComponentSerializer.gson();
    
    // team tracking
    private final Map<UUID, String> playerTeams = new ConcurrentHashMap<>();
    private final Map<String, TeamData> teams = new ConcurrentHashMap<>();
    
    // update batching
    private final Set<UUID> pendingUpdates = ConcurrentHashMap.newKeySet();
    private BukkitRunnable updateTask;
    
    public NametagManager(@NotNull HxPrefix plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        
        startUpdateTask();
    }
    
    /**
     * initialize nametags for all online players
     */
    public void initialize() {
        if (!plugin.getConfigManager().isNametagsEnabled()) {
            Log.info("nametags disabled in config");
            return;
        }
        
        // setup teams for all online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            setupPlayer(player);
        }
        
        Log.info("initialized nametags for " + Bukkit.getOnlinePlayers().size() + " players");
    }
    
    /**
     * setup nametag for a player
     */
    public void setupPlayer(@NotNull Player player) {
        if (!plugin.getConfigManager().isNametagsEnabled()) {
            return;
        }
        
        // get player data
        @SuppressWarnings("unused")
        PlayerCustomization data = plugin.getDataCache().getOrCreatePlayerData(player.getUniqueId());
        
        // determine team name based on rank weight
        String teamName = getTeamName(player);
        
        // create team if needed
        if (!teams.containsKey(teamName)) {
            createTeam(teamName, player);
        }
        
        // add player to team
        addToTeam(player, teamName);
        
        // update display
        updatePlayer(player);
    }
    
    /**
     * update a player's nametag
     */
    public void updatePlayer(@NotNull Player player) {
        if (!plugin.getConfigManager().isNametagsEnabled()) {
            return;
        }
        
        // batch updates to reduce packet spam
        pendingUpdates.add(player.getUniqueId());
    }
    
    /**
     * process pending updates
     */
    private void processPendingUpdates() {
        if (pendingUpdates.isEmpty()) {
            return;
        }
        
        Set<UUID> toUpdate = new HashSet<>(pendingUpdates);
        pendingUpdates.clear();
        
        for (UUID uuid : toUpdate) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                updatePlayerInternal(player);
            }
        }
    }
    
    /**
     * internal update method
     */
    private void updatePlayerInternal(@NotNull Player player) {
        // get player data
        PlayerCustomization data = plugin.getDataCache().getPlayerData(player.getUniqueId());
        if (data == null) {
            return;
        }
        
        // get current team
        String teamName = playerTeams.get(player.getUniqueId());
        if (teamName == null) {
            setupPlayer(player);
            return;
        }
        
        TeamData team = teams.get(teamName);
        if (team == null) {
            return;
        }
        
        // build prefix component
        Component prefix = Component.empty();
        if (data.getPrefix() != null) {
            prefix = mm.deserialize(data.getPrefix() + " ");
        }
        
        // build suffix component
        Component suffix = Component.empty();
        if (data.getSuffix() != null) {
            suffix = mm.deserialize(" " + data.getSuffix());
        }
        
        // update team data
        team.prefix = prefix;
        team.suffix = suffix;
        team.nameColour = data.getNameColour();
        
        // send update packets
        sendTeamUpdate(team);
    }
    
    /**
     * remove a player's nametag
     */
    public void removePlayer(@NotNull Player player) {
        String teamName = playerTeams.remove(player.getUniqueId());
        if (teamName == null) {
            return;
        }
        
        TeamData team = teams.get(teamName);
        if (team != null) {
            team.members.remove(player.getName());
            
            // remove team if empty
            if (team.members.isEmpty()) {
                teams.remove(teamName);
                sendTeamRemove(teamName);
            } else {
                // update team membership
                sendTeamMemberRemove(teamName, player.getName());
            }
        }
    }
    
    /**
     * get team name for a player based on rank weight
     */
    @NotNull
    private String getTeamName(@NotNull Player player) {
        // get rank weight for sorting
        int weight = 0;
        if (plugin.getLuckPermsHook() != null) {
            String rank = plugin.getLuckPermsHook().getPrimaryGroup(player);
            weight = plugin.getLuckPermsHook().getGroupWeight(rank);
        }
        
        // format weight as 3-digit number for sorting (000-999)
        String weightStr = String.format("%03d", Math.max(0, Math.min(999, 999 - weight)));
        
        // team name format: weight_uuid (first 8 chars)
        return weightStr + "_" + player.getUniqueId().toString().substring(0, 8);
    }
    
    /**
     * create a new team
     */
    private void createTeam(@NotNull String teamName, @NotNull Player owner) {
        TeamData team = new TeamData(teamName);
        teams.put(teamName, team);
        
        // send create packet
        sendTeamCreate(team);
    }
    
    /**
     * add player to team
     */
    private void addToTeam(@NotNull Player player, @NotNull String teamName) {
        // remove from old team if exists
        String oldTeam = playerTeams.put(player.getUniqueId(), teamName);
        if (oldTeam != null && !oldTeam.equals(teamName)) {
            TeamData old = teams.get(oldTeam);
            if (old != null) {
                old.members.remove(player.getName());
                sendTeamMemberRemove(oldTeam, player.getName());
            }
        }
        
        // add to new team
        TeamData team = teams.get(teamName);
        if (team != null) {
            team.members.add(player.getName());
            sendTeamMemberAdd(teamName, player.getName());
        }
    }
    
    /**
     * send team create packet
     */
    @SuppressWarnings("deprecation")
    private void sendTeamCreate(@NotNull TeamData team) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.SCOREBOARD_TEAM);
        
        // set team name
        packet.getStrings().write(0, team.name);
        
        // set mode to 0 (create team)
        packet.getIntegers().write(0, 0);
        
        // set display name
        packet.getChatComponents().write(0, WrappedChatComponent.fromText(team.name));
        
        // set prefix
        String prefixJson = gson.serialize(team.prefix);
        packet.getChatComponents().write(1, WrappedChatComponent.fromJson(prefixJson));
        
        // set suffix
        String suffixJson = gson.serialize(team.suffix);
        packet.getChatComponents().write(2, WrappedChatComponent.fromJson(suffixJson));
        
        // set options (friendly fire, see invisible)
        packet.getIntegers().write(1, 3); // allow friendly fire + see invisible
        
        // set name tag visibility
        packet.getStrings().write(1, "always");
        
        // set collision rule
        packet.getStrings().write(2, "always");
        
        // set colour (for glowing effect)
        packet.getEnumModifier(ChatColor.class, 0).write(0, ChatColor.WHITE);
        
        // add members
        packet.getModifier().write(7, new ArrayList<>(team.members));
        
        // send to all players
        broadcastPacket(packet);
    }
    
    /**
     * send team update packet
     */
    private void sendTeamUpdate(@NotNull TeamData team) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.SCOREBOARD_TEAM);
        
        // set team name
        packet.getStrings().write(0, team.name);
        
        // set mode to 2 (update team info)
        packet.getIntegers().write(0, 2);
        
        // set display name
        packet.getChatComponents().write(0, WrappedChatComponent.fromText(team.name));
        
        // set prefix
        String prefixJson = gson.serialize(team.prefix);
        packet.getChatComponents().write(1, WrappedChatComponent.fromJson(prefixJson));
        
        // set suffix
        String suffixJson = gson.serialize(team.suffix);
        packet.getChatComponents().write(2, WrappedChatComponent.fromJson(suffixJson));
        
        // set options
        packet.getIntegers().write(1, 3);
        
        // set name tag visibility
        packet.getStrings().write(1, "always");
        
        // set collision rule
        packet.getStrings().write(2, "always");
        
        // send to all players
        broadcastPacket(packet);
    }
    
    /**
     * send team remove packet
     */
    private void sendTeamRemove(@NotNull String teamName) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.SCOREBOARD_TEAM);
        
        // set team name
        packet.getStrings().write(0, teamName);
        
        // set mode to 1 (remove team)
        packet.getIntegers().write(0, 1);
        
        // send to all players
        broadcastPacket(packet);
    }
    
    /**
     * send team member add packet
     */
    private void sendTeamMemberAdd(@NotNull String teamName, @NotNull String playerName) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.SCOREBOARD_TEAM);
        
        // set team name
        packet.getStrings().write(0, teamName);
        
        // set mode to 3 (add players)
        packet.getIntegers().write(0, 3);
        
        // set players to add
        packet.getModifier().write(7, Collections.singletonList(playerName));
        
        // send to all players
        broadcastPacket(packet);
    }
    
    /**
     * send team member remove packet
     */
    private void sendTeamMemberRemove(@NotNull String teamName, @NotNull String playerName) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.SCOREBOARD_TEAM);
        
        // set team name
        packet.getStrings().write(0, teamName);
        
        // set mode to 4 (remove players)
        packet.getIntegers().write(0, 4);
        
        // set players to remove
        packet.getModifier().write(7, Collections.singletonList(playerName));
        
        // send to all players
        broadcastPacket(packet);
    }
    
    /**
     * broadcast packet to all players
     */
    private void broadcastPacket(@NotNull PacketContainer packet) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                protocolManager.sendServerPacket(player, packet);
            } catch (Exception e) {
                Log.debug("failed to send packet to " + player.getName() + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * start the update task
     */
    private void startUpdateTask() {
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                processPendingUpdates();
            }
        };
        
        // run every 5 ticks (0.25 seconds)
        updateTask.runTaskTimer(plugin, 5L, 5L);
    }
    
    /**
     * cleanup resources
     */
    public void cleanup() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        
        // remove all teams
        for (String teamName : teams.keySet()) {
            sendTeamRemove(teamName);
        }
        
        teams.clear();
        playerTeams.clear();
        pendingUpdates.clear();
    }
    
    /**
     * team data holder
     */
    private static class TeamData {
        final String name;
        final Set<String> members = new HashSet<>();
        Component prefix = Component.empty();
        Component suffix = Component.empty();
        @SuppressWarnings("unused")
        String nameColour = null;
        
        TeamData(@NotNull String name) {
            this.name = name;
        }
    }
}