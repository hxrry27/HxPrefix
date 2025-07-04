# Prxfix27 Player Customisation Plugin

A small paper/purpur plugin built for player name colors, chat prefixes, and nickname customization with rank-based permissions.

## Main Features

- **Rank-based color system** - Different features for 3 tiers of ranks [Supporter, Patron, and Devoted ranks]
- **Interactive GUIs** - Minecraft-centric and hopefully easy-to-use inventory interfaces for customization  
- **Gradient support** - Custom gradient colors for higher ranks
- **Custom prefixes** - Player-requested prefixes with admin approval system
- **Multi-server support** - Shared database across network servers [for velocity compatibility]
- **LuckPerms integration** - Deep permission system integration [luckperms my beloved]

## Commands

- `/customize` - Open the main customization hub
- `/color` - Direct access to color selection
- `/prefix` - Choose from available prefixes
- `/nick <name>` - Set your nickname
- `/gradient` - Create custom gradients (Patron+)

## Admin Commands

- `/customization admin` - Admin interface for managing requests
- `/customization reload` - Reload plugin configuration
- `/customization reset <player>` - Reset player's customizations

## Installation

1. Drop the JAR into your `plugins/` folder
2. Make sure you have LuckPerms installed
3. Restart your server
4. Configure ranks and permissions as needed

## Building

```bash
mvn clean package
```

Requires Java 21 and Maven.