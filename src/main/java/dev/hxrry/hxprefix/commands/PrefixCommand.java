package dev.hxrry.hxprefix.commands;

import dev.hxrry.hxprefix.HxPrefix;
import dev.hxrry.hxprefix.api.models.StyleOption;
import dev.hxrry.hxprefix.gui.menus.PrefixSelectionMenu;

import io.papermc.paper.command.brigadier.Commands;

import com.mojang.brigadier.arguments.StringArgumentType;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * command for managing prefixes
 */
public class PrefixCommand extends BaseCommand {
    
    public PrefixCommand(@NotNull HxPrefix plugin) {
        super(plugin, "prefix", null, true);
    }
    
    @Override
    public void register(@NotNull Commands commands) {
        commands.register(
            Commands.literal(name)
                .executes(ctx -> {
                    // open gui with no args
                    CommandSender sender = ctx.getSource().getSender();
                    if (!checkSender(sender)) return 0;
                    
                    Player player = getPlayer(sender);
                    if (!checkPrefixPermission(player)) return 0;
                    
                    openPrefixMenu(player);
                    return 1;
                })
                .then(Commands.argument("prefix", StringArgumentType.greedyString())
                    .suggests((ctx, builder) -> {
                        CommandSender sender = ctx.getSource().getSender();
                        if (sender instanceof Player player) {
                            // suggest available prefixes
                            getAvailablePrefixes(player).forEach(builder::suggest);
                        }
                        return builder.buildFuture();
                    })
                    .executes(ctx -> {
                        CommandSender sender = ctx.getSource().getSender();
                        if (!checkSender(sender)) return 0;
                        
                        Player player = getPlayer(sender);
                        if (!checkPrefixPermission(player)) return 0;
                        
                        String prefix = ctx.getArgument("prefix", String.class);
                        
                        // special cases
                        if (prefix.equalsIgnoreCase("off") || 
                            prefix.equalsIgnoreCase("reset") || 
                            prefix.equalsIgnoreCase("remove") ||
                            prefix.equalsIgnoreCase("none")) {
                            removePrefix(player);
                            return 1;
                        }
                        
                        // try to set the prefix
                        setPrefix(player, prefix);
                        return 1;
                    })
                )
                .build()
        );
    }
    
    /**
     * check if player has permission to use prefixes
     */
    private boolean checkPrefixPermission(@NotNull Player player) {
        if (!hasFeaturePermission(player, "prefix")) {
            sendMessage(player, "error.no-prefix-permission", 
                "{rank}", getPlayerRank(player));
            return false;
        }
        return true;
    }
    
    /**
     * open the prefix selection menu
     */
    private void openPrefixMenu(@NotNull Player player) {
        new PrefixSelectionMenu(plugin, player).open();
    }
    
    /**
     * set a prefix directly
     */
    private void setPrefix(@NotNull Player player, @NotNull String prefixInput) {
        // check if it's a valid prefix option
        List<StyleOption> available = plugin.getAPI().getAvailablePrefixes(player);
        
        StyleOption selected = null;
        for (StyleOption option : available) {
            // check by id or display name (case insensitive)
            if (option.getId().equalsIgnoreCase(prefixInput) ||
                option.getDisplayName().equalsIgnoreCase(prefixInput)) {
                selected = option;
                break;
            }
        }
        
        if (selected == null) {
            // check if they're trying to use a custom tag
            if (hasFeaturePermission(player, "custom-tags")) {
                sendError(player, "prefix '" + prefixInput + "' not found");
                send(player, "<gray>use /hxtag <name> to request a custom prefix");
            } else {
                sendError(player, "prefix '" + prefixInput + "' not available for your rank");
                send(player, "<gray>use /hxprefix to see available options");
            }
            return;
        }
        
        // check if it's seasonal and currently active
        if (selected.isSeasonal()) {
            if (!isSeasonalActive(selected)) {
                sendError(player, "this prefix is not currently available (seasonal)");
                return;
            }
        }
        
        // set the prefix
        if (plugin.getAPI().setPrefix(player, selected.getValue())) {
            sendMessage(player, "prefix.changed", 
                "{prefix}", selected.getDisplayName());
        } else {
            sendError(player, "failed to set prefix");
        }
    }
    
    /**
     * remove player's prefix
     */
    private void removePrefix(@NotNull Player player) {
        if (plugin.getAPI().setPrefix(player, null)) {
            sendMessage(player, "prefix.removed");
        } else {
            sendError(player, "failed to remove prefix");
        }
    }
    
    /**
     * get available prefix names for tab completion
     */
    private List<String> getAvailablePrefixes(@NotNull Player player) {
        List<String> prefixes = plugin.getAPI().getAvailablePrefixes(player)
            .stream()
            .filter(opt -> !opt.isSeasonal() || isSeasonalActive(opt))
            .map(opt -> opt.getDisplayName().toLowerCase())
            .collect(Collectors.toList());
        
        // add special options
        prefixes.add("off");
        prefixes.add("reset");
        prefixes.add("none");
        
        return prefixes;
    }
    
    /**
     * check if a seasonal option is currently active
     */
    private boolean isSeasonalActive(@NotNull StyleOption option) {
        if (option.getMetadata() == null) return true;
        
        Object seasonal = option.getMetadata().get("seasonal");
        if (!(seasonal instanceof String)) return true;
        
        String season = (String) seasonal;
        // check against current date/events
        // this would be loaded from config
        return plugin.getConfigManager().getMainConfig()
            .getBoolean("events." + season + ".active", false);
    }
}