package com.yourserver.playercustomisation.placeholders;

import com.yourserver.playercustomisation.PlayerCustomisation;
import com.yourserver.playercustomisation.models.PlayerData;
import com.yourserver.playercustomisation.utils.ColorUtils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CustomisationExpansion extends PlaceholderExpansion {
    private final PlayerCustomisation plugin;

    public CustomisationExpansion(PlayerCustomisation plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "playercustomisation";
    }

    @Override
    @NotNull
    public String getAuthor() {
        return "YourServer";
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        // Get player data synchronously (from cache if available)
        PlayerData data = null;
        try {
            data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId()).get();
        } catch (Exception e) {
            // Return default values if we can't get data
        }

        switch (params.toLowerCase()) {
            case "prefix":
                if (data != null && data.hasCustomPrefix()) {
                    return ColorUtils.colorize(data.getCustomPrefix());
                } else if (data != null && data.getPrefixStyle() != null) {
                    return ColorUtils.colorize(data.getPrefixStyle());  // This adds ONE space
                }
                return "";

            case "nickname":
                if (data != null && data.getNickname() != null) {
                    return data.getNickname();
                }
                return player.getName();

            case "namecolor":
                if (data != null && data.getNameColor() != null) {
                    return data.getNameColor();
                }
                return "&f"; // Default white

            case "formatted_name":
                String displayName = player.getName();
                if (data != null && data.getNickname() != null) {
                    displayName = data.getNickname();
                }
                
                if (data != null && data.getNameColor() != null) {
                    return ColorUtils.colorize(data.getNameColor() + displayName);
                }
                return displayName;

            default:
                return null;
        }
    }
}