package dev.hxrry.hxprefix.config;

import dev.hxrry.hxcore.utils.Log;

import dev.hxrry.hxprefix.HxPrefix;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * handles permission configuration for ranks and features
 */
public class PermissionConfig {
    private final HxPrefix plugin;
    private final FileConfiguration config;
    
    // rank feature permissions
    private final Map<String, RankPermissions> ranks = new HashMap<>();
    
    // feature permission nodes
    private final Map<String, String> permissionNodes = new HashMap<>();
    
    public PermissionConfig(@NotNull HxPrefix plugin, @NotNull FileConfiguration config) {
        this.plugin = plugin;
        this.config = config;
    }
    
    /**
     * load permission configuration
     */
    public void load() {
        ranks.clear();
        permissionNodes.clear();
        
        // load rank permissions
        loadRankPermissions();
        
        // load permission nodes
        loadPermissionNodes();
        
        Log.debug("loaded permissions for " + ranks.size() + " ranks");
    }
    
    /**
     * load rank-based permissions
     */
    private void loadRankPermissions() {
        ConfigurationSection ranksSection = config.getConfigurationSection("ranks");
        if (ranksSection == null) {
            Log.warning("no ranks section in config.yml - using defaults");
            loadDefaultRanks();
            return;
        }
        
        for (String rankName : ranksSection.getKeys(false)) {
            ConfigurationSection rankSection = ranksSection.getConfigurationSection(rankName);
            if (rankSection == null) continue;
            
            RankPermissions perms = new RankPermissions(rankName);
            
            // load feature permissions
            perms.canUseColours = rankSection.getBoolean("colours", false);
            perms.canUsePrefixes = rankSection.getBoolean("prefixes", false);
            perms.canUseSuffixes = rankSection.getBoolean("suffixes", false);
            perms.canUseNicknames = rankSection.getBoolean("nicknames", false);
            perms.canUseCustomTags = rankSection.getBoolean("custom-tags", false);
            perms.canUseCustomColour = rankSection.getBoolean("custom-colour", false);
            perms.canUseCustomSuffix = rankSection.getBoolean("custom-suffix", false);
            
            // load limits
            perms.maxNicknameLength = rankSection.getInt("max-nickname-length", 16);
            perms.minNicknameLength = rankSection.getInt("min-nickname-length", 3);
            perms.nicknameCooldown = rankSection.getInt("nickname-cooldown", 300);
            
            // load special permissions
            perms.bypassCooldown = rankSection.getBoolean("bypass-cooldown", false);
            perms.bypassFilter = rankSection.getBoolean("bypass-filter", false);
            
            ranks.put(rankName.toLowerCase(), perms);
        }
    }
    
    /**
     * load default rank configuration
     */
    private void loadDefaultRanks() {
        // default rank - no permissions
        RankPermissions defaultRank = new RankPermissions("default");
        ranks.put("default", defaultRank);
        
        // vip rank
        RankPermissions vip = new RankPermissions("vip");
        vip.canUseColours = true;
        vip.canUsePrefixes = true;
        vip.canUseSuffixes = true;
        vip.canUseNicknames = true;
        ranks.put("vip", vip);
        
        // supporter rank
        RankPermissions supporter = new RankPermissions("supporter");
        supporter.canUseColours = true;
        supporter.canUsePrefixes = true;
        supporter.canUseSuffixes = true;
        supporter.canUseNicknames = true;
        ranks.put("supporter", supporter);
        
        // patron rank
        RankPermissions patron = new RankPermissions("patron");
        patron.canUseColours = true;
        patron.canUsePrefixes = true;
        patron.canUseSuffixes = true;
        patron.canUseNicknames = true;
        patron.canUseCustomColour = true;
        ranks.put("patron", patron);
        
        // devoted rank
        RankPermissions devoted = new RankPermissions("devoted");
        devoted.canUseColours = true;
        devoted.canUsePrefixes = true;
        devoted.canUseSuffixes = true;
        devoted.canUseNicknames = true;
        devoted.canUseCustomTags = true;
        devoted.canUseCustomColour = true;
        devoted.canUseCustomSuffix = true;
        devoted.bypassCooldown = true;
        ranks.put("devoted", devoted);
        
        // legend rank
        RankPermissions legend = new RankPermissions("legend");
        legend.canUseColours = true;
        legend.canUsePrefixes = true;
        legend.canUseSuffixes = true;
        legend.canUseNicknames = true;
        legend.canUseCustomTags = true;
        legend.canUseCustomColour = true;
        legend.canUseCustomSuffix = true;
        legend.bypassCooldown = true;
        legend.bypassFilter = true;
        ranks.put("legend", legend);
        
        Log.info("loaded default rank permissions");
    }
    
    /**
     * load permission nodes for features
     */
    private void loadPermissionNodes() {
        ConfigurationSection nodesSection = config.getConfigurationSection("permission-nodes");
        if (nodesSection == null) {
            // use defaults
            permissionNodes.put("colour", "hxprefix.colour");
            permissionNodes.put("prefix", "hxprefix.prefix");
            permissionNodes.put("suffix", "hxprefix.suffix");
            permissionNodes.put("nickname", "hxprefix.nickname");
            permissionNodes.put("custom-tags", "hxprefix.tags");
            permissionNodes.put("custom-colour", "hxprefix.custom.colour");
            permissionNodes.put("custom-suffix", "hxprefix.custom.suffix");
            return;
        }
        
        for (String feature : nodesSection.getKeys(false)) {
            String node = nodesSection.getString(feature);
            if (node != null) {
                permissionNodes.put(feature, node);
            }
        }
    }
    
    /**
     * check if a player has permission for a feature
     */
    public boolean hasPermission(@NotNull Player player, @NotNull String feature) {
        // check explicit permission first
        String permNode = permissionNodes.get(feature);
        if (permNode != null && player.hasPermission(permNode)) {
            return true;
        }
        
        // check rank-based permission
        String rank = getRank(player);
        RankPermissions perms = ranks.get(rank.toLowerCase());
        
        if (perms == null) {
            return false;
        }
        
        return switch (feature) {
            case "colour", "colours" -> perms.canUseColours;
            case "prefix", "prefixes" -> perms.canUsePrefixes;
            case "suffix", "suffixes" -> perms.canUseSuffixes;
            case "nickname", "nicknames" -> perms.canUseNicknames;
            case "custom-tags" -> perms.canUseCustomTags;
            case "custom-colour" -> perms.canUseCustomColour;
            case "custom-suffix" -> perms.canUseCustomSuffix;
            default -> false;
        };
    }
    
    /**
     * get a player's rank
     */
    @NotNull
    private String getRank(@NotNull Player player) {
        if (plugin.getLuckPermsHook() != null) {
            return plugin.getLuckPermsHook().getPrimaryGroup(player);
        }
        
        // fallback - check by permission
        for (String rank : ranks.keySet()) {
            if (player.hasPermission("group." + rank)) {
                return rank;
            }
        }
        
        return "default";
    }
    
    /**
     * get rank permissions
     */
    @NotNull
    public RankPermissions getRankPermissions(@NotNull String rank) {
        RankPermissions perms = ranks.get(rank.toLowerCase());
        return perms != null ? perms : new RankPermissions(rank);
    }
    
    /**
     * check if a rank exists
     */
    public boolean rankExists(@NotNull String rank) {
        return ranks.containsKey(rank.toLowerCase());
    }
    
    /**
     * get all rank names
     */
    @NotNull
    public Set<String> getRankNames() {
        return new HashSet<>(ranks.keySet());
    }
    
    /**
     * get nickname limits for a player
     */
    public int getMaxNicknameLength(@NotNull Player player) {
        String rank = getRank(player);
        RankPermissions perms = ranks.get(rank.toLowerCase());
        return perms != null ? perms.maxNicknameLength : 16;
    }
    
    public int getMinNicknameLength(@NotNull Player player) {
        String rank = getRank(player);
        RankPermissions perms = ranks.get(rank.toLowerCase());
        return perms != null ? perms.minNicknameLength : 3;
    }
    
    public int getNicknameCooldown(@NotNull Player player) {
        String rank = getRank(player);
        RankPermissions perms = ranks.get(rank.toLowerCase());
        return perms != null ? perms.nicknameCooldown : 300;
    }
    
    /**
     * check special permissions
     */
    public boolean canBypassCooldown(@NotNull Player player) {
        if (player.hasPermission("hxprefix.bypass.cooldown")) {
            return true;
        }
        
        String rank = getRank(player);
        RankPermissions perms = ranks.get(rank.toLowerCase());
        return perms != null && perms.bypassCooldown;
    }
    
    public boolean canBypassFilter(@NotNull Player player) {
        if (player.hasPermission("hxprefix.bypass.filter")) {
            return true;
        }
        
        String rank = getRank(player);
        RankPermissions perms = ranks.get(rank.toLowerCase());
        return perms != null && perms.bypassFilter;
    }
    
    /**
     * reload permissions
     */
    public void reload() {
        load();
    }
    
    /**
     * rank permission data
     */
    public static class RankPermissions {
        public final String name;
        public boolean canUseColours = false;
        public boolean canUsePrefixes = false;
        public boolean canUseSuffixes = false;
        public boolean canUseNicknames = false;
        public boolean canUseCustomTags = false;
        public boolean canUseCustomColour = false;
        public boolean canUseCustomSuffix = false;
        
        public int maxNicknameLength = 16;
        public int minNicknameLength = 3;
        public int nicknameCooldown = 300;
        
        public boolean bypassCooldown = false;
        public boolean bypassFilter = false;
        
        public RankPermissions(@NotNull String name) {
            this.name = name;
        }
    }
}