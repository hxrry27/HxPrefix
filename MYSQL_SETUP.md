# MySQL Cross-Server Setup Guide

## The Issue
Each server needs to connect to the SAME MySQL database for cross-server sync to work.

## Current Setup
- Database: `customisation-mysql` container
- Database name: `playercustomisation`
- Username: `playercustomisation`
- Password: `minecraft_password`

## Configuration Required

### Each Server Needs This config.yml:
```yaml
database:
  type: "mysql"
  host: "customisation-mysql"  # Docker service name
  port: 3306
  database: "playercustomisation"
  username: "playercustomisation"
  password: "minecraft_password"
```

### File Locations:
- `/lobby-server/plugins/PlayerCustomization/config.yml`
- `/public-server/plugins/PlayerCustomization/config.yml`  
- `/creative-velocity/plugins/PlayerCustomization/config.yml`
- `/resource-server/plugins/PlayerCustomization/config.yml`

## Test Connection
Check logs for: "Connected to MySQL database: playercustomisation"

If you see SQLite messages, the config isn't being read properly.

## Common Issues
1. **Config file not copied** to all servers
2. **Wrong service name** - must be "customisation-mysql" 
3. **Network issues** - servers not on same Docker network
4. **Database not created** - container didn't initialize properly

## Quick Test
1. Set color on creative server
2. Check MySQL directly: `SELECT * FROM players WHERE username='YourName';`
3. Switch to SMP server
4. Check if color persists