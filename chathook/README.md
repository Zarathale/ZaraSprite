## chathook Plugin for Paper 1.20+

**Purpose:**  
Intercept private messages (DMs) sent to the in-game player `ZaraSprite` and forward them as JSON to an external Flask endpoint. Does **not** handle GPT responses—this is done downstream by the Flask/Node components.

### 1. Installation

1. **Build the plugin JAR**  
   - Place all Java source files under `src/main/java/com/playtheatria/chathook/`.
   - Place `plugin.yml` under `src/main/resources/`.
   - Run `mvn package` (or your preferred build tool) to produce `chathook.jar`.

2. **Deploy to the server**  
   - Copy `chathook.jar` into `plugins/chathook.jar` on your Paper 1.20+ server.
   - Start (or restart) the server. On first run, `plugins/chathook/config.yml` will be generated with default values, and the folder `plugins/chathook/logs/` will be created.

### 2. Configuration

- Open `plugins/chathook/config.yml` to adjust the following keys:
  ```yaml
  endpoint-url: "http://zarachat.duckdns.org:5000/chat"
  timeout-ms: 5000
  retry-limit: 3
  debug: false
