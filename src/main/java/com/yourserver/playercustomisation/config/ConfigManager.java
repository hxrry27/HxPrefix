package com.yourserver.playercustomisation.config;

import com.yourserver.playercustomisation.PlayerCustomisation;
import com.yourserver.playercustomisation.gui.MenuUtils;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration manager stub - will be fully implemented in Phase 4
 * This stub prevents errors while we work on Phase 2
 */
public class ConfigManager {
    private final PlayerCustomisation plugin;
    
    public ConfigManager(PlayerCustomisation plugin) {
        this.plugin = plugin;
    }
    
    // Temporary methods that return hardcoded values for testing
    
    public boolean canUseColors(String rank) {
        return !rank.equalsIgnoreCase("default");
    }
    
    public boolean canUseGradients(String rank) {
        return rank.equalsIgnoreCase("patron") || 
               rank.equalsIgnoreCase("devoted") || 
               rank.equalsIgnoreCase("premium") ||
               rank.equalsIgnoreCase("vip+");
    }
    
    public boolean canUseRainbow(String rank) {
        return rank.equalsIgnoreCase("devoted") || 
               rank.equalsIgnoreCase("premium");
    }
    
    public boolean canUsePrefix(String rank) {
        return !rank.equalsIgnoreCase("default");
    }
    
    public boolean canUseSuffix(String rank) {
        return rank.equalsIgnoreCase("vip+") ||
               rank.equalsIgnoreCase("patron") || 
               rank.equalsIgnoreCase("devoted") || 
               rank.equalsIgnoreCase("premium");
    }
    
    public List<String> getAvailablePrefixes(String rank) {
        // Temporary hardcoded prefixes
        if (rank.equalsIgnoreCase("vip")) {
            return Arrays.asList("VIP", "MEMBER");
        } else if (rank.equalsIgnoreCase("supporter")) {
            return Arrays.asList("SUPPORTER", "HELPER", "MEMBER");
        } else {
            // Higher ranks get all prefixes
            return Arrays.asList(
                "PLAYER", "MEMBER", "VIP", "VIP+", "ELITE",
                "PREMIUM", "HERO", "LEGEND", "CHAMPION",
                "SUPPORTER", "PATRON", "DEVOTED"
            );
        }
    }
    
    public List<String> getAvailableSuffixes(String rank) {
        // Temporary hardcoded suffixes
        return Arrays.asList("★", "✦", "♦", "✓", "✪", "⚡", "✯", "♛", "⚔");
    }
    
    public String getMessage(String key) {
        // Temporary messages
        switch (key) {
            case "no-permission":
                return "&8[&bCustom&8] &cYou don't have permission!";
            case "no-permission-rank":
                return "&8[&bCustom&8] &cYour rank ({rank}) doesn't have access to this feature!";
            case "color-changed":
                return "&8[&bCustom&8] &aColor changed!";
            case "prefix-changed":
                return "&8[&bCustom&8] &aPrefix set!";
            case "suffix-changed":
                return "&8[&bCustom&8] &aSuffix set!";
            default:
                return "&8[&bCustom&8] &7" + key;
        }
    }
}