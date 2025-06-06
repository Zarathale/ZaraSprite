# chathook (Paper 1.20+)

Intercepts private messages (DMs) sent to the in-game player `ZaraSprite`, forwards them as JSON to a configured HTTP endpoint, and logs each request under a configurable logs folder (`plugins/chathook/<log-folder>/`).

---

## Configuration

Edit `plugins/chathook/config.yml`

After saving changes, run: `/chathook reload` in-game to apply them without restarting the server.

---

## Code Structure

src/main/java/com/playtheatria/chathook/
├─ Chathook.java                    (main plugin class)
├─ commands/
│  └─ ChathookCommand.java          (handles reload & purge-all)
├─ listeners/
│  └─ PrivateMessageListener.java   (captures DMs, posts JSON, logs)
└─ utils/
   ├─ ConfigManager.java            (reads & validates config.yml)
   ├─ FileLogger.java               (singleton rolling-file logger)
   └─ HttpPostTask.java             (sends JSON via Java 11 HttpClient)

---

## Log Files

# Per-user logs
Path: plugins/chathook/<log-folder>/<username>.log
   *  Each line appended looks like:
      `[2025-06-05T02:14:33Z] {"username":"Zarathale","message":"Hey ZaraSprite!","timestamp":"2025-06-05T02:14:33Z","uuid":"..."} → SENT`

# Error log (rolling)
Path: plugins/chathook/<log-folder>/error.log (rotates to error.1.log, error.2.log, etc. once it exceeds log-max-bytes)
   *  Records HTTP failures and exceptions. 
   *  If debug: true, full stack traces appear.