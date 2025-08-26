package dev.hxrry.hxprefix.config;

import dev.hxrry.hxcore.utils.Log;

import dev.hxrry.hxprefix.HxPrefix;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * handles all message configuration and formatting
 */
public class MessagesConfig {
    @SuppressWarnings("unused")
    private final HxPrefix plugin;
    private final File file;
    private FileConfiguration config;
    private final MiniMessage mm = MiniMessage.miniMessage();
    
    // cache processed messages
    private final Map<String, String> messageCache = new HashMap<>();
    private String prefix;
    
    public MessagesConfig(@NotNull HxPrefix plugin, @NotNull File file) {
        this.plugin = plugin;
        this.file = file;
    }
    
    /**
     * load messages from file
     */
    public void load() {
        config = YamlConfiguration.loadConfiguration(file);
        messageCache.clear();
        
        // load prefix
        prefix = config.getString("prefix", "<gray>[<gradient:#00ff00:#00ffff>HxPrefix</gradient>]</gray> ");
        
        // validate messages exist
        validateMessages();
        
        Log.debug("loaded " + countMessages() + " messages");
    }
    
    /**
     * get a message by key
     */
    @NotNull
    public String getMessage(@NotNull String key) {
        // check cache first
        if (messageCache.containsKey(key)) {
            return messageCache.get(key);
        }
        
        // load from config
        String message = config.getString(key);
        if (message == null) {
            Log.warning("missing message: " + key);
            message = "<red>missing message: " + key;
        }
        
        // process message (add prefix if needed)
        if (!key.startsWith("prefix") && shouldHavePrefix(key)) {
            message = prefix + message;
        }
        
        // cache it
        messageCache.put(key, message);
        return message;
    }
    
    /**
     * get a message with replacements
     */
    @NotNull
    public String getMessage(@NotNull String key, @NotNull String... replacements) {
        String message = getMessage(key);
        
        // apply replacements (format: key, value, key, value...)
        for (int i = 0; i < replacements.length - 1; i += 2) {
            String placeholder = replacements[i];
            String value = replacements[i + 1];
            message = message.replace(placeholder, value);
        }
        
        return message;
    }
    
    /**
     * get a message as a component
     */
    @NotNull
    public Component getComponent(@NotNull String key) {
        return mm.deserialize(getMessage(key));
    }
    
    /**
     * get a message as a component with replacements
     */
    @NotNull
    public Component getComponent(@NotNull String key, @NotNull TagResolver... resolvers) {
        return mm.deserialize(getMessage(key), resolvers);
    }
    
    /**
     * get a message with placeholder replacements
     */
    @NotNull
    public Component getComponent(@NotNull String key, @NotNull String... replacements) {
        // convert string replacements to tag resolvers
        TagResolver[] resolvers = new TagResolver[replacements.length / 2];
        for (int i = 0, j = 0; i < replacements.length - 1; i += 2, j++) {
            String placeholder = replacements[i].replace("{", "").replace("}", "");
            String value = replacements[i + 1];
            resolvers[j] = Placeholder.unparsed(placeholder, value);
        }
        
        return mm.deserialize(getMessage(key), resolvers);
    }
    
    /**
     * get the prefix
     */
    @NotNull
    public String getPrefix() {
        return prefix;
    }
    
    /**
     * check if a message key should have the prefix
     */
    private boolean shouldHavePrefix(@NotNull String key) {
        // don't add prefix to certain message types
        if (key.startsWith("error.") || 
            key.startsWith("usage.") ||
            key.contains(".current") ||
            key.contains(".status")) {
            return false;
        }
        
        // check config for prefix setting
        return config.getBoolean("use-prefix", true);
    }
    
    /**
     * validate that required messages exist
     */
    private void validateMessages() {
        // check for required message keys
        String[] required = {
            "error.no-permission",
            "error.player-only",
            "error.no-colour-permission",
            "error.no-prefix-permission",
            "error.no-suffix-permission",
            "error.no-nickname-permission",
            "error.no-tag-permission",
            
            "colour.changed",
            "colour.removed",
            
            "prefix.changed",
            "prefix.removed",
            
            "suffix.changed",
            "suffix.removed",
            
            "nickname.changed",
            "nickname.removed",
            "nickname.current",
            "nickname.not-set",
            
            "tag.submitted",
            "tag.cancelled",
            "tag.status.pending",
            "tag.status.active",
            "tag.status.none"
        };
        
        int missing = 0;
        for (String key : required) {
            if (!config.contains(key)) {
                Log.warning("missing required message: " + key);
                missing++;
                
                // set a default
                config.set(key, getDefaultMessage(key));
            }
        }
        
        if (missing > 0) {
            Log.warning("added " + missing + " missing messages with defaults");
            saveConfig();
        }
    }
    
    /**
     * get a default message for a key
     */
    @NotNull
    private String getDefaultMessage(@NotNull String key) {
        // provide sensible defaults
        return switch (key) {
            case "error.no-permission" -> "<red>you don't have permission to do that!";
            case "error.player-only" -> "<red>this command can only be used by players!";
            case "error.no-colour-permission" -> "<red>your rank ({rank}) doesn't have access to colours!";
            case "error.no-prefix-permission" -> "<red>your rank ({rank}) doesn't have access to prefixes!";
            case "error.no-suffix-permission" -> "<red>your rank ({rank}) doesn't have access to suffixes!";
            case "error.no-nickname-permission" -> "<red>your rank ({rank}) doesn't have access to nicknames!";
            case "error.no-tag-permission" -> "<red>custom tags are only available for devoted rank and above!";
            
            case "colour.changed" -> "<green>your colour has been updated!";
            case "colour.removed" -> "<yellow>your colour has been removed!";
            
            case "prefix.changed" -> "<green>your prefix has been set to {prefix}!";
            case "prefix.removed" -> "<yellow>your prefix has been removed!";
            
            case "suffix.changed" -> "<green>your suffix has been set to {suffix}!";
            case "suffix.removed" -> "<yellow>your suffix has been removed!";
            
            case "nickname.changed" -> "<green>your nickname has been set to {nickname}!";
            case "nickname.removed" -> "<yellow>your nickname has been removed!";
            case "nickname.current" -> "<gray>your current nickname is: <white>{nickname}";
            case "nickname.not-set" -> "<gray>you don't have a nickname set";
            
            case "tag.submitted" -> "<green>your tag request for '{tag}' has been submitted!";
            case "tag.cancelled" -> "<yellow>your tag request for '{tag}' has been cancelled!";
            case "tag.status.pending" -> "<yellow>you have a pending request for '{tag}' ({days} days old)";
            case "tag.status.active" -> "<green>your current custom tag is: {tag}";
            case "tag.status.none" -> "<gray>you don't have any tag requests";
            
            default -> "<red>missing message: " + key;
        };
    }
    
    /**
     * save the config file
     */
    private void saveConfig() {
        try {
            config.save(file);
        } catch (Exception e) {
            Log.error("failed to save messages.yml", e);
        }
    }
    
    /**
     * count total messages
     */
    private int countMessages() {
        int count = 0;
        for (String key : config.getKeys(true)) {
            if (!config.isConfigurationSection(key)) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * reload messages
     */
    public void reload() {
        load();
    }
}