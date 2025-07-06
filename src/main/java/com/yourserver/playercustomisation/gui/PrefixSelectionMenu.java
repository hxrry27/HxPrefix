package com.yourserver.playercustomisation.gui;

import com.yourserver.playercustomisation.PlayerCustomisation;
import org.bukkit.entity.Player;

/**
 * Menu for selecting prefixes
 * TODO: Implement in Phase 2
 */
public class PrefixSelectionMenu extends AbstractMenu {
    
    public PrefixSelectionMenu(PlayerCustomisation plugin, Player player, String rank) {
        super(plugin, player, rank, "&d&lSelect Prefix", 54);
    }
    
    @Override
    protected void build() {
        // TODO: Implement prefix selection menu
        // - Load available prefixes from config
        // - Check whitelist if applicable
        // - Apply player's color to preview
        // - Add reset button
        
        // Placeholder for now
        fillEmpty();
    }
}