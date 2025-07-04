package com.prefix27.customization.managers;

import com.prefix27.customization.PlayerCustomizationPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatInputManager implements Listener {
    
    private final PlayerCustomizationPlugin plugin;
    private final Map<UUID, ChatInputType> awaitingInput;
    
    public enum ChatInputType {
        CUSTOM_PREFIX_REQUEST
    }
    
    public ChatInputManager(PlayerCustomizationPlugin plugin) {
        this.plugin = plugin;
        this.awaitingInput = new HashMap<>();
    }
    
    public void requestCustomPrefixInput(Player player) {
        awaitingInput.put(player.getUniqueId(), ChatInputType.CUSTOM_PREFIX_REQUEST);
        player.sendMessage("§6§l┌─────────────────────────────────┐");
        player.sendMessage("§6§l│    §e§lCUSTOM PREFIX REQUEST    §6§l│");
        player.sendMessage("§6§l├─────────────────────────────────┤");
        player.sendMessage("§6§l│ §7Enter your desired prefix:    §6§l│");
        player.sendMessage("§6§l│ §7- Max 16 characters           §6§l│");
        player.sendMessage("§6§l│ §7- No offensive content        §6§l│");
        player.sendMessage("§6§l│ §7- Staff will review it        §6§l│");
        player.sendMessage("§6§l│ §7Type 'cancel' to abort        §6§l│");
        player.sendMessage("§6§l└─────────────────────────────────┘");
    }
    
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        
        ChatInputType inputType = awaitingInput.get(playerUUID);
        if (inputType == null) {
            return; // Not waiting for input from this player
        }
        
        // Cancel the chat event to prevent the message from being broadcast
        event.setCancelled(true);
        
        String input = event.getMessage().trim();
        
        // Handle cancellation
        if (input.equalsIgnoreCase("cancel")) {
            awaitingInput.remove(playerUUID);
            player.sendMessage("§cCustom prefix request cancelled.");
            return;
        }
        
        switch (inputType) {
            case CUSTOM_PREFIX_REQUEST:
                handleCustomPrefixRequest(player, input);
                break;
        }
        
        awaitingInput.remove(playerUUID);
    }
    
    private void handleCustomPrefixRequest(Player player, String prefixText) {
        // Validate the prefix text
        if (!plugin.getPrefixManager().isValidCustomPrefix(prefixText)) {
            player.sendMessage("§c§l✗ Invalid prefix!");
            player.sendMessage("§7Prefix must be 1-16 characters and contain no forbidden words.");
            player.sendMessage("§7Forbidden words: " + String.join(", ", plugin.getConfig().getStringList("custom_prefixes.forbidden_words")));
            return;
        }
        
        // Check cooldown
        if (!plugin.getPrefixManager().canRequestCustomPrefix(player.getUniqueId())) {
            player.sendMessage("§c§l✗ You're on cooldown!");
            player.sendMessage("§7You can only request a custom prefix once every 30 days.");
            return;
        }
        
        // Submit the request
        plugin.getPrefixManager().requestCustomPrefix(player.getUniqueId(), prefixText);
        
        player.sendMessage("§a§l✓ Custom prefix request submitted!");
        player.sendMessage("§7Requested prefix: §f[" + prefixText + "]");
        player.sendMessage("§7Status: §ePending staff review");
        player.sendMessage("§7You will be notified when it's reviewed.");
        
        // Notify staff
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.getServer().getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("customization.admin"))
                .forEach(staff -> {
                    staff.sendMessage("§6§l[ADMIN] §7New custom prefix request from §e" + player.getName());
                    staff.sendMessage("§7Requested: §f[" + prefixText + "]");
                    staff.sendMessage("§7Use /customization admin to review requests.");
                });
        });
    }
    
    public boolean isAwaitingInput(Player player) {
        return awaitingInput.containsKey(player.getUniqueId());
    }
    
    public void cancelInput(Player player) {
        awaitingInput.remove(player.getUniqueId());
    }
    
    public void reload() {
        awaitingInput.clear();
    }
}