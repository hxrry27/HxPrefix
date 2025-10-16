package dev.hxrry.hxprefix.commands;

import dev.hxrry.hxprefix.HxPrefix;
import dev.hxrry.hxprefix.api.models.StyleOption;
import dev.hxrry.hxprefix.gui.menus.ColourSelectionMenu;

import io.papermc.paper.command.brigadier.Commands;

import com.mojang.brigadier.arguments.StringArgumentType;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * command for managing name colours
 */
public class ColourCommand extends BaseCommand {
    
    public ColourCommand(@NotNull HxPrefix plugin) {
        super(plugin, "colour", null, true); // no base permission, checked per-rank
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
                    if (!checkColourPermission(player)) return 0;
                    
                    openColourMenu(player);
                    return 1;
                })
                .then(Commands.argument("colour", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        CommandSender sender = ctx.getSource().getSender();
                        if (sender instanceof Player player) {
                            // suggest available colours
                            getAvailableColours(player).forEach(builder::suggest);
                        }
                        return builder.buildFuture();
                    })
                    .executes(ctx -> {
                        CommandSender sender = ctx.getSource().getSender();
                        if (!checkSender(sender)) return 0;
                        
                        Player player = getPlayer(sender);
                        if (!checkColourPermission(player)) return 0;
                        
                        String colour = ctx.getArgument("colour", String.class);
                        
                        // special cases
                        if (colour.equalsIgnoreCase("off") || 
                            colour.equalsIgnoreCase("reset") || 
                            colour.equalsIgnoreCase("remove")) {
                            removeColour(player);
                            return 1;
                        }
                        
                        // try to set the colour
                        setColour(player, colour);
                        return 1;
                    })
                )
                .build()
        );
    }
    
    /**
     * check if player has permission to use colours
     */
    private boolean checkColourPermission(@NotNull Player player) {
        if (!hasFeaturePermission(player, "colour")) {
            sendMessage(player, "error.no-colour-permission", 
                "{rank}", getPlayerRank(player));
            return false;
        }
        return true;
    }
    
    /**
     * open the colour selection menu
     */
    private void openColourMenu(@NotNull Player player) {
        new ColourSelectionMenu(plugin, player).open();
    }
    
    /**
     * set a colour directly
     */
    private void setColour(@NotNull Player player, @NotNull String colourInput) {
        // check if it's a valid colour option
        List<StyleOption> available = plugin.getAPI().getAvailableColours(player);
        
        StyleOption selected = null;
        for (StyleOption option : available) {
            // check by id or display name
            if (option.getId().equalsIgnoreCase(colourInput) ||
                option.getDisplayName().equalsIgnoreCase(colourInput)) {
                selected = option;
                break;
            }
        }
        
        if (selected == null) {
            // try as a hex colour if they have permission
            if (colourInput.startsWith("#") && hasFeaturePermission(player, "custom-colour")) {
                if (!colourInput.matches("^#[A-Fa-f0-9]{6}$")) {
                    sendError(player, "invalid hex colour format. use #RRGGBB");
                    return;
                }
                
                // set custom hex colour
                if (plugin.getAPI().setNameColour(player, "<color:" + colourInput + ">")) {
                    sendSuccess(player, "colour set to " + colourInput);
                } else {
                    sendError(player, "failed to set colour");
                }
            } else {
                sendError(player, "colour '" + colourInput + "' not available for your rank");
                send(player, "<gray>use /hxcolour to see available options");
            }
            return;
        }
        
        // set the colour
        if (plugin.getAPI().setNameColour(player, selected.getValue())) {
            sendMessage(player, "colour.changed", 
                "{colour}", selected.getDisplayName());
        } else {
            sendError(player, "failed to set colour");
        }
    }
    
    /**
     * remove player's colour
     */
    private void removeColour(@NotNull Player player) {
        if (plugin.getAPI().setNameColour(player, null)) {
            sendMessage(player, "colour.removed");
        } else {
            sendError(player, "failed to remove colour");
        }
    }
    
    /**
     * get available colour names for tab completion
     */
    private List<String> getAvailableColours(@NotNull Player player) {
        List<String> colours = plugin.getAPI().getAvailableColours(player)
            .stream()
            .map(opt -> opt.getId().toLowerCase())
            .collect(Collectors.toList());
        
        // add special options
        colours.add("off");
        colours.add("reset");
        
        // add hex example if they have custom permission
        if (hasFeaturePermission(player, "custom-colour")) {
            colours.add("#");
        }
        
        return colours;
    }
}