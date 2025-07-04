package com.yourserver.playercustomisation.models;

import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private String username;
    private String nickname;
    private String nameColor;
    private String prefixStyle;
    private String customPrefix;

    public PlayerData(UUID uuid, String username) {
        this.uuid = uuid;
        this.username = username;
    }

    // Getters
    public UUID getUuid() { return uuid; }
    public String getUsername() { return username; }
    public String getNickname() { return nickname; }
    public String getNameColor() { return nameColor; }
    public String getPrefixStyle() { return prefixStyle; }
    public String getCustomPrefix() { return customPrefix; }

    // Setters
    public void setUsername(String username) { this.username = username; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public void setNameColor(String nameColor) { this.nameColor = nameColor; }
    public void setPrefixStyle(String prefixStyle) { this.prefixStyle = prefixStyle; }
    public void setCustomPrefix(String customPrefix) { this.customPrefix = customPrefix; }

    // Utility methods
    public String getDisplayName() {
        return nickname != null && !nickname.isEmpty() ? nickname : username;
    }

    public boolean hasCustomPrefix() {
        return customPrefix != null && !customPrefix.isEmpty();
    }
}