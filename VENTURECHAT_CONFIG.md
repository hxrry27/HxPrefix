# VentureChat Configuration Fix

## The Issue
VentureChat is showing literal `%customization_full%: [message]` instead of processing the placeholder.

## Root Cause
VentureChat config is using our placeholder but PlaceholderAPI isn't processing it properly.

## Solution 1: Check VentureChat Config

### In VentureChat's `config.yml`, find the chat format section:

```yaml
# Look for something like this:
default:
  format: "%customization_full%: %message%"
  
# Or this:
chat:
  format: "%customization_full%: %message%"
```

### Replace with proper format:
```yaml
default:
  format: "%customization_prefix% %customization_name%: %message%"
  
# Or if you want the full format:
default:
  format: "%customization_full%: %message%"
```

## Solution 2: Verify PlaceholderAPI

### Test placeholders manually:
```
/papi parse YourName %customization_prefix%
/papi parse YourName %customization_name%
/papi parse YourName %customization_full%
```

### If placeholders show as literal text:
1. Check PlaceholderAPI is installed
2. Restart server after installing our plugin
3. Check our plugin registered with PAPI: `/papi list` should show "customization"

## Solution 3: Alternative VentureChat Format

### Try this format instead:
```yaml
# In VentureChat config.yml
default:
  format: "{prefix}{nickname}: {message}"
  prefix: "%customization_prefix% "
  nickname: "%customization_name%"
```

## Solution 4: Disable VentureChat Temporarily

### To test if our fallback chat works:
1. Temporarily disable VentureChat
2. Restart server
3. Test chat - should show formatted colors/prefixes
4. If it works, the issue is VentureChat config

## Expected Result
Chat should show:
- `[SUPPORTER] PlayerName: message` 
- `[PATRON] PlayerName: message`
- With prefix bold and name colored

## Quick Debug Steps
1. Run `/papi parse YourName %customization_full%` - does it show formatted text?
2. Check VentureChat config format line
3. Restart server after any config changes
4. Test with a simple message

## Fallback Option
Our plugin has a built-in chat formatter that works without VentureChat. If VentureChat continues to cause issues, we can disable it and rely on our fallback system.