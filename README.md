# ZaraSprite Chat Relay

This project bridges the Minecraft server **Theatria** with a GPT-powered assistant named **ZaraSprite**. Private messages (PMs) to ZaraSprite are logged, interpreted, answered via GPT, and delivered back in-game — enabling rich, responsive interactions without server-side AI compute.

---

## 1. Components & Key Functions

### 1.1 `/chathook` (Paper Plugin)

* **Purpose**: Intercepts any private‐message (DM) sent to the in‐game player `ZaraSprite` and immediately forwards it as JSON to the Flask receiver.
* **Key Responsibilities**:

  1. **Event Listener**

     * Register a listener for Paper’s `AsyncChatEvent` with `ChatType.PRIVATE_MESSAGE`.
     * In the handler:

       ```java
       if (!event.getRecipient().getName().equalsIgnoreCase("ZaraSprite")) return;
       String sender = event.getSender().getName();
       String message = event.message().toPlainText();
       ```
  2. **HTTP POST**

     * Build a JSON payload—e.g.

       ```jsonc
       {
         "id": "<UUID>",
         "player": "Alice",
         "message": "How do I warp home?",
         "timestamp": "2025-06-04T19:00:00Z"
       }
       ```
     * POST to the Flask endpoint defined in `config.yml` (e.g. `http://zarachat.duckdns.org:5000/incoming-chat`), with a 5 s timeout and up to 3 retry attempts.
     * If the POST fails, log the error (with stack trace if `debug: true`) to `logs/chathook-error.log` and enqueue a retry (back-off).
* **Configuration (`config.yml`)**:

  ```yaml
  endpoint-url: "http://zarachat.duckdns.org:5000/incoming-chat"
  timeout-ms: 5000
  retry-limit: 3
  debug: false
  ```
* **Permissions / Commands**:

  * Register `/zarasprite reload` and `/zarasprite purge <username|all>` as op-only commands (permission `zarasprite.admin`).
  * In `onCommand`, simply enqueue a special “internal” request that instructs `bot.js` to clear conversation history, then respond with an in-game confirmation.

### 1.2 `/chathook-flask` (Flask Receiver)

* **Purpose**: Receive, validate, and store incoming DMs in a lightweight, queryable data store.
* **Key Responsibilities**:

  1. **HTTP Endpoint**

     * **Route:** `POST /incoming-chat`
     * **Payload Schema:**

       ```json
       {
         "id": "uuid1",
         "player": "SirMonkeyBoy",
         "message": "Where is the Smittiville Art Museum?",
         "timestamp": "2025-06-04T19:00:00Z"
       }
       ```
     * Return `200 OK` on success; `400 Bad Request` if JSON is malformed or missing fields; `500 Internal Server Error` if DB write fails.
  2. **Data Validation**

     * Ensure `id`, `player`, `message`, and `timestamp` are present and correctly typed.
     * If any key is invalid, return `400` with a JSON body explaining the error.
  3. **Data Storage**

     * Use **SQLite** (recommended over flat JSON) for concurrency and simple querying.
     * Schema (in `models.py` using SQLAlchemy or plain `sqlite3`):

       ```sql
       CREATE TABLE inbound_messages (
         id TEXT PRIMARY KEY,
         player TEXT NOT NULL,
         message TEXT NOT NULL,
         timestamp TEXT NOT NULL,
         status TEXT NOT NULL CHECK(status IN ('PENDING','COMPLETE','FAILED'))
       );
       CREATE TABLE conversation_history (
         msg_id INTEGER PRIMARY KEY AUTOINCREMENT,
         player TEXT NOT NULL,
         direction TEXT NOT NULL CHECK(direction IN ('INBOUND','OUTBOUND')),
         text TEXT NOT NULL,
         timestamp TEXT NOT NULL
       );
       ```
     * On each valid POST, insert a row into `inbound_messages` with `status = 'PENDING'`.
     * If insertion fails (e.g. DB locked), return `500` and log the exception to `logs/flask-error.log`.
* **Configuration (`config.py`)**:

  ```python
  DB_PATH = "./data/zara.db"
  DEBUG = False
  ```
* **Deployment**:

  * Use Waitress or Gunicorn to serve `app.py` on port 5000.
  * Expose only port 5000 (firewall only allows 22 and 5000).
  * DuckDNS domain points to the server IP: `zarachat.duckdns.org` (94.156.149.73) .

### 1.3 `bot.js` (Mineflayer + GPT Integration)

* **Purpose**:

  1. Poll the SQLite DB for new DMs (`PENDING`).
  2. Maintain per-player conversation history.
  3. For each new message, build an OpenAI prompt, call GPT, store the GPT response, and immediately send it in-game (either as chat or a game command).
  4. Handle in-game slash commands (`/zarasprite *`) for purge, warp, home, etc.
* **Key Responsibilities**:

  1. **Initial Connection**

     ```js
     const mineflayer = require('mineflayer');
     const bot = mineflayer.createBot({
       host: 'mc.playtheatria.com',
       port: 25565,
       username: 'ZaraSprite',
       version: '1.20' // Paper 1.20+ compatible
     });
     ```

     * On `bot.on('login')`, log “ZaraSprite connected.”
     * Listen for `bot.on('end')` and attempt a reconnect with exponential back-off.
  2. **Database Access**

     * Open a single SQLite connection (`zara.db`) in read/write mode (use the same DB path as Flask).
     * Provide helper methods:

       ```js
       async function fetchPendingMessages() { /* SELECT * FROM inbound_messages WHERE status='PENDING' ORDER BY timestamp ASC */ }
       async function markMessageComplete(id) { /* UPDATE inbound_messages SET status='COMPLETE' WHERE id = ? */ }
       async function saveToConversation(player, direction, text) { /* INSERT INTO conversation_history ... */ }
       async function purgeHistory(player) { /* DELETE FROM conversation_history WHERE player = ? */ }
       ```
  3. **Polling Loop (every 2 s)**

     ```js
     setInterval(async () => {
       const pending = await fetchPendingMessages();
       for (const row of pending) {
         const { id, player, message, timestamp } = row;
         // 1) Mark as ‘ACTIVE’ or lock to prevent duplication
         await markMessageInProgress(id);
         // 2) Load last 10 history rows for ‘player’
         const history = await fetchConversationHistory(player, 10);
         // 3) Build OpenAI prompt
         const prompt = buildPrompt(history, player, message); 
         // 4) Call OpenAI with a 10 s timeout
         let gptReply;
         try {
           gptReply = await callOpenAI(prompt);
         } catch (err) {
           await sendInGame(player, "Sorry, I’m having trouble right now. Please try again shortly.");
           await markMessageComplete(id, 'FAILED');
           continue;
         }
         // 5) Save GPT reply into conversation_history
         await saveToConversation(player, 'INBOUND', message);
         await saveToConversation(player, 'OUTBOUND', gptReply);
         // 6) Send reply in-game (chunk if > 200 chars)
         await sendInGame(player, gptReply);
         // 7) Mark inbound_messages as COMPLETE
         await markMessageComplete(id, 'COMPLETE');
       }
     }, 2000);
     ```

     * **Chunking Logic**: If `gptReply.length > 200`, split into 190-char chunks, send each with a 500 ms delay.
     * **Rate-Limiting**: If more than 3 messages are sent to the same player within 5 s, queue the extras and send them 1 s apart to avoid anti-spam kicks.
  4. **Slash-Command Handler**

     ```js
     bot.on('chat', async (username, message) => {
       if (username === bot.username) return; // ignore self
       if (!message.startsWith('/zarasprite ')) return;
       const args = message.slice(11).trim().split(' ');
       const sub = args.shift().toLowerCase();
       switch(sub) {
         case 'purge':
           await purgeHistory(username);
           await bot.chat(`/tell ${username} Your chat history has been cleared.`);
           break;
         case 'warp':
           if (args[0]) {
             await bot.chat(`/warp ${args[0]}`);
           } else {
             await bot.chat(`/tell ${username} Usage: /zarasprite warp <location>`);
           }
           break;
         case 'home':
           await bot.chat(`/home`);
           break;
         default:
           // Everything else gets forwarded to GPT via the polling system:
           const id = generateUUID();
           const timestamp = new Date().toISOString();
           await insertInboundMessage({ id, player: username, message: message, timestamp });
           break;
       }
     });
     ```

     * Any message not matching a known subcommand is treated as a “fresh GPT request,” inserted into `inbound_messages` with `status='PENDING'`.
  5. **Error Handling**

     * Wrap all DB calls in `try/catch`; on failure, log to `logs/bot-error.log` and continue (but do not exit).
     * If the OpenAI quota is reached or times out, send a fallback message in-game and skip that turn.

---

## 2. Technical Summary

### 2.1 Project Stack

* **Java (17+) / Maven** for the Paper plugin

  * Target: Paper 1.20+ server (Theatria).
  * Dependencies: Paper API, Java 11 HttpClient (built-in), SnakeYAML for `config.yml`, a small JSON library (Gson or Jackson) for building POST payloads.
* **Python 3.10+ / Flask 2.x / Waitress** for the REST receiver

  * Minimal dependencies:

    * `Flask` (for routing)
    * `SQLAlchemy` or built-in `sqlite3` for database access
    * `waitress` (production WSGI server)
* **Node.js 20+ / Mineflayer** for the bot

  * Dependencies:

    * `mineflayer` (connect to Theatria)
    * `sqlite3` or `better-sqlite3` (DB driver)
    * `openai` (official Node SDK)
    * Optional: `uuid` for generating UUIDs
* **Data Store**:

  * **SQLite** (single file `zara.db` under `/data`)

    * Chosen because it’s light, requires zero external setup, supports concurrent reads (Flask writes, bot reads), and easy to query.
    * A single-file DB avoids complications of schema migrations or JSON-parsing quirks.
* **Operating System / Hosting**:

  * **LiquidWeb Cloud VPS**, running **Ubuntu 22.04 LTS**

    * DuckDNS domain: `zarachat.duckdns.org` → `94.156.149.73` (auto-updated via `duckdns` cron job).
    * Open ports:

      * `22` (SSH for admin)
      * `5000` (Flask HTTP)
      * `25565` (Minecraft, forwarded via Theatria host; not directly on this VPS)
    * Specs (as provisioned):

      * CPU: 2 vCPU
      * RAM: 4 GB
      * Disk: 100 GB SSD
      * Ubuntu packages installed: `python3`, `python3-venv`, `nodejs`, `npm`, `sqlite3`, `git`, `ufw`.
    * Firewall (UFW) rules:

      ```bash
      sudo ufw allow 22
      sudo ufw allow 5000
      sudo ufw enable
      ```
    * SSL/TLS: Left unencrypted on 5000 (behind DuckDNS). If needed, consider running `nginx` as a reverse proxy with Let’s Encrypt in the future.&#x20;

### 2.2 Folder & File Layout

```
/ (root)
├─ .env                  # OPENAI_API_KEY, DB_PATH=./data/zara.db
├─ README.md             # This overview
├─ package.json          # Node dependencies & scripts (e.g., "start": "node bot.js")
├─ package-lock.json
├─ bot.js                # Node bot (Mineflayer + GPT logic)
/chathook                # Paper plugin
│   ├─ src/main/java/com/theatria/chathook/
│   │    ├─ ChatHookPlugin.java
│   │    ├─ ChatListener.java
│   │    ├─ CommandHandler.java
│   │    └─ util/HttpClientHelper.java
│   └─ config.yml
│   └─ plugin.yml
/chathook-flask          # Flask receiver
│   ├─ app.py
│   ├─ models.py
│   ├─ config.py
│   ├─ requirements.txt
│   └─ data/              # holds `zara.db` (SQLite file)
/data                    # Common data folder (zara.db if you prefer a single location)
/logs                    # All log files: bot-error.log, flask-error.log, chathook-error.log
/prompts                 # System prompts, few-shot examples, template files
/diagrams                # Architecture diagrams (optional, for reference)
```

---

## 3. Roadmap (Sequential Milestones)

1. **Project Bootstrap**

   * Initialize Git repo with the folder structure above.
   * Provision LiquidWeb VPS: Ubuntu 22.04 installation, basic firewall (`ufw`), DuckDNS setup, install Python & Node.

2. **Flask Receiver (§3.1)**

   * Write `app.py` with `POST /incoming-chat`.
   * Create SQLite schema: `inbound_messages`, `conversation_history`.
   * Test with `curl` from a separate machine:

     ```bash
     curl -i -X POST http://zarachat.duckdns.org:5000/incoming-chat \
       -H "Content-Type: application/json" \
       -d '{"id":"test1","player":"Tester","message":"hello","timestamp":"2025-06-04T19:00:00Z"}'
     ```
   * Verify that `zara.db` has that row with `status='PENDING'`.

3. **Paper Plugin (§3.2)**

   * Scaffold a Maven/Gradle project under `/chathook`.
   * Implement `ChatListener.java` → on `AsyncChatEvent`, POST to Flask using Java 11 HttpClient.
   * Add `config.yml` and `plugin.yml`.
   * Deploy JAR to Thetria test server; DM `ZaraSprite` in-game; confirm Flask logs it.

4. **Basic `bot.js` Setup (§3.3)**

   * `npm init` → install `mineflayer`, `sqlite3`, `openai`, `uuid`.
   * Connect `bot.js` to `mc.playtheatria.com` as `ZaraSprite`.
   * Build DB helpers: connect to `zara.db`, implement `fetchPendingMessages()` and `markMessageComplete()`.
   * Test: manually insert a row into `inbound_messages` via `sqlite3` CLI; confirm `bot.js` picks it up and marks it COMPLETE.

5. **GPT Integration & In-Game Delivery**

   * Flesh out the polling loop to load last 10 conversation rows, build a system prompt, call `openai.chat.completions.create(…)`.
   * If GPT replies with text, send via `/tell <player> <reply>`.
   * Validate chunking logic if the reply exceeds 200 characters.
   * Confirm in-game that `ZaraSprite` responds meaningfully (e.g., a canned test prompt).

6. **Command Handling (§3.4)**

   * Implement `/zarasprite purge` in `bot.js` (deletes from `conversation_history` and sends confirmation).
   * Implement `/zarasprite warp <location>` and `/zarasprite home` to call `/warp` or `/home` in-game.
   * Add these commands to `plugin.yml` so players see usage hints.

7. **Memory Improvements & Persistent Profiles**

   * Extend `conversation_history` with a `player_profiles` table:

     ```sql
     CREATE TABLE player_profiles (
       player TEXT PRIMARY KEY,
       profile_data TEXT,   -- JSON blob summarizing key facts
       last_updated TEXT
     );
     ```
   * On each inbound DM, if no profile exists, create a default profile skeleton (e.g., `{ "first_interaction": "2025-06-04T..." }`).
   * Allow GPT to reference both:

     1. The last N exchange rows from `conversation_history`.
     2. A short “profile\_data” JSON for evergreen context (e.g., “Alice is building a nether portal”).
   * Add a sub-command `/zarasprite profile` to display or edit the stored profile in-game.

8. **Branching Behaviors by DM Content**

   * Define keywords or prefixes that invoke special flows:

     1. **Tour Mode** (e.g., DM starts with `tour:`) → GPT responds with a guided tour script.
     2. **Shrine Guide** (e.g., DM contains “shrine”) → bot issues `/warp shrine` and prompts the user with instructions.
     3. **Economy Queries** (e.g., DM “money” or “earn”) → trigger a custom script that reads in-game economy data (if available) and replies with next steps.
   * Update `buildPrompt(...)` in `bot.js` to check for these patterns before defaulting to straight GPT.

9. **Error-Handling & Observability**

   * Finalize logging:

     * `logs/bot-error.log` (Node exceptions)
     * `logs/flask-error.log` (Flask exceptions)
     * `logs/chathook-error.log` (Java plugin HTTP failures)
   * Build an admin command `/zarasprite stats` that returns counts of:

     * Total DMs received (from `inbound_messages`)
     * Pending requests
     * Active conversation count (`SELECT COUNT(DISTINCT player) FROM conversation_history`)
   * If the polling loop in `bot.js` detects > 3 GPT failures in 5 minutes, send a Discord webhook to a designated admin channel (optional).

10. **Documentation & Final Polish**

    * Update `README.md` with:

      * This Project Overview (sections 1–3 above).
      * Clear instructions on how to build/deploy `/chathook` (Maven commands), how to start Flask (e.g., `python3 -m venv venv && source venv/bin/activate && pip install -r requirements.txt && waitress-serve --port=5000 app:app`), and how to run `bot.js` (e.g., `npm install && node bot.js`).
    * Create a short “Troubleshooting” doc for common errors: DB lock, GPT rate limits, plugin timeouts.
    * Draw a simple Mermaid diagram in `/diagrams/architecture.md` (or embed in `README.md`).

---

### Summary of How It Comes Together

1. **Player DMs** → `/chathook` Paper plugin → `Flask /incoming-chat` → stored as `PENDING` in SQLite.
2. **`bot.js` polls** every 2 seconds → for each `PENDING` row:

   * Load last 10 conversation rows (and any evergreen profile).
   * Construct system prompt + user message.
   * Call OpenAI → get `gptReply`.
   * Insert inbound/outbound rows into `conversation_history`.
   * Send in-game reply (chat or game command).
   * Mark `inbound_messages` as `COMPLETE`.
3. **Slash commands** (`/zarasprite *`) all funnel through `bot.js`, which updates the DB or issues Minecraft commands directly.

Over time, this sequence can be expanded into specialized flows (tours, shrine guidance, economy helper) simply by adding pattern checks inside `bot.js` before falling back on GPT. All persistent state (DMs, history, profiles) lives in a single SQLite file (`zara.db`), which both Flask and the Node bot share on your Ubuntu VPS at LiquidWeb.

This design meets your low-code preference by keeping the “GPT logic” in one place (`bot.js`), minimizing inter-language plumbing and allowing easy future extraction if needed.

---

Questions or collaboration ideas? Message **Zarathale** on Theatria or Discord.
