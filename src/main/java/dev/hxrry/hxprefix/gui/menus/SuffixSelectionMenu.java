package dev.hxrry.hxprefix.gui.menus;

import dev.hxrry.hxgui.core.MenuItem;
import dev.hxrry.hxgui.core.SharedMenu;

import dev.hxrry.hxprefix.HxPrefix;
import dev.hxrry.hxprefix.api.models.StyleOption;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * menu for selecting suffixes TODO: not currently being used in VSmp
 */
public class SuffixSelectionMenu extends SharedMenu {
    private final HxPrefix plugin;
    private final Player player;
    private final String rank;
    private final MiniMessage mm = MiniMessage.miniMessage();
    
    public SuffixSelectionMenu(@NotNull HxPrefix plugin, @NotNull Player player) {
        super(Component.text("Suffix Selection", NamedTextColor.AQUA, TextDecoration.BOLD),
              6);
        
        this.plugin = plugin;
        this.player = player;
        this.rank = plugin.getLuckPermsHook() != null ?
            plugin.getLuckPermsHook().getPrimaryGroup(player) : "default";
        
        build();
    }
    
    /**
     * build the menu contents
     */
    private void build() {
        // get available suffixes for rank
        List<StyleOption> suffixes = plugin.getConfigManager().getStyleConfig()
            .getAvailableSuffixes(rank);
        
        // separate symbols and text suffixes
        List<StyleOption> symbols = new ArrayList<>();
        List<StyleOption> textSuffixes = new ArrayList<>();
        
        for (StyleOption suffix : suffixes) {
            String value = stripColours(suffix.getValue());
            if (value.length() <= 2 && !value.matches("[a-zA-Z]+")) {
                symbols.add(suffix);
            } else {
                textSuffixes.add(suffix);
            }
        }
        
        // add headers
        if (!symbols.isEmpty()) {
            addSectionHeader(4, "Symbol Suffixes", Material.GOLD_NUGGET);
            displaySuffixes(symbols, 10, 25);
        }
        
        if (!textSuffixes.isEmpty()) {
            addSectionHeader(31, "Text Suffixes", Material.NAME_TAG);
            displaySuffixes(textSuffixes, 37, 43);
        }
        
        // add reset button
        addResetButton(49);
        
        // fill empty slots
        fillEmpty();
    }
    
    /**
     * display suffixes in a range
     */
    private void displaySuffixes(@NotNull List<StyleOption> suffixes, int startSlot, int endSlot) {
        int slot = startSlot;
        
        for (StyleOption suffix : suffixes) {
            if (slot > endSlot) break;
            
            // skip certain slots for visual layout
            if (slot == 17 || slot == 18 || slot == 26 || slot == 27) {
                slot = 19; // jump to next row
            }
            if (slot == 35 || slot == 36) {
                slot = 37; // jump to next section
            }
            
            addSuffixOption(slot, suffix);
            slot++;
        }
    }
    
    /**
     * add a suffix option to the menu
     */
    private void addSuffixOption(int slot, @NotNull StyleOption suffix) {
        // create item
        ItemStack item = new ItemStack(suffix.getMaterial());
        ItemMeta meta = item.getItemMeta();
        
        // set display name - show the actual suffix for symbols
        String displayValue = stripColours(suffix.getValue());
        Component displayName;
        
        if (displayValue.length() <= 2 && !displayValue.matches("[a-zA-Z]+")) {
            // for symbols, show the symbol as the name
            displayName = mm.deserialize(suffix.getValue() + " <white>" + suffix.getDisplayName());
        } else {
            // for text, show the display name
            displayName = mm.deserialize("<white>" + suffix.getDisplayName());
        }
        
        meta.displayName(displayName.decoration(TextDecoration.ITALIC, false));
        
        // set lore
        List<Component> lore = new ArrayList<>();
        
        // add preview
        String preview = player.getName() + " " + suffix.getValue();
        lore.add(mm.deserialize("<gray>preview: <white>" + preview)
            .decoration(TextDecoration.ITALIC, false));
        
        // add type indicator
        if (displayValue.length() <= 2 && !displayValue.matches("[a-zA-Z]+")) {
            lore.add(mm.deserialize("<dark_gray>symbol suffix")
                .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(mm.deserialize("<dark_gray>text suffix")
                .decoration(TextDecoration.ITALIC, false));
        }
        
        lore.add(Component.empty());
        lore.add(mm.deserialize("<yellow>click to apply!")
            .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(lore);
        
        // add glow if needed
        if (suffix.hasGlow()) {
            meta.setEnchantmentGlintOverride(true);
        }
        
        item.setItemMeta(meta);
        
        // create menu item with click handler
        setItem(slot, new MenuItem(item, event -> {
            event.setCancelled(true);
            Player p = (Player) event.getWhoClicked();
            selectSuffix(p, suffix);
        }));
    }
    
    /**
     * add section header
     */
    private void addSectionHeader(int slot, @NotNull String title, @NotNull Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(mm.deserialize("<gold><bold>" + title)
            .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(List.of(
            mm.deserialize("<gray>choose from the options below")
                .decoration(TextDecoration.ITALIC, false)
        ));
        
        item.setItemMeta(meta);
        
        setItem(slot, new MenuItem(item));
    }
    
    /**
     * add reset button
     */
    private void addResetButton(int slot) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(mm.deserialize("<red><bold>remove suffix")
            .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(List.of(
            mm.deserialize("<gray>remove your current suffix")
                .decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            mm.deserialize("<red>click to remove!")
                .decoration(TextDecoration.ITALIC, false)
        ));
        
        item.setItemMeta(meta);
        
        setItem(slot, new MenuItem(item, event -> {
            event.setCancelled(true);
            Player p = (Player) event.getWhoClicked();
            resetSuffix(p);
        }));
    }
    
    /**
     * fill empty slots with glass
     */
    private void fillEmpty() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.displayName(Component.empty());
        filler.setItemMeta(meta);
        
        for (int i = 0; i < getSize(); i++) {
            if (getItem(i) == null) {
                setItem(i, new MenuItem(filler));
            }
        }
    }
    
    /**
     * handle suffix selection
     */
    private void selectSuffix(@NotNull Player player, @NotNull StyleOption suffix) {
        // apply the suffix
        if (plugin.getAPI().setSuffix(player, suffix.getValue())) {
            // success
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            
            String displayValue = stripColours(suffix.getValue());
            String message = plugin.getConfigManager().getMessagesConfig()
                .getMessage("suffix.changed", "{suffix}", displayValue);
            player.sendMessage(mm.deserialize(message));
            
            player.closeInventory();
        } else {
            // failed
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
    
    /**
     * handle suffix reset
     */
    private void resetSuffix(@NotNull Player player) {
        if (plugin.getAPI().setSuffix(player, null)) {
            // success
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            
            Component message = mm.deserialize(
                plugin.getConfigManager().getMessagesConfig()
                    .getMessage("suffix.removed")
            );
            player.sendMessage(message);
            
            player.closeInventory();
        } else {
            // failed
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
    
    /**
     * strip colour codes from a string
     */
    private String stripColours(@NotNull String input) {
        // strip minimessage tags
        String stripped = input.replaceAll("<[^>]+>", "");
        // strip legacy codes
        stripped = stripped.replaceAll("&[0-9a-fk-or]", "");
        stripped = stripped.replaceAll("§[0-9a-fk-or]", "");
        return stripped.trim();
    }
}