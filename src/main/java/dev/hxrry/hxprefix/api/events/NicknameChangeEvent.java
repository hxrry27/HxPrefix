package dev.hxrry.hxprefix.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Event fired when a player's nickname is changed
 */
public class NicknameChangeEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final String oldNickname;
    private String newNickname;
    private boolean cancelled;
    private String cancelReason;
    
    public NicknameChangeEvent(@NotNull Player player, @Nullable String oldNickname, @Nullable String newNickname) {
        super(player);
        this.oldNickname = oldNickname;
        this.newNickname = newNickname;
        this.cancelled = false;
        this.cancelReason = null;
    }
    
    /**
     * Get the player's previous nickname
     * @return The old nickname or null if they had none
     */
    @Nullable
    public String getOldNickname() {
        return oldNickname;
    }
    
    /**
     * Get the player's new nickname
     * @return The new nickname or null if being removed
     */
    @Nullable
    public String getNewNickname() {
        return newNickname;
    }
    
    /**
     * Set the new nickname
     * Allows event listeners to modify the nickname before it's applied
     * @param newNickname The new nickname (without formatting)
     */
    public void setNewNickname(@Nullable String newNickname) {
        this.newNickname = newNickname;
    }
    
    /**
     * Check if this is a nickname removal (new nickname is null)
     * @return true if the nickname is being removed
     */
    public boolean isRemoval() {
        return newNickname == null;
    }
    
    /**
     * Check if this is setting an initial nickname (old nickname is null)
     * @return true if this is the first nickname being set
     */
    public boolean isInitial() {
        return oldNickname == null && newNickname != null;
    }
    
    /**
     * Check if the player is reverting to their real username
     * @return true if removing nickname to show real name
     */
    public boolean isRevertingToUsername() {
        return isRemoval();
    }
    
    /**
     * Get the display name that will be shown after this change
     * @return The display name (nickname or username)
     */
    @NotNull
    public String getResultingDisplayName() {
        if (newNickname != null) {
            return newNickname;
        }
        return getPlayer().getName();
    }
    
    /**
     * Check if the new nickname matches a pattern
     * @param pattern Regex pattern to check
     * @return true if the nickname matches
     */
    public boolean matchesPattern(@NotNull String pattern) {
        return newNickname != null && newNickname.matches(pattern);
    }
    
    /**
     * Check if the nickname length is within bounds
     * @param min Minimum length
     * @param max Maximum length
     * @return true if within bounds
     */
    public boolean isLengthValid(int min, int max) {
        if (newNickname == null) return true; // Removal is always valid
        return newNickname.length() >= min && newNickname.length() <= max;
    }
    
    /**
     * Get the reason for cancellation if cancelled
     * @return The cancel reason or null
     */
    @Nullable
    public String getCancelReason() {
        return cancelReason;
    }
    
    /**
     * Set a reason when cancelling the event
     * @param reason The reason for cancellation
     */
    public void setCancelReason(@Nullable String reason) {
        this.cancelReason = reason;
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }
    
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
    
    /**
     * Cancel the event with a reason
     * @param reason The reason for cancellation
     */
    public void cancel(@NotNull String reason) {
        this.cancelled = true;
        this.cancelReason = reason;
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