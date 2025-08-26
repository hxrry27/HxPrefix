package dev.hxrry.hxprefix.api.models;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * represents a custom tag/prefix request from a player
 */
public class CustomTagRequest {
    
    /**
     * status of a tag request
     */
    public enum Status {
        PENDING("pending"),
        APPROVED("approved"),
        DENIED("denied"),
        EXPIRED("expired"); // auto-expire after X days
        
        private final String value;
        
        Status(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static Status fromString(String value) {
            for (Status status : values()) {
                if (status.value.equalsIgnoreCase(value)) {
                    return status;
                }
            }
            return PENDING;
        }
    }
    
    private int id;
    private final UUID playerUuid;
    private final String playerName;
    private final String requestedTag;
    private Status status;
    private UUID reviewedBy;
    private String reviewerName;
    private String denyReason; // reason if denied
    private final long requestedAt;
    private long reviewedAt;
    
    // constructor for new requests
    public CustomTagRequest(@NotNull UUID playerUuid, @NotNull String playerName, 
                           @NotNull String requestedTag) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.requestedTag = requestedTag;
        this.status = Status.PENDING;
        this.requestedAt = System.currentTimeMillis();
    }
    
    // full constructor for loading from database
    public CustomTagRequest(int id, @NotNull UUID playerUuid, @NotNull String playerName,
                           @NotNull String requestedTag, @NotNull Status status,
                           @Nullable UUID reviewedBy, @Nullable String reviewerName,
                           @Nullable String denyReason, long requestedAt, long reviewedAt) {
        this.id = id;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.requestedTag = requestedTag;
        this.status = status;
        this.reviewedBy = reviewedBy;
        this.reviewerName = reviewerName;
        this.denyReason = denyReason;
        this.requestedAt = requestedAt;
        this.reviewedAt = reviewedAt;
    }
    
    // getters
    public int getId() { 
        return id; 
    }
    
    @NotNull
    public UUID getPlayerUuid() { 
        return playerUuid; 
    }
    
    @NotNull
    public String getPlayerName() { 
        return playerName; 
    }
    
    @NotNull
    public String getRequestedTag() { 
        return requestedTag; 
    }
    
    @NotNull
    public Status getStatus() { 
        return status; 
    }
    
    @Nullable
    public UUID getReviewedBy() { 
        return reviewedBy; 
    }
    
    @Nullable
    public String getReviewerName() { 
        return reviewerName; 
    }
    
    @Nullable
    public String getDenyReason() { 
        return denyReason; 
    }
    
    public long getRequestedAt() { 
        return requestedAt; 
    }
    
    public long getReviewedAt() { 
        return reviewedAt; 
    }
    
    // setters
    public void setId(int id) { 
        this.id = id; 
    }
    
    public void setStatus(@NotNull Status status) { 
        this.status = status; 
    }
    
    /**
     * approve this request
     */
    public void approve(@NotNull UUID reviewerUuid, @NotNull String reviewerName) {
        this.status = Status.APPROVED;
        this.reviewedBy = reviewerUuid;
        this.reviewerName = reviewerName;
        this.reviewedAt = System.currentTimeMillis();
        this.denyReason = null;
    }
    
    /**
     * deny this request
     */
    public void deny(@NotNull UUID reviewerUuid, @NotNull String reviewerName, 
                     @Nullable String reason) {
        this.status = Status.DENIED;
        this.reviewedBy = reviewerUuid;
        this.reviewerName = reviewerName;
        this.reviewedAt = System.currentTimeMillis();
        this.denyReason = reason;
    }
    
    /**
     * mark as expired
     */
    public void expire() {
        this.status = Status.EXPIRED;
        this.reviewedAt = System.currentTimeMillis();
    }
    
    // utility methods
    
    /**
     * check if this request is still pending
     */
    public boolean isPending() {
        return status == Status.PENDING;
    }
    
    /**
     * check if this request has been reviewed
     */
    public boolean isReviewed() {
        return status != Status.PENDING;
    }
    
    /**
     * get age of request in milliseconds
     */
    public long getAge() {
        return System.currentTimeMillis() - requestedAt;
    }
    
    /**
     * get age in days
     */
    public int getAgeInDays() {
        return (int) (getAge() / (1000L * 60 * 60 * 24));
    }
    
    /**
     * check if request is old enough to auto-expire
     */
    public boolean shouldExpire(int maxDays) {
        return isPending() && getAgeInDays() >= maxDays;
    }
    
    /**
     * get formatted tag with any colour codes
     */
    @NotNull
    public String getFormattedTag() {
        // this will be processed by the formatter later
        return requestedTag;
    }
    
    @Override
    public String toString() {
        return "CustomTagRequest{" +
            "id=" + id +
            ", player='" + playerName + '\'' +
            ", tag='" + requestedTag + '\'' +
            ", status=" + status +
            ", age=" + getAgeInDays() + " days" +
            '}';
    }
}