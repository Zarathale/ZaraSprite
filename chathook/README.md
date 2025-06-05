# chathook (Paper 1.20+)

Intercepts private messages (DMs) to the in-game player `ZaraSprite`, forwards them as JSON to a configured HTTP endpoint, and logs each request to per-user files under `plugins/chathook/logs/`.

---

## Configuration

Edit `plugins/chathook/config.yml`:

```yaml
# URL to POST each private-message JSON payload
endpoint-url: "http://your-endpoint.example.com/receive"

# HTTP timeout in milliseconds
timeout-ms: 5000

# Retry count if POST fails
retry-limit: 3

# When true, exception stack traces go to error.log
debug: false
```

After saving changes, run:
```
/chathook reload
```
to apply them without restarting.

---

## Commands & Permissions

All subcommands live under the single root `/chathook`. Tab-completion suggests exactly:
```
reload
purger
purgeall
```

```
/chathook reload
/chathook purger <username>
/chathook purgeall
```

Permission nodes (in `plugin.yml`):

```yaml
permissions:
  chathook.admin:
    description: "Full access to chathook (assign mod, etc.)"
    default: op
    children:
      chathook.mod: true

  chathook.mod:
    description: "Can reload config and purge logs"
    default: false
```

- **`/chathook reload`**  
  - Requires: `chathook.mod` (or `op`)  
  - Action: Re-reads `config.yml` into memory.  
  - Feedback: “§aConfiguration reloaded.” or “§cYou don’t have permission to reload.”

- **`/chathook purger <username>`**  
  - Requires: `chathook.mod` or sender’s name equals `<username>`.  
  - Action: Deletes `plugins/chathook/logs/<username>.log`, if present.  
  - Feedback:  
    - “§aPurged logs for <username>.”  
    - “§eNo logs exist for user <username>.”  
    - “§cYou don’t have permission to purge logs for “<username>”.”

- **`/chathook purgeall`**  
  - Requires: `chathook.mod`.  
  - Action: Deletes every file under `plugins/chathook/logs/`.  
  - Feedback: “§aAll user logs purged.” or “§cYou don’t have permission to purge all logs.”

---

## JSON Payload Format

When a DM arrives for `ZaraSprite`, the plugin builds and POSTs this JSON:

```json
{
  "username": "<senderName>",
  "message": "<rawMessage>",
  "timestamp": "<ISO-8601 UTC timestamp>"
}
```

Example:

```json
{
  "username": "alice",
  "message": "Hey ZaraSprite!",
  "timestamp": "2025-06-05T02:14:33Z"
}
```

---

## Log Files

- **Per-user logs**:  
  `plugins/chathook/logs/<username>.log`  
  Each line appended:

  ```
  [2025-06-05T02:14:33Z] {"username":"alice","message":"Hey ZaraSprite!","timestamp":"2025-06-05T02:14:33Z"} → SENT
  ```

- **Error log**:  
  `plugins/chathook/logs/error.log`  
  Records HTTP failures and exceptions. If `debug: true`, full stack traces are included.

---

## Code Structure

```
src/main/java/com/playtheatria/chathook/
├─ Chathook.java                  
├─ commands/
│  └─ ChathookCommand.java        
├─ listeners/
│  └─ PrivateMessageListener.java 
└─ utils/
   ├─ ConfigManager.java          
   ├─ FileLogger.java             
   └─ HttpPostTask.java           
```

- **`Chathook.onEnable()`**:
  1. `saveDefaultConfig()`
  2. `configManager = new ConfigManager(this)`
  3. `logsDir = new File(getDataFolder(), "logs"); logsDir.mkdirs(); fileLogger = new FileLogger(this, logsDir)`
  4. Register `new PrivateMessageListener(this, configManager, fileLogger)`
  5. Register `new ChathookCommand(configManager, fileLogger)`

- **`PrivateMessageListener`**:
  - Listens for `AsyncChatEvent` where `ChatType.PRIVATE_MESSAGE` and recipient is `ZaraSprite`.
  - Builds JSON string and runs asynchronously:
    ```java
    HttpPostTask.postJson(jsonString, configManager);
    fileLogger.logToUserFile(senderName, jsonString, "SENT");
    ```

- **`ChathookCommand`** (implements `TabExecutor`):
  - `onCommand(...)` handles:
    - `reload` → `configManager.reload()`
    - `purger <username>` → `fileLogger.purgeUserLog(username)`
    - `purgeall` → `fileLogger.purgeAllLogs()`
  - `onTabComplete(...)` returns `["purgeall","purger","reload"]` if `args.length == 1`.

- **`ConfigManager`**:
  - Constructor takes `Chathook plugin`, reads `${dataFolder}/config.yml`.
  - `reload()` re-reads the same file.

- **`FileLogger`**:
  - Constructor takes `(JavaPlugin pluginInstance, File logsDir)`.
  - Methods: `logToUserFile(...)`, `logError(...)`, `purgeAllLogs()`, `purgeUserLog(...)`.

- **`HttpPostTask`**:
  - `public static void postJson(String jsonPayload, ConfigManager cfg)`
  - Uses Java 11’s `HttpClient` with `cfg.getTimeoutMs()` and retries `cfg.getRetryLimit()` times.

---

## Extending & Troubleshooting

- **Adjust JSON fields**: Edit `PrivateMessageListener.java`’s `String.format(...)` line.
- **Replace HTTP client**: Modify `HttpPostTask.java` to use OkHttp or another library.
- **Missing logs**: Ensure you DM exactly “ZaraSprite” (case-insensitive) in-game.
- **Endpoint errors**: Check `plugins/chathook/logs/error.log` for stack traces (set `debug: true`).
- **Permission issues**: Verify users have `chathook.mod` or `chathook.admin`.

---

## plugin.yml Reference

```yaml
name: chathook
version: 1.0.0
main: com.playtheatria.chathook.Chathook
api-version: "1.20"

commands:
  chathook:
    description: "Manage chathook: reload config or purge logs"
    usage: "/chathook <reload|purger <username>|purgeall>"
    permission: chathook.mod

permissions:
  chathook.admin:
    description: "Full access to chathook (assign mod, etc.)"
    default: op
    children:
      chathook.mod: true

  chathook.mod:
    description: "Can reload config and purge logs"
    default: false
```

---

