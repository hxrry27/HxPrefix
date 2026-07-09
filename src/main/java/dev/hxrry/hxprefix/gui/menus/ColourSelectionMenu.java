package dev.hxrry.hxprefix.gui.menus;

import dev.hxrry.hxcore.text.Colours;
import dev.hxrry.hxgui.HxMenu;
import dev.hxrry.hxgui.builders.ItemBuilder;

import dev.hxrry.hxprefix.HxPrefix;
import dev.hxrry.hxprefix.api.models.StyleOption;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// rewritten in jul 26 using hxgui changes on the api
public class ColourSelectionMenu {

    private static final ItemStack FILLER =
        ItemBuilder.of(Material.LIGHT_GRAY_STAINED_GLASS_PANE).name(" ").build();

    private final HxPrefix plugin;
    private final Player player;
    private final String rank;

    public ColourSelectionMenu(@NotNull HxPrefix plugin, @NotNull Player player) {
        this.plugin = plugin;
        this.player = player;
        this.rank = plugin.getLuckPermsHook() != null ?
            plugin.getLuckPermsHook().getPrimaryGroup(player) : "default";
    }

    public void open() {
        List<StyleOption> colours = new ArrayList<>(
            plugin.getConfigManager().getStyleConfig().getAvailableColours(rank));

        // keep the old visual grouping without the header items
        colours.sort(Comparator.comparingInt(c -> switch (c.getType()) {
            case COLOUR_SOLID -> 0;
            case COLOUR_GRADIENT -> 1;
            case COLOUR_SPECIAL -> 2;
            default -> 3;
        }));

        HxMenu.create("<gold><bold>Colour Selection")
            .type(MenuType.GENERIC_9X6)
            .layout("FFFFIFFFF",
                    "FCCCCCCCF",
                    "FCCCCCCCF",
                    "FCCCCCCCF",
                    "FCCCCCCCF",
                    "F<FFRFF>F")
            .item('F', FILLER)
            .item('I', headerItem(colours.size()))
            .items('C', colours, this::colourItem, this::selectColour)
            .item('R', resetItem(), (p, click) -> resetColour(p))
            .nav('<', '>')
            .open(player);
    }

    private ItemBuilder headerItem(int colourCount) {
        return ItemBuilder.of(Material.PAINTING)
            .name("<gold><bold>Available Colours")
            .lore("<gray>Rank: <white>" + rank,
                  "<gray>Available colours: <white>" + colourCount,
                  "",
                  "<yellow>Click a colour to apply!")
            .glow();
    }

    private ItemBuilder colourItem(@NotNull StyleOption colour) {
        ItemBuilder item = ItemBuilder.of(colour.getMaterial());

        // solid colours show their name in their own colour
        if (colour.getType() == StyleOption.Type.COLOUR_SOLID) {
            item.name(colour.getValue() + colour.getDisplayName());
        } else {
            item.name(colour.getDisplayName());
        }

        if (colour.getDescription() != null) {
            for (String line : colour.getDescription()) {
                item.lore("<gray>" + line);
            }
        }

        String preview = plugin.getConfigManager().getStyleConfig()
            .formatWithColour(colour.getValue(), player.getName());
        item.lore("", "<gray>Preview: " + preview, "", "<yellow>Click to apply!");

        if (colour.hasGlow()) {
            item.glow();
        }

        return item;
    }

    private ItemBuilder resetItem() {
        return ItemBuilder.of(Material.BARRIER)
            .name("<red><bold>Reset Colour")
            .lore("<gray>Remove your current colour",
                  "",
                  "<red>Click to reset!");
    }

    private void selectColour(@NotNull Player player, @NotNull StyleOption colour) {
        if (plugin.getAPI().setNameColour(player, colour.getValue())) {
            player.sendMessage(Colours.parse(
                plugin.getConfigManager().getMessagesConfig().getMessage("colour.changed")));
            player.closeInventory();
        } else {
            player.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    private void resetColour(@NotNull Player player) {
        if (plugin.getAPI().setNameColour(player, null)) {
            player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            player.sendMessage(Colours.parse(
                plugin.getConfigManager().getMessagesConfig().getMessage("colour.removed")));
            player.closeInventory();
        } else {
            player.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
}
