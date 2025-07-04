package com.yourserver.playercustomisation.models;

import java.util.UUID;

public class TagRequest {
    private int id;
    private UUID uuid;
    private String username;
    private String requestedTag;
    private String status;
    private UUID reviewedBy;
    private long requestedAt;
    private long reviewedAt;

    public TagRequest(UUID uuid, String username, String requestedTag) {
        this.uuid = uuid;
        this.username = username;
        this.requestedTag = requestedTag;
        this.status = "pending";
        this.requestedAt = System.currentTimeMillis();
    }

    // Getters
    public int getId() { return id; }
    public UUID getUuid() { return uuid; }
    public String getUsername() { return username; }
    public String getRequestedTag() { return requestedTag; }
    public String getStatus() { return status; }
    public UUID getReviewedBy() { return reviewedBy; }
    public long getRequestedAt() { return requestedAt; }
    public long getReviewedAt() { return reviewedAt; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setStatus(String status) { this.status = status; }
    public void setReviewedBy(UUID reviewedBy) { this.reviewedBy = reviewedBy; }
    public void setReviewedAt(long reviewedAt) { this.reviewedAt = reviewedAt; }
}