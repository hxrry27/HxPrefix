package dev.hxrry.hxprefix.listeners;

import dev.hxrry.hxprefix.HxPrefix;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.TabCompleteEvent;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * listener for command-related events
 */
public class CommandListener implements Listener {
    @SuppressWarnings("unused")
    private final HxPrefix plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    
    // redirect the OLD hx commands to the NEW simple ones
    private static final List<String> OLD_HX_COMMANDS = Arrays.asList(
        "/hxcolour", "/hxcolor", "/hxprefix", "/hxsuffix", "/hxnick", "/hxtag", "/hxadmin"
    );
    
    // other old command variations that should redirect to new ones
    private static final List<String> OLD_COLOUR_VARIANTS = Arrays.asList(
        "/colors", "/namecolor", "/namecolors", "/setcolor", "/namecolour", "/namecolours"
    );
    
    private static final List<String> OLD_PREFIX_VARIANTS = Arrays.asList(
        "/setprefix", "/customprefix", "/tag", "/tags", "/prefixes"
    );
    
    private static final List<String> OLD_NICK_VARIANTS = Arrays.asList(
        "/nickname", "/setnick", "/name", "/setname"
    );
    
    private static final List<String> OLD_SUFFIX_VARIANTS = Arrays.asList(
        "/setsuffix", "/suffixes"
    );
    
    public CommandListener(@NotNull HxPrefix plugin) {
        this.plugin = plugin;
    }
    
    /**
     * handle command preprocessing for old commands
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommandPreprocess(@NotNull PlayerCommandPreprocessEvent event) {
        String message = event.getMessage().toLowerCase();
        Player player = event.getPlayer();
        
        // check if it's an old command
        String command = message.split(" ")[0];
        String args = message.length() > command.length() ? message.substring(command.length()) : "";
        
        // handle the old hx commands redirecting to new simple ones (altho hx commands were never *officially* released so..)
        switch (command) {
            case "/hxcolour", "/hxcolor" -> {
                event.setMessage("/colour" + args);
                return;
            }
            case "/hxsuffix" -> {
                event.setMessage("/suffix" + args);
                return;
            }
            case "/hxnick" -> {
                event.setMessage("/nick" + args);
                return;
            }
            case "/hxtag" -> {
                // tags are now handled through prefix command
                event.setCancelled(true);
                suggestNewCommand(player, "prefix", "custom tag requests");
                return;
            }
            case "/hxadmin" -> {
                // admin commands now use /hxprefix
                event.setMessage("/hxprefix" + args);
                return;
            }
        }
        
        // handle /color -> /colour (silly americans)
        if (command.equals("/color")) {
            event.setMessage("/colour" + args);
            return;
        }
        
        // (maybe not needed anymore?) 
        // handle other old command variants
        if (OLD_COLOUR_VARIANTS.contains(command)) {
            event.setCancelled(true);
            suggestNewCommand(player, "colour", "colour selection");
            return;
        }
        
        if (OLD_PREFIX_VARIANTS.contains(command)) {
            event.setCancelled(true);
            suggestNewCommand(player, "prefix", "prefix selection");
            return;
        }
        
        if (OLD_NICK_VARIANTS.contains(command)) {
            event.setCancelled(true);
            suggestNewCommand(player, "nick", "nickname management");
            return;
        }
        
        if (OLD_SUFFIX_VARIANTS.contains(command)) {
            event.setCancelled(true);
            suggestNewCommand(player, "suffix", "suffix selection");
            return;
        }
    }
    
    /**
     * suggest new command to player
     */
    private void suggestNewCommand(@NotNull Player player, @NotNull String newCommand, 
                                   @NotNull String description) {
        Component message = mm.deserialize(
            "<gray>That command has been replaced! Use <yellow>/" + newCommand + 
            " <gray>for " + description + "."
        );
        
        // make it clickable
        message = message.clickEvent(ClickEvent.runCommand("/" + newCommand));
        
        player.sendMessage(message);
    }
    
    /**
     * handle tab completion for better suggestions
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onTabComplete(@NotNull TabCompleteEvent event) {
        String buffer = event.getBuffer().toLowerCase();
        
        // add NEW commands to root tab completion
        if (buffer.equals("/") || buffer.equals("/ ")) {
            List<String> completions = event.getCompletions();
            
            // add simplified commands if player has permission
            if (event.getSender().hasPermission("hxprefix.use")) {
                completions.add("prefix");
                completions.add("suffix");
                completions.add("colour");
                completions.add("color");  // silly american spelling alias
                completions.add("nick");
            }
            
            if (event.getSender().hasPermission("hxprefix.admin")) {
                completions.add("hxprefix");  // admin command
            }
            
            event.setCompletions(completions);
        }
    }
}