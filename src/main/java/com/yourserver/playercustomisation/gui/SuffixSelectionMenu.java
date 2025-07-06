package com.yourserver.playercustomisation.gui;

import com.yourserver.playercustomisation.PlayerCustomisation;
import org.bukkit.entity.Player;

/**
 * Menu for selecting suffixes
 * TODO: Implement in Phase 2
 */
public class SuffixSelectionMenu extends AbstractMenu {
    
    public SuffixSelectionMenu(PlayerCustomisation plugin, Player player, String rank) {
        super(plugin, player, rank, "&b&lSelect Suffix", 54);
    }
    
    @Override
    protected void build() {
        // TODO: Implement suffix selection menu
        // - Load available suffixes from config
        // - Check whitelist if applicable
        // - Apply player's color to preview
        // - Add reset button
        
        // Placeholder for now
        fillEmpty();
    }
}