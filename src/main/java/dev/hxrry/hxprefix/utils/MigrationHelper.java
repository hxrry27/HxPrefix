package dev.hxrry.hxprefix.utils;

import dev.hxrry.hxcore.utils.Log;

import dev.hxrry.hxprefix.HxPrefix;
import dev.hxrry.hxprefix.api.models.PlayerCustomization;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility to migrate from old PlayerCustomisation plugin to HxPrefix
 */
public class MigrationHelper {
    private final HxPrefix plugin;
    
    public MigrationHelper(@NotNull HxPrefix plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Migrate data from old plugin
     * @return number of players migrated
     */
    public int migrate() {
        Log.info("Starting migration from PlayerCustomisation...");
        
        // Check for old plugin folder
        File oldPluginFolder = new File(plugin.getDataFolder().getParentFile(), "PlayerCustomisation");
        if (!oldPluginFolder.exists()) {
            Log.error("PlayerCustomisation folder not found, trying ValePrefix...");
            oldPluginFolder = new File(plugin.getDataFolder().getParentFile(), "ValePrefix");
            
            if (!oldPluginFolder.exists()) {
                Log.error("No old plugin folder found to migrate from");
                return 0;
            }
        }
        
        AtomicInteger migrated = new AtomicInteger(0);
        
        // Try database migration first
        int dbMigrated = migrateDatabase(oldPluginFolder);
        migrated.addAndGet(dbMigrated);
        
        // If no database, try config files
        if (dbMigrated == 0) {
            int fileMigrated = migrateFiles(oldPluginFolder);
            migrated.addAndGet(fileMigrated);
        }
        
        // Migrate configuration
        migrateConfig(oldPluginFolder);
        
        Log.info("Migration complete! Migrated " + migrated.get() + " players");
        return migrated.get();
    }
    
    /**
     * Migrate from old database
     */
    private int migrateDatabase(@NotNull File oldFolder) {
        File dbFile = new File(oldFolder, "database.db");
        if (!dbFile.exists()) {
            // Try MySQL config
            return migrateMySQLDatabase(oldFolder);
        }
        
        // SQLite migration
        int count = 0;
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            
            // Check if old table exists
            ResultSet rs = stmt.executeQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='player_customisation'"
            );
            
            if (!rs.next()) {
                Log.error("No player_customisation table found in old database");
                return 0;
            }
            
            // Query old data
            String query = "SELECT * FROM player_customisation";
            rs = stmt.executeQuery(query);
            
            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    String username = rs.getString("username");
                    
                    PlayerCustomization data = new PlayerCustomization(uuid, username);
                    
                    // Map old fields to new ones
                    String nickname = rs.getString("nickname");
                    if (nickname != null && !nickname.isEmpty()) {
                        data.setNickname(nickname);
                    }
                    
                    String color = rs.getString("color");
                    if (color != null && !color.isEmpty()) {
                        // Convert old color format to new
                        data.setNameColour(convertOldColor(color));
                    }
                    
                    String prefix = rs.getString("prefix");
                    if (prefix != null && !prefix.isEmpty()) {
                        data.setPrefix(prefix);
                    }
                    
                    String suffix = rs.getString("suffix");
                    if (suffix != null && !suffix.isEmpty()) {
                        data.setSuffix(suffix);
                    }
                    
                    // Save to new database
                    plugin.getDatabaseManager().savePlayerData(data);
                    count++;
                    
                } catch (Exception e) {
                    Log.error("Failed to migrate player: " + e.getMessage());
                }
            }
            
            Log.info("Migrated " + count + " players from SQLite database");
            
        } catch (SQLException e) {
            Log.error("Failed to migrate SQLite database", e);
        }
        
        return count;
    }
    
    /**
     * Migrate from MySQL database
     */
    private int migrateMySQLDatabase(@NotNull File oldFolder) {
        File configFile = new File(oldFolder, "config.yml");
        if (!configFile.exists()) {
            return 0;
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        ConfigurationSection dbSection = config.getConfigurationSection("database");
        
        if (dbSection == null || !dbSection.getString("type", "").equalsIgnoreCase("mysql")) {
            return 0;
        }
        
        String host = dbSection.getString("host", "localhost");
        int port = dbSection.getInt("port", 3306);
        String database = dbSection.getString("database", "playercustomisation");
        String username = dbSection.getString("username", "root");
        String password = dbSection.getString("password", "");
        
        int count = 0;
        String url = String.format("jdbc:mysql://%s:%d/%s", host, port, database);
        
        try (Connection conn = DriverManager.getConnection(url, username, password);
             Statement stmt = conn.createStatement()) {
            
            // Query old data
            String query = "SELECT * FROM player_customisation";
            ResultSet rs = stmt.executeQuery(query);
            
            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    String playerName = rs.getString("username");
                    
                    PlayerCustomization data = new PlayerCustomization(uuid, playerName);
                    
                    // Map fields
                    String nickname = rs.getString("nickname");
                    if (nickname != null && !nickname.isEmpty()) {
                        data.setNickname(nickname);
                    }
                    
                    String color = rs.getString("color");
                    if (color != null && !color.isEmpty()) {
                        data.setNameColour(convertOldColor(color));
                    }
                    
                    String prefix = rs.getString("prefix");
                    if (prefix != null && !prefix.isEmpty()) {
                        data.setPrefix(prefix);
                    }
                    
                    String suffix = rs.getString("suffix");
                    if (suffix != null && !suffix.isEmpty()) {
                        data.setSuffix(suffix);
                    }
                    
                    // Save to new database
                    plugin.getDatabaseManager().savePlayerData(data);
                    count++;
                    
                } catch (Exception e) {
                    Log.error("Failed to migrate MySQL player: " + e.getMessage());
                }
            }
            
            Log.info("Migrated " + count + " players from MySQL database");
            
        } catch (SQLException e) {
            Log.error("Failed to migrate MySQL database", e);
        }
        
        return count;
    }
    
    /**
     * Migrate from player data files
     */
    private int migrateFiles(@NotNull File oldFolder) {
        File dataFolder = new File(oldFolder, "playerdata");
        if (!dataFolder.exists()) {
            dataFolder = new File(oldFolder, "data");
            if (!dataFolder.exists()) {
                return 0;
            }
        }
        
        int count = 0;
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        
        if (files == null) {
            return 0;
        }
        
        for (File file : files) {
            try {
                String fileName = file.getName();
                String uuidStr = fileName.replace(".yml", "");
                UUID uuid = UUID.fromString(uuidStr);
                
                FileConfiguration data = YamlConfiguration.loadConfiguration(file);
                String username = data.getString("username", "Unknown");
                
                PlayerCustomization playerData = new PlayerCustomization(uuid, username);
                
                // Map old fields
                String nickname = data.getString("nickname");
                if (nickname != null && !nickname.isEmpty()) {
                    playerData.setNickname(nickname);
                }
                
                String color = data.getString("color");
                if (color != null && !color.isEmpty()) {
                    playerData.setNameColour(convertOldColor(color));
                }
                
                String prefix = data.getString("prefix");
                if (prefix != null && !prefix.isEmpty()) {
                    playerData.setPrefix(prefix);
                }
                
                String suffix = data.getString("suffix");
                if (suffix != null && !suffix.isEmpty()) {
                    playerData.setSuffix(suffix);
                }
                
                // Save to new database
                plugin.getDatabaseManager().savePlayerData(playerData);
                count++;
                
            } catch (Exception e) {
                Log.error("Failed to migrate file: " + file.getName() + " - " + e.getMessage());
            }
        }
        
        Log.info("Migrated " + count + " players from data files");
        return count;
    }
    
    /**
     * Migrate configuration settings
     */
    private void migrateConfig(@NotNull File oldFolder) {
        File oldConfig = new File(oldFolder, "config.yml");
        if (!oldConfig.exists()) {
            return;
        }
        
        Log.info("Migrating configuration...");
        FileConfiguration old = YamlConfiguration.loadConfiguration(oldConfig);
        
        // Create migration report
        File reportFile = new File(plugin.getDataFolder(), "migration-report.txt");
        StringBuilder report = new StringBuilder();
        report.append("=== HxPrefix Migration Report ===\n\n");
        report.append("Migrated from: ").append(oldFolder.getName()).append("\n");
        report.append("Migration date: ").append(new java.util.Date()).append("\n\n");
        
        // Note important settings that need manual review
        report.append("Settings requiring manual review:\n");
        
        if (old.contains("colors")) {
            report.append("- Colors: Please review colors.yml for rank permissions\n");
        }
        
        if (old.contains("prefixes")) {
            report.append("- Prefixes: Please review prefixes.yml for rank permissions\n");
        }
        
        if (old.contains("suffixes")) {
            report.append("- Suffixes: Please review suffixes.yml for rank permissions\n");
        }
        
        if (old.contains("custom-tags")) {
            report.append("- Custom tags: Settings migrated, review tags.yml\n");
        }
        
        // Save report
        try {
            java.nio.file.Files.write(reportFile.toPath(), report.toString().getBytes());
            Log.info("Migration report saved to migration-report.txt");
        } catch (Exception e) {
            Log.error("Failed to save migration report: " + e.getMessage());
        }
    }
    
    /**
     * Convert old color format to new format
     */
    private String convertOldColor(@NotNull String oldColor) {
        // Remove any & or § symbols
        oldColor = oldColor.replace("&", "").replace("§", "");
        
        // Map old color codes to new format
        switch (oldColor.toLowerCase()) {
            case "a" -> { return "<green>"; }
            case "b" -> { return "<aqua>"; }
            case "c" -> { return "<red>"; }
            case "d" -> { return "<light_purple>"; }
            case "e" -> { return "<yellow>"; }
            case "f" -> { return "<white>"; }
            case "0" -> { return "<black>"; }
            case "1" -> { return "<dark_blue>"; }
            case "2" -> { return "<dark_green>"; }
            case "3" -> { return "<dark_aqua>"; }
            case "4" -> { return "<dark_red>"; }
            case "5" -> { return "<dark_purple>"; }
            case "6" -> { return "<gold>"; }
            case "7" -> { return "<gray>"; }
            case "8" -> { return "<dark_gray>"; }
            case "9" -> { return "<blue>"; }
            default -> { return oldColor; } // Keep as-is if not recognized
        }
    }
}