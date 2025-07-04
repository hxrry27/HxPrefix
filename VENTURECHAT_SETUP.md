# VentureChat Integration Setup

## Overview
This plugin integrates with VentureChat through PlaceholderAPI to provide custom name colors, prefixes, and gradients in chat.

## Hybrid Prefix System
The plugin automatically assigns rank-based prefixes when players get promoted in LuckPerms:
- **Player rank**: No prefix (blank)
- **Supporter rank**: `[Supporter]` prefix in white
- **Patron rank**: `[Patron]` prefix in white  
- **Devoted rank**: `[Devoted]` prefix in white

Players can then use the GUI to customize their prefix color or choose alternative prefixes available to their rank.

## Available Placeholders

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%customization_name%` | Player's formatted name with colors/gradients | `§cPlayerName` |
| `%customization_prefix%` | Player's formatted prefix | `§6[Supporter]` |
| `%customization_full%` | Full formatted name with prefix | `§6[Supporter] §cPlayerName` |
| `%customization_nick%` | Player's nickname (if set) | `CoolNick` |
| `%customization_rank%` | Player's rank | `supporter` |
| `%customization_color%` | Current color name | `red` |
| `%customization_gradient%` | Current gradient | `red:blue` |

## VentureChat Configuration

### Method 1: Using the full formatted name
In your VentureChat `config.yml`, find the format section and use:

```yaml
chat-format: '%customization_full%: %message%'
```

### Method 2: Using separate prefix and name
```yaml
chat-format: '%customization_prefix% %customization_name%: %message%'
```

### Method 3: Integration with existing format
If you have existing rank prefixes, you can combine them:
```yaml
chat-format: '%vault_prefix%%customization_prefix% %customization_name%: %message%'
```

## Troubleshooting

1. **Colors not showing**: Make sure PlaceholderAPI is installed and our plugin is loaded
2. **Placeholders not working**: Restart the server after installing both plugins
3. **Permission issues**: Ensure players have the `customization.use` permission

## Testing

1. Install the plugin and reload/restart
2. Set a color with `/color`
3. Type in chat to see if colors appear
4. Use `/papi parse [player] %customization_name%` to test placeholders directly