package com.prefix27.customization.database;

import java.util.UUID;

public class PlayerData {
    
    private final UUID uuid;
    private String username;
    private String currentNameColor;
    private String currentNameGradient;
    private String currentPrefixId;
    private String currentNickname;
    private String rank;
    
    public PlayerData(UUID uuid, String username, String currentNameColor, 
                     String currentNameGradient, String currentPrefixId, 
                     String currentNickname, String rank) {
        this.uuid = uuid;
        this.username = username;
        this.currentNameColor = currentNameColor;
        this.currentNameGradient = currentNameGradient;
        this.currentPrefixId = currentPrefixId;
        this.currentNickname = currentNickname;
        this.rank = rank;
    }
    
    public PlayerData(UUID uuid, String username, String rank) {
        this.uuid = uuid;
        this.username = username;
        this.rank = rank;
        this.currentNameColor = null;
        this.currentNameGradient = null;
        this.currentPrefixId = null;
        this.currentNickname = null;
    }
    
    // Getters
    public UUID getUuid() {
        return uuid;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getCurrentNameColor() {
        return currentNameColor;
    }
    
    public String getCurrentNameGradient() {
        return currentNameGradient;
    }
    
    public String getCurrentPrefixId() {
        return currentPrefixId;
    }
    
    public String getCurrentNickname() {
        return currentNickname;
    }
    
    public String getRank() {
        return rank;
    }
    
    // Setters
    public void setUsername(String username) {
        this.username = username;
    }
    
    public void setCurrentNameColor(String currentNameColor) {
        this.currentNameColor = currentNameColor;
    }
    
    public void setCurrentNameGradient(String currentNameGradient) {
        this.currentNameGradient = currentNameGradient;
    }
    
    public void setCurrentPrefixId(String currentPrefixId) {
        this.currentPrefixId = currentPrefixId;
    }
    
    public void setCurrentNickname(String currentNickname) {
        this.currentNickname = currentNickname;
    }
    
    public void setRank(String rank) {
        this.rank = rank;
    }
    
    // Utility methods
    public boolean hasNameColor() {
        return currentNameColor != null && !currentNameColor.isEmpty();
    }
    
    public boolean hasNameGradient() {
        return currentNameGradient != null && !currentNameGradient.isEmpty();
    }
    
    public boolean hasPrefix() {
        return currentPrefixId != null && !currentPrefixId.isEmpty();
    }
    
    public boolean hasNickname() {
        return currentNickname != null && !currentNickname.isEmpty();
    }
    
    public String getDisplayName() {
        if (hasNickname()) {
            return currentNickname;
        }
        return username;
    }
    
    @Override
    public String toString() {
        return "PlayerData{" +
                "uuid=" + uuid +
                ", username='" + username + '\'' +
                ", currentNameColor='" + currentNameColor + '\'' +
                ", currentNameGradient='" + currentNameGradient + '\'' +
                ", currentPrefixId='" + currentPrefixId + '\'' +
                ", currentNickname='" + currentNickname + '\'' +
                ", rank='" + rank + '\'' +
                '}';
    }
}