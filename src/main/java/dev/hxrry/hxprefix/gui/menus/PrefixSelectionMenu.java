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
import java.util.List;

// rewritten in jul 26 for hxgui changes
public class PrefixSelectionMenu {

    private static final ItemStack FILLER =
        ItemBuilder.of(Material.LIGHT_GRAY_STAINED_GLASS_PANE).name(" ").build();
    
    private final HxPrefix plugin;
    private final Player player;
    private final String rank;
    
    public PrefixSelectionMenu(@NotNull HxPrefix plugin, @NotNull Player player) {
        this.plugin = plugin;
        this.player = player;
        this.rank = plugin.getLuckPermsHook() != null ?
            plugin.getLuckPermsHook().getPrimaryGroup(player) : "default";
    }
    
    public void open() {
        List<StyleOption> prefixes = new ArrayList<>(
            plugin.getConfigManager().getStyleConfig().getAvailablePrefixes(rank));

        HxMenu.create("<gold><bold>Prefix Selection")
            .type(MenuType.GENERIC_9X6)
            .layout("FFFFIFFFF",
                    "FCCCCCCCF",
                    "FCCCCCCCF",
                    "FCCCCCCCF",
                    "FCCCCCCCF",
                    "F<FFRFF>F")
            .item('F', FILLER)
            .item('I', headerItem(prefixes.size()))
            .items('C', prefixes, this::prefixItem, this::selectPrefix)
            .item('R', resetItem(), (p, click) -> resetPrefix(p))
            .nav('<', '>')
            .open(player);
    }

    private ItemBuilder headerItem(int prefixCount) {
        return ItemBuilder.of(Material.NAME_TAG)
            .name("<gold><bold>Available Prefixes")
            .lore("<gray>Rank: <white>" + rank,
                  "<gray>Available prefixes: <white>" + prefixCount,
                  "",
                  "<yellow>Click a prefix to apply!")
            .glow();
    }

    private ItemBuilder prefixItem(@NotNull StyleOption prefix) {
        // chat-style preview: [Prefix] Name » hello!
        String preview = prefix.getValue() + " <white>" + player.getName() + " <gray>» <white>hello!";

        return ItemBuilder.of(prefix.getMaterial())
            .name("<white>" + prefix.getDisplayName())
            .lore("<gray>Preview: " + preview, "", "<yellow>Click to apply!");
    }
    
    private ItemBuilder resetItem() {
        return ItemBuilder.of(Material.BARRIER)
            .name("<red><bold>Reset Prefix")
            .lore("<gray>Remove your current prefix",
                  "",
                  "<red>Click to reset!");
    }
    
    private void selectPrefix(@NotNull Player player, @NotNull StyleOption prefix) {
        if (plugin.getAPI().setPrefix(player, prefix.getValue())) {
            player.sendMessage(Colours.parse(
                plugin.getConfigManager().getMessagesConfig().getMessage("prefix.changed")));
            player.closeInventory();
        } else {
            player.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    private void resetPrefix(@NotNull Player player) {
        if (plugin.getAPI().setPrefix(player, null)) {
            player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            player.sendMessage(Colours.parse(
                plugin.getConfigManager().getMessagesConfig().getMessage("prefix.removed")));
            player.closeInventory();
        } else {
            player.playSound(player, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
}