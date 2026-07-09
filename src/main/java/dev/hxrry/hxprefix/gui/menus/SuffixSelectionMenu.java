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

// rebuilt in hxgui as of jul 26 for new api scheme
public class SuffixSelectionMenu {

    private static final ItemStack FILLER =
        ItemBuilder.of(Material.LIGHT_GRAY_STAINED_GLASS_PANE).name(" ").build();

    private final HxPrefix plugin;
    private final Player player;
    private final String rank;

    public SuffixSelectionMenu(@NotNull HxPrefix plugin, @NotNull Player player) {
        this.plugin = plugin;
        this.player = player;
        this.rank = plugin.getLuckPermsHook() != null ?
            plugin.getLuckPermsHook().getPrimaryGroup(player) : "default";
    }

    public void open() {
        List<StyleOption> suffixes = new ArrayList<>(
            plugin.getConfigManager().getStyleConfig().getAvailableSuffixes(rank));

        // keep the old visual grouping without the header items: symbols first
        suffixes.sort(Comparator.comparingInt(s -> isSymbol(s) ? 0 : 1));

        HxMenu.create("<gold><bold>Suffix Selection")
            .type(MenuType.GENERIC_9X6)
            .layout("FFFFIFFFF",
                    "FSSSSSSSF",
                    "FSSSSSSSF",
                    "FSSSSSSSF",
                    "FSSSSSSSF",
                    "F<FFRFF>F")
            .item('F', FILLER)
            .item('I', headerItem(suffixes.size()))
            .items('S', suffixes, this::suffixItem, this::selectSuffix)
            .item('R', resetItem(), (p, click) -> resetSuffix(p))
            .nav('<', '>')
            .open(player);
    }

    // symbols are short non-letter values like stars and hearts
    private boolean isSymbol(@NotNull StyleOption suffix) {
        String plain = Colours.strip(suffix.getValue());
        return plain.length() <= 2 && !plain.matches("[a-zA-Z]+");
    }

    private ItemBuilder headerItem(int suffixCount) {
        return ItemBuilder.of(Material.PAPER)
            .name("<gold><bold>Available Suffixes")
            .lore("<gray>Rank: <white>" + rank,
                  "<gray>Available suffixes: <white>" + suffixCount,
                  "",
                  "<yellow>Click a suffix to apply!")
            .glow();
    }

    private ItemBuilder suffixItem(@NotNull StyleOption suffix) {
        boolean symbol = isSymbol(suffix);
        ItemBuilder item = ItemBuilder.of(suffix.getMaterial())
            .name(symbol
                ? suffix.getValue() + " <white>" + suffix.getDisplayName()
                : "<white>" + suffix.getDisplayName())
            .lore("<gray>Preview: <white>" + player.getName() + " " + suffix.getValue(),
                  symbol ? "<dark_gray>Symbol suffix" : "<dark_gray>Text suffix",
                  "",
                  "<yellow>Click to apply!");
        if (suffix.hasGlow()) {
            item.glow();
        }
        return item;
    }

    private ItemBuilder resetItem() {
        return ItemBuilder.of(Material.BARRIER)
            .name("<red><bold>Remove Suffix")
            .lore("<gray>Remove your current suffix",
                  "",
                  "<red>Click to remove!");
    }

    private void selectSuffix(@NotNull Player player, @NotNull StyleOption suffix) {
        if (plugin.getAPI().setSuffix(player, suffix.getValue())) {
            player.sendMessage(Colours.parse(
                plugin.getConfigManager().getMessagesConfig()
                    .getMessage("suffix.changed", "{suffix}", Colours.strip(suffix.getValue()))));
            player.closeInventory();
        } else {
            player.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    private void resetSuffix(@NotNull Player player) {
        if (plugin.getAPI().setSuffix(player, null)) {
            player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            player.sendMessage(Colours.parse(
                plugin.getConfigManager().getMessagesConfig().getMessage("suffix.removed")));
            player.closeInventory();
        } else {
            player.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
}