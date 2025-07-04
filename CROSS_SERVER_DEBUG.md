# Cross-Server Chat Debug Guide

## The Issue
- Data (colors/prefixes) is syncing to MySQL ✅
- Chat formatting is NOT appearing on other servers ❌

## Debugging Steps

### 1. Check VentureChat Configuration
VentureChat needs to be configured to use our PlaceholderAPI placeholders.

**In VentureChat's config, look for chat format and ensure it uses:**
```
format: "%customization_prefix% %customization_name%: %message%"
```

**Available placeholders:**
- `%customization_prefix%` - The formatted prefix (bold, colored)
- `%customization_name%` - The formatted player name (colored, not bold)
- `%customization_full%` - Full name with prefix
- `%customization_nick%` - Nickname if set

### 2. Check Plugin Loading Order
1. LuckPerms loads first
2. PlaceholderAPI loads
3. VentureChat loads
4. Our plugin loads last and registers placeholders

### 3. Test Placeholders Manually
Use `/papi parse <player> %customization_prefix%` to test if placeholders work.

### 4. Check Server Logs
Look for these log messages:
- "Player X joined - refreshing data for cross-server sync"
- "PlaceholderAPI prefix for X: prefix=..., color=..."
- "Loaded player data for X - Color: ..., Prefix: ..."

### 5. Common Issues
- **PlaceholderAPI not installed** on all servers
- **VentureChat config** not using our placeholders
- **Plugin load order** - our plugin needs to load after PlaceholderAPI
- **Data timing** - PlaceholderAPI called before data loads

## Quick Fix Test
If VentureChat isn't working, try our fallback chat listener by adding to config:
```yaml
fallback_chat:
  enabled: true
```