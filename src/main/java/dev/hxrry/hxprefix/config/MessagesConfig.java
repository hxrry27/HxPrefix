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
 * Handles all message configuration and formatting
 */
public class MessagesConfig {
    @SuppressWarnings("unused")
    private final HxPrefix plugin;
    private final File file;
    private FileConfiguration config;
    private final MiniMessage mm = MiniMessage.miniMessage();
    
    // Cache processed messages
    private final Map<String, String> messageCache = new HashMap<>();
    private String pluginPrefix;
    
    public MessagesConfig(@NotNull HxPrefix plugin, @NotNull File file) {
        this.plugin = plugin;
        this.file = file;
    }
    
    /**
     * Load messages from file
     */
    public void load() {
        config = YamlConfiguration.loadConfiguration(file);
        messageCache.clear();
        
        // Load plugin prefix
        pluginPrefix = config.getString("plugin_prefix", "<gray>[<gradient:#00ff00:#00ffff>HxPrefix</gradient>]</gray> ");
        
        // Validate messages exist
        validateMessages();
        
        Log.debug("Loaded " + countMessages() + " messages");
    }
    
    /**
     * Get a message by key
     */
    @NotNull
    public String getMessage(@NotNull String key) {
        // Check cache first
        if (messageCache.containsKey(key)) {
            return messageCache.get(key);
        }
        
        // Load from config
        String message = config.getString(key);
        if (message == null) {
            Log.warning("Missing message: " + key);
            message = "<red>Missing message: " + key;
        }
        
        // Process message (add plugin prefix if needed)
        if (shouldHavePrefix(key)) {
            message = pluginPrefix + message;
        }
        
        // Cache it
        messageCache.put(key, message);
        return message;
    }
    
    /**
     * Get a message with replacements
     */
    @NotNull
    public String getMessage(@NotNull String key, @NotNull String... replacements) {
        String message = getMessage(key);
        
        // Apply replacements (format: key, value, key, value...)
        for (int i = 0; i < replacements.length - 1; i += 2) {
            String placeholder = replacements[i];
            String value = replacements[i + 1];
            message = message.replace(placeholder, value);
        }
        
        return message;
    }
    
    /**
     * Get a message as a component
     */
    @NotNull
    public Component getComponent(@NotNull String key) {
        return mm.deserialize(getMessage(key));
    }
    
    /**
     * Get a message as a component with replacements
     */
    @NotNull
    public Component getComponent(@NotNull String key, @NotNull TagResolver... resolvers) {
        return mm.deserialize(getMessage(key), resolvers);
    }
    
    /**
     * Get a message with placeholder replacements
     */
    @NotNull
    public Component getComponent(@NotNull String key, @NotNull String... replacements) {
        // Convert string replacements to tag resolvers
        TagResolver[] resolvers = new TagResolver[replacements.length / 2];
        for (int i = 0, j = 0; i < replacements.length - 1; i += 2, j++) {
            String placeholder = replacements[i].replace("{", "").replace("}", "");
            String value = replacements[i + 1];
            resolvers[j] = Placeholder.unparsed(placeholder, value);
        }
        
        return mm.deserialize(getMessage(key), resolvers);
    }
    
    /**
     * Get the plugin prefix
     */
    @NotNull
    public String getPrefix() {
        return pluginPrefix;
    }
    
    /**
     * Check if a message key should have the plugin prefix added
     */
    private boolean shouldHavePrefix(@NotNull String key) {
        // Don't add prefix to certain message categories
        if (key.equals("prefix")) {
            // "prefix" is the plugin prefix itself, not a message
            return false;
        }
        
        if (key.startsWith("gui.") || 
            key.endsWith(".current") ||
            key.endsWith(".not-set") ||
            key.startsWith("usage.")) {
            return false;
        }
        
        // All other messages get the prefix
        return true;
    }
    
    /**
     * Validate that required messages exist
     */
    private void validateMessages() {
        // Check for required message keys
        String[] required = {
            "permissions.no-permission",
            "permissions.no-permission-rank",
            
            "color.changed",
            "color.reset",
            
            "prefix.changed",
            "prefix.removed",
            
            "suffix.changed",
            "suffix.removed",
            
            "nickname.changed",
            "nickname.removed",
            "nickname.current",
            "nickname.invalid",
            "nickname.blocked"
        };
        
        int missing = 0;
        for (String key : required) {
            if (!config.contains(key)) {
                Log.warning("Missing required message: " + key);
                missing++;
                
                // Set a default
                config.set(key, getDefaultMessage(key));
            }
        }
        
        if (missing > 0) {
            Log.warning("Added " + missing + " missing messages with defaults");
            saveConfig();
        }
    }
    
    /**
     * Get a default message for a key
     */
    @NotNull
    private String getDefaultMessage(@NotNull String key) {
        // Provide sensible defaults
        return switch (key) {
            case "permissions.no-permission" -> "<red>You don't have permission to use this command!";
            case "permissions.no-permission-rank" -> "<red>Your rank ({rank}) doesn't have access to this feature!";
            
            case "color.changed" -> "<green>Your name color has been updated!";
            case "color.reset" -> "<yellow>Your name color has been reset!";
            
            case "prefix.changed" -> "<green>Your prefix has been set to: {value}";
            case "prefix.removed" -> "<yellow>Your prefix has been removed!";
            
            case "suffix.changed" -> "<green>Your suffix has been set to: {value}";
            case "suffix.removed" -> "<yellow>Your suffix has been removed!";
            
            case "nickname.changed" -> "<green>Your nickname has been set to: {value}";
            case "nickname.removed" -> "<yellow>Your nickname has been removed!";
            case "nickname.current" -> "<gray>Your current nickname is: <white>{value}";
            case "nickname.invalid" -> "<red>Nickname must be alphanumeric and {min}-{max} characters!";
            case "nickname.blocked" -> "<red>That nickname is not allowed!";
            
            default -> "<red>Missing message: " + key;
        };
    }
    
    /**
     * Save the config file
     */
    private void saveConfig() {
        try {
            config.save(file);
        } catch (Exception e) {
            Log.error("Failed to save messages.yml", e);
        }
    }
    
    /**
     * Count total messages
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
     * Reload messages
     */
    public void reload() {
        load();
    }
}