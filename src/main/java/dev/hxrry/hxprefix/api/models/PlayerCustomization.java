package dev.hxrry.hxprefix.api.models;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * holds all customization data for a player
 */
public class PlayerCustomization {
    private final UUID uuid;
    private String username;
    private String nickname;
    private String nameColour;
    private String prefix;
    private String suffix;
    private String customTagRequest; // pending custom tag if any
    private long lastUpdated;
    
    // constructor for new players
    public PlayerCustomization(@NotNull UUID uuid, @NotNull String username) {
        this.uuid = uuid;
        this.username = username;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    // full constructor for loading from database
    public PlayerCustomization(@NotNull UUID uuid, @NotNull String username, 
                              @Nullable String nickname, @Nullable String nameColour,
                              @Nullable String prefix, @Nullable String suffix,
                              @Nullable String customTagRequest, long lastUpdated) {
        this.uuid = uuid;
        this.username = username;
        this.nickname = nickname;
        this.nameColour = nameColour;
        this.prefix = prefix;
        this.suffix = suffix;
        this.customTagRequest = customTagRequest;
        this.lastUpdated = lastUpdated;
    }
    
    // getters
    @NotNull
    public UUID getUuid() { 
        return uuid; 
    }
    
    @NotNull
    public String getUsername() { 
        return username; 
    }
    
    @Nullable
    public String getNickname() { 
        return nickname; 
    }
    
    @Nullable
    public String getNameColour() { 
        return nameColour; 
    }
    
    @Nullable
    public String getPrefix() { 
        return prefix; 
    }
    
    @Nullable
    public String getSuffix() { 
        return suffix; 
    }
    
    @Nullable
    public String getCustomTagRequest() { 
        return customTagRequest; 
    }
    
    public long getLastUpdated() { 
        return lastUpdated; 
    }
    
    // setters - all update the timestamp
    public void setUsername(@NotNull String username) {
        this.username = username;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public void setNickname(@Nullable String nickname) {
        this.nickname = nickname;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public void setNameColour(@Nullable String nameColour) {
        this.nameColour = nameColour;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public void setPrefix(@Nullable String prefix) {
        this.prefix = prefix;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public void setSuffix(@Nullable String suffix) {
        this.suffix = suffix;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public void setCustomTagRequest(@Nullable String customTagRequest) {
        this.customTagRequest = customTagRequest;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    // utility methods
    
    /**
     * get the display name (nickname or username)
     */
    @NotNull
    public String getDisplayName() {
        return nickname != null ? nickname : username;
    }
    
    /**
     * check if player has any customizations
     */
    public boolean hasCustomizations() {
        return nickname != null || nameColour != null || 
               prefix != null || suffix != null;
    }
    
    /**
     * check if player has a pending custom tag request
     */
    public boolean hasPendingTagRequest() {
        return customTagRequest != null;
    }
    
    /**
     * clear all customizations (reset to default)
     */
    public void clearAll() {
        this.nickname = null;
        this.nameColour = null;
        this.prefix = null;
        this.suffix = null;
        this.customTagRequest = null;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    /**
     * create a copy of this data
     */
    @NotNull
    public PlayerCustomization copy() {
        return new PlayerCustomization(
            uuid, username, nickname, nameColour, 
            prefix, suffix, customTagRequest, lastUpdated
        );
    }
    
    @Override
    public String toString() {
        return "PlayerCustomization{" +
            "uuid=" + uuid +
            ", username='" + username + '\'' +
            ", nickname='" + nickname + '\'' +
            ", nameColour='" + nameColour + '\'' +
            ", prefix='" + prefix + '\'' +
            ", suffix='" + suffix + '\'' +
            ", hasTagRequest=" + (customTagRequest != null) +
            '}';
    }
    
    /**
     * builder pattern for easy creation
     */
    public static class Builder {
        private final UUID uuid;
        private final String username;
        private String nickname;
        private String nameColour;
        private String prefix;
        private String suffix;
        private String customTagRequest;
        private long lastUpdated;
        
        public Builder(@NotNull UUID uuid, @NotNull String username) {
            this.uuid = uuid;
            this.username = username;
            this.lastUpdated = System.currentTimeMillis();
        }
        
        public Builder nickname(String nickname) {
            this.nickname = nickname;
            return this;
        }
        
        public Builder nameColour(String nameColour) {
            this.nameColour = nameColour;
            return this;
        }
        
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }
        
        public Builder suffix(String suffix) {
            this.suffix = suffix;
            return this;
        }
        
        public Builder customTagRequest(String request) {
            this.customTagRequest = request;
            return this;
        }
        
        public Builder lastUpdated(long timestamp) {
            this.lastUpdated = timestamp;
            return this;
        }
        
        public PlayerCustomization build() {
            return new PlayerCustomization(
                uuid, username, nickname, nameColour,
                prefix, suffix, customTagRequest, lastUpdated
            );
        }
    }
}