package dev.hxrry.hxprefix.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Event fired when a player's prefix is changed
 */
public class PrefixChangeEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final String oldPrefix;
    private String newPrefix;
    private boolean cancelled;
    
    public PrefixChangeEvent(@NotNull Player player, @Nullable String oldPrefix, @Nullable String newPrefix) {
        super(player);
        this.oldPrefix = oldPrefix;
        this.newPrefix = newPrefix;
        this.cancelled = false;
    }
    
    /**
     * Get the player's previous prefix
     * @return The old prefix or null if they had none
     */
    @Nullable
    public String getOldPrefix() {
        return oldPrefix;
    }
    
    /**
     * Get the player's new prefix
     * @return The new prefix or null if being removed
     */
    @Nullable
    public String getNewPrefix() {
        return newPrefix;
    }
    
    /**
     * Set the new prefix
     * Allows event listeners to modify the prefix before it's applied
     * @param newPrefix The new prefix
     */
    public void setNewPrefix(@Nullable String newPrefix) {
        this.newPrefix = newPrefix;
    }
    
    /**
     * Check if this is a prefix removal (new prefix is null)
     * @return true if the prefix is being removed
     */
    public boolean isRemoval() {
        return newPrefix == null;
    }
    
    /**
     * Check if this is setting an initial prefix (old prefix is null)
     * @return true if this is the first prefix being set
     */
    public boolean isInitial() {
        return oldPrefix == null && newPrefix != null;
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