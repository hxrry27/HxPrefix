package dev.hxrry.hxprefix.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Event fired when a player's suffix is changed
 */
public class SuffixChangeEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final String oldSuffix;
    private String newSuffix;
    private boolean cancelled;
    
    public SuffixChangeEvent(@NotNull Player player, @Nullable String oldSuffix, @Nullable String newSuffix) {
        super(player);
        this.oldSuffix = oldSuffix;
        this.newSuffix = newSuffix;
        this.cancelled = false;
    }
    
    /**
     * Get the player's previous suffix
     * @return The old suffix or null if they had none
     */
    @Nullable
    public String getOldSuffix() {
        return oldSuffix;
    }
    
    /**
     * Get the player's new suffix
     * @return The new suffix or null if being removed
     */
    @Nullable
    public String getNewSuffix() {
        return newSuffix;
    }
    
    /**
     * Set the new suffix
     * Allows event listeners to modify the suffix before it's applied
     * @param newSuffix The new suffix
     */
    public void setNewSuffix(@Nullable String newSuffix) {
        this.newSuffix = newSuffix;
    }
    
    /**
     * Check if this is a suffix removal (new suffix is null)
     * @return true if the suffix is being removed
     */
    public boolean isRemoval() {
        return newSuffix == null;
    }
    
    /**
     * Check if this is setting an initial suffix (old suffix is null)
     * @return true if this is the first suffix being set
     */
    public boolean isInitial() {
        return oldSuffix == null && newSuffix != null;
    }
    
    /**
     * Check if this is a symbol suffix (single character/emoji)
     * @return true if the suffix is a single character or emoji
     */
    public boolean isSymbol() {
        if (newSuffix == null) return false;
        // Strip colour codes and check length
        String stripped = newSuffix.replaceAll("<[^>]+>", "").replaceAll("&[0-9a-fk-or]", "");
        return stripped.length() <= 2; // Account for some emojis being 2 chars
    }
    
    /**
     * Check if this is a text suffix (multiple characters)
     * @return true if the suffix is text rather than a symbol
     */
    public boolean isText() {
        return newSuffix != null && !isSymbol();
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }
    
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
    
    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    
    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}