package com.prefix27.customization.gui;

import com.prefix27.customization.PlayerCustomizationPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

public abstract class CustomizationGUI {
    
    protected final PlayerCustomizationPlugin plugin;
    protected final Player player;
    protected Inventory inventory;
    protected boolean temporaryClose = false;
    
    public CustomizationGUI(PlayerCustomizationPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }
    
    public abstract void open();
    
    public final void handleClick(InventoryClickEvent event) {
        // CRITICAL: Always cancel to prevent item theft
        event.setCancelled(true);
        onInventoryClick(event);
    }
    
    protected abstract void onInventoryClick(InventoryClickEvent event);
    public abstract void handleClose(InventoryCloseEvent event);
    
    public void close() {
        if (player != null && inventory != null) {
            player.closeInventory();
        }
    }
    
    public boolean isTemporaryClose() {
        return temporaryClose;
    }
    
    public void setTemporaryClose(boolean temporaryClose) {
        this.temporaryClose = temporaryClose;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public Inventory getInventory() {
        return inventory;
    }
    
    protected void playSound(String soundName) {
        try {
            org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            // Sound not found, ignore
        }
    }
    
    protected void playClickSound() {
        String soundName = plugin.getConfig().getString("gui.sounds.click", "UI_BUTTON_CLICK");
        playSound(soundName);
    }
    
    protected void playSuccessSound() {
        String soundName = plugin.getConfig().getString("gui.sounds.success", "ENTITY_EXPERIENCE_ORB_PICKUP");
        playSound(soundName);
    }
    
    protected void playErrorSound() {
        String soundName = plugin.getConfig().getString("gui.sounds.error", "ENTITY_VILLAGER_NO");
        playSound(soundName);
    }
    
    protected void spawnSuccessParticles() {
        String particleName = plugin.getConfig().getString("gui.particles.success", "VILLAGER_HAPPY");
        try {
            org.bukkit.Particle particle = org.bukkit.Particle.valueOf(particleName);
            player.spawnParticle(particle, player.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0.1);
        } catch (IllegalArgumentException e) {
            // Particle not found, ignore
        }
    }
    
    protected void spawnApplyChangeParticles() {
        String particleName = plugin.getConfig().getString("gui.particles.apply_change", "ENCHANTMENT_TABLE");
        try {
            org.bukkit.Particle particle = org.bukkit.Particle.valueOf(particleName);
            player.spawnParticle(particle, player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
        } catch (IllegalArgumentException e) {
            // Particle not found, ignore
        }
    }
}