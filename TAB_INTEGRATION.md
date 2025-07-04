# TAB Plugin Integration Guide

## Overview
TAB plugin can display our custom colors and prefixes in the tab list using PlaceholderAPI placeholders.

## Available Placeholders
Our plugin provides these placeholders for TAB:

### Main Placeholders:
- `%customization_prefix%` - The formatted prefix (bold, colored) 
- `%customization_name%` - The formatted player name (colored, not bold)
- `%customization_full%` - Full name with prefix: "[PREFIX] PlayerName"
- `%customization_nick%` - Nickname if set, otherwise regular name

### Info Placeholders:
- `%customization_rank%` - Player's rank (supporter, patron, devoted)
- `%customization_color%` - Current name color
- `%customization_has_prefix%` - true/false if player has prefix

## TAB Configuration

### Basic Setup
In TAB's `config.yml`, configure the tablist format:

```yaml
tablist-name-formatting:
  enabled: true
  # Format: prefix + colored name
  format: "%customization_prefix%%customization_name%"
```

### Advanced Setup
For more control, you can use conditional formatting:

```yaml
tablist-name-formatting:
  enabled: true
  format: "%customization_full%"

# Or with rank-based formatting:
groups:
  supporter:
    tabformat: "%customization_prefix% %customization_name%"
  patron:
    tabformat: "%customization_prefix% %customization_name%"
  devoted:
    tabformat: "%customization_prefix% %customization_name%"
  default:
    tabformat: "%customization_name%"
```

### With Sorting
TAB can sort players by rank using LuckPerms:

```yaml
tablist-objective:
  enabled: true
  value: "%luckperms_primary_group_weight%"
```

## Example Results
- **Supporter**: `[SUPPORTER] PlayerName` (prefix bold, name colored)
- **Patron**: `[PATRON] PlayerName` (prefix bold, name colored)  
- **Devoted**: `[DEVOTED] PlayerName` (prefix bold, name colored)
- **Event**: `[PRIDE] PlayerName` (gradient prefix, colored name)

## Testing
1. Install TAB plugin
2. Add our placeholders to TAB config
3. Restart server
4. Check tab list shows formatted names
5. Change colors/prefixes and verify updates

## Common Issues
- **PlaceholderAPI not installed** - TAB needs PAPI to read our placeholders
- **Wrong placeholder format** - Must use exact names like `%customization_prefix%`
- **No formatting** - Check TAB config syntax and restart server
- **Cross-server** - TAB config must be identical on all servers

## Alternative: Built-in Integration
If TAB doesn't work properly, we could create a direct TAB integration that doesn't rely on PlaceholderAPI.