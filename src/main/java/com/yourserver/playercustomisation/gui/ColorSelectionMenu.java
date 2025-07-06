package com.yourserver.playercustomisation.gui;

import com.yourserver.playercustomisation.PlayerCustomisation;
import org.bukkit.entity.Player;

/**
 * Menu for selecting name colors
 * TODO: Implement in Phase 2
 */
public class ColorSelectionMenu extends AbstractMenu {
    
    public ColorSelectionMenu(PlayerCustomisation plugin, Player player, String rank) {
        super(plugin, player, rank, "&6&lSelect Name Color", 54);
    }
    
    @Override
    protected void build() {
        // TODO: Implement color selection menu
        // - Load available colors from config
        // - Show solid colors
        // - Show gradients if rank allows
        // - Show rainbow if rank allows
        // - Add reset button
        
        // Placeholder for now
        fillEmpty();
    }
}