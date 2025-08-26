package dev.hxrry.hxprefix.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Event fired when a player's name colour is changed
 */
public class ColourChangeEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final String oldColour;
    private String newColour;
    private boolean cancelled;
    
    public ColourChangeEvent(@NotNull Player player, @Nullable String oldColour, @Nullable String newColour) {
        super(player);
        this.oldColour = oldColour;
        this.newColour = newColour;
        this.cancelled = false;
    }
    
    /**
     * Get the player's previous colour
     * @return The old colour in MiniMessage format or null if they had none
     */
    @Nullable
    public String getOldColour() {
        return oldColour;
    }
    
    /**
     * Get the player's new colour
     * @return The new colour in MiniMessage format or null if being removed
     */
    @Nullable
    public String getNewColour() {
        return newColour;
    }
    
    /**
     * Set the new colour
     * Allows event listeners to modify the colour before it's applied
     * @param newColour The new colour in MiniMessage format
     */
    public void setNewColour(@Nullable String newColour) {
        this.newColour = newColour;
    }
    
    /**
     * Check if this is a colour removal (new colour is null)
     * @return true if the colour is being removed
     */
    public boolean isRemoval() {
        return newColour == null;
    }
    
    /**
     * Check if this is setting an initial colour (old colour is null)
     * @return true if this is the first colour being set
     */
    public boolean isInitial() {
        return oldColour == null && newColour != null;
    }
    
    /**
     * Check if this is a gradient colour
     * @return true if the new colour is a gradient
     */
    public boolean isGradient() {
        return newColour != null && newColour.contains("gradient:");
    }
    
    /**
     * Check if this is a rainbow colour
     * @return true if the new colour is rainbow
     */
    public boolean isRainbow() {
        return newColour != null && newColour.contains("rainbow");
    }
    
    /**
     * Check if this is a solid colour
     * @return true if the new colour is a solid hex/named colour
     */
    public boolean isSolid() {
        return newColour != null && !isGradient() && !isRainbow();
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