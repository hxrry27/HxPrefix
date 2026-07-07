package dev.hxrry.hxprefix.commands;

import dev.hxrry.hxcore.commands.HxCommand;
import dev.hxrry.hxprefix.HxPrefix;
import dev.hxrry.hxprefix.api.models.StyleOption;
import dev.hxrry.hxprefix.gui.menus.SuffixSelectionMenu;

import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;


public class SuffixCommand extends BaseCommand {
    
    public SuffixCommand(@NotNull HxPrefix plugin) {
        super(plugin, "suffix", null, true);
    }

    public void register(HxPrefix plugin) {
        HxCommand.create("suffix")

            .executes(sender -> {
                if (!(sender instanceof Player player)) {
                    sendPlayerOnly(sender);
                    return;
                }
                if (!checkSuffixPermission(player)) return;
                openSuffixMenu(player);
            })
            .register(plugin);
    }
    
    private boolean checkSuffixPermission(@NotNull Player player) {
        if (!hasFeaturePermission(player, "suffix")) {
            sendMessage(player, "error.no-suffix-permission", 
                "{rank}", getPlayerRank(player));
            return false;
        }
        return true;
    }
    
    private void openSuffixMenu(@NotNull Player player) {
        new SuffixSelectionMenu(plugin, player).open();
    }
    
    private void setSuffix(@NotNull Player player, @NotNull String suffixInput) {
        // check if it's a valid suffix option
        List<StyleOption> available = plugin.getAPI().getAvailableSuffixes(player);
        
        StyleOption selected = null;
        for (StyleOption option : available) {
            // for suffixes, also match by the actual value (like ★ or ✦)
            String value = stripColourCodes(option.getValue());
            if (option.getId().equalsIgnoreCase(suffixInput) ||
                option.getDisplayName().equalsIgnoreCase(suffixInput) ||
                value.equalsIgnoreCase(suffixInput)) {
                selected = option;
                break;
            }
        }
        
        if (selected == null) {
            // check if they're trying to use an emoji/symbol directly
            if (suffixInput.length() <= 2 && isSymbol(suffixInput)) {
                // check if any rank allows custom symbols
                if (hasFeaturePermission(player, "custom-suffix")) {
                    // allow custom symbol
                    if (plugin.getAPI().setSuffix(player, suffixInput)) {
                        sendSuccess(player, "suffix set to " + suffixInput);
                    } else {
                        sendError(player, "failed to set suffix");
                    }
                    return;
                }
            }
            
            sendError(player, "suffix '" + suffixInput + "' not available for your rank");
            send(player, "<gray>use /hxsuffix to see available options");
            return;
        }
        
        // set the suffix
        if (plugin.getAPI().setSuffix(player, selected.getValue())) {
            sendMessage(player, "suffix.changed", 
                "{suffix}", stripColourCodes(selected.getValue()));
        } else {
            sendError(player, "failed to set suffix");
        }
    }
    
    /**
     * remove player's suffix
     */
    private void removeSuffix(@NotNull Player player) {
        if (plugin.getAPI().setSuffix(player, null)) {
            sendMessage(player, "suffix.removed");
        } else {
            sendError(player, "failed to remove suffix");
        }
    }
    
    /**
     * get available suffix names for tab completion
     */
    private List<String> getAvailableSuffixes(@NotNull Player player) {
        List<String> suffixes = plugin.getAPI().getAvailableSuffixes(player)
            .stream()
            .map(opt -> {
                // for symbols, return the actual symbol
                String value = stripColourCodes(opt.getValue());
                if (value.length() <= 2) {
                    return value;
                }
                // for text suffixes, return the display name
                return opt.getDisplayName().toLowerCase();
            })
            .distinct()
            .collect(Collectors.toList());
        
        // add special options
        suffixes.add("off");
        suffixes.add("reset");
        suffixes.add("none");
        
        return suffixes;
    }
    
    /**
     * strip colour codes from a string
     */
    private String stripColourCodes(@NotNull String input) {
        // strip minimessage tags
        String stripped = input.replaceAll("<[^>]+>", "");
        // strip legacy codes
        stripped = stripped.replaceAll("&[0-9a-fk-or]", "");
        // strip section symbols
        stripped = stripped.replaceAll("§[0-9a-fk-or]", "");
        return stripped.trim();
    }
    
    /**
     * check if input is a symbol (emoji or special character)
     */
    private boolean isSymbol(@NotNull String input) {
        // check if it's an emoji or special character
        // this is a simple check, could be expanded
        return input.matches("[^a-zA-Z0-9\\s]+");
    }
}