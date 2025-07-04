# PlayerCustomisation Setup Instructions

## Prerequisites
1. **DeluxeMenus** plugin installed on your server
2. **PlaceholderAPI** plugin installed on your server
3. **LuckPerms** plugin installed on your server
4. **MySQL database** (configured in docker-compose)

## Installation Steps

### 1. Install the Plugin
1. Compile the plugin: `mvn clean package`
2. Place `PlayerCustomisation-1.0.0.jar` in your server's `plugins/` folder
3. Restart the server to generate configuration files

### 2. Setup DeluxeMenus
1. Copy all `.yml` files from the `deluxemenus/` folder to your server's `plugins/DeluxeMenus/menus/` folder:
   - `color_supporter.yml`
   - `color_patron.yml` 
   - `prefix_supporter.yml`
   - `prefix_patron.yml`
   - `prefix_devoted.yml`

2. Restart your server or reload DeluxeMenus: `/dm reload`

### 3. Configure LuckPerms Permissions
Set up permissions for each rank:

```bash
# Supporter rank permissions
/lp group supporter permission set playercustomisation.color true
/lp group supporter permission set playercustomisation.prefix true
/lp group supporter permission set playercustomisation.nick true

# Patron rank permissions (inherits supporter)
/lp group patron parent add supporter
/lp group patron permission set playercustomisation.color true
/lp group patron permission set playercustomisation.prefix true
/lp group patron permission set playercustomisation.nick true

# Devoted rank permissions (inherits patron)
/lp group devoted parent add patron
/lp group devoted permission set playercustomisation.customtag true

# Admin permissions
/lp group admin permission set playercustomisation.admin.tags true
/lp group admin permission set playercustomisation.admin.reload true
```

### 4. Database Setup
The plugin will automatically:
- Connect to your MySQL database using the configured credentials
- Create the required tables (`player_data` and `custom_tag_requests`)
- Set up connection pooling with HikariCP

### 5. PlaceholderAPI Integration
The plugin automatically registers these placeholders:
- `%playercustomisation_prefix%` - Player's formatted prefix
- `%playercustomisation_nickname%` - Player's nickname or username
- `%playercustomisation_namecolor%` - Player's name color code
- `%playercustomisation_formatted_name%` - Fully formatted colored name

### 6. VentureChat Integration
Update your VentureChat configuration to use the placeholders:

```yaml
# In VentureChat's config.yml
default:
  format: "%playercustomisation_prefix% %playercustomisation_formatted_name%: %message%"
```

## Commands Available
- `/color` - Opens color selection GUI (supporter+)
- `/prefix` - Opens prefix selection GUI (supporter+)
- `/nick <name|off>` - Set or reset nickname (supporter+)
- `/requesttag <tag>` - Request custom tag (devoted only)
- `/managetags` - Review tag requests (admin only)
- `/pcreload` - Reload configuration (admin only)

## Troubleshooting

### GUI not opening?
1. Make sure DeluxeMenus is installed and running
2. Check that the menu files are in the correct directory
3. Reload DeluxeMenus: `/dm reload`

### Database connection issues?
1. Verify MySQL container is running
2. Check the connection details in `config.yml`
3. Look at server console for error messages

### Placeholders not working?
1. Ensure PlaceholderAPI is installed
2. Check that the expansion is registered: `/papi list`
3. Test placeholders: `/papi parse <player> %playercustomisation_prefix%`

### Permissions not working?
1. Verify LuckPerms groups are set up correctly
2. Check player's primary group: `/lp user <player> info`
3. Ensure permissions are set on the correct groups

## Cross-Server Setup
For Velocity networks:
1. Install the plugin on ALL backend servers
2. Use the same MySQL database for all servers
3. Configure identical DeluxeMenus files on all servers
4. Data will sync automatically across servers with 30-second cache TTL