# ZaraSprite Chat Relay

This project connects the Minecraft server **Theatria** with a GPT-powered assistant named **ZaraSprite**. Private messages sent to ZaraSprite in-game are captured by a lightweight Paper plugin, routed through a Flask service, processed by a Node.js bot that handles session logic and GPT calls, and then delivered back to players—ensuring responsive, stateful interactions without embedding AI directly on the Minecraft server.

---

## Overview

- **Primary Goal**  
  Enable players to DM ZaraSprite in Minecraft and receive AI-generated responses in real time, while keeping all AI and session logic off the game server.

- **Key Advantages**  
  1. Keeps Paper server performance unaffected by AI calls.  
  2. Centralizes message tracking and session management in a single SQLite database.  
  3. Provides clear separation of concerns:  
     - Minecraft plugin only forwards messages  
     - Flask service only logs and validates  
     - Node.js bot handles all AI interactions and replies  

---

## 1. Components & Responsibilities

### 1.1 chathook (Paper Plugin)
- **Purpose**  
  Listens for any private-message (DM) sent to the player `ZaraSprite` and immediately forwards it—as a simple JSON payload—to a Flask endpoint.
- **Responsibilities**  
  1. Register an event listener on Paper’s AsyncChatEvent that filters for chat type `PRIVATE_MESSAGE`.  
  2. When ZaraSprite receives a DM, extract:  
     - Sender’s username  
     - Plain-text message content  
     - Current timestamp  
     - A unique message ID  
  3. Build a minimal JSON object containing those fields (id, player, message, timestamp) and POST it to the Flask server URL specified in `config.yml`.  
  4. On HTTP failure, retry a configurable number of times and log errors (stack traces only if debug mode is enabled).  
  5. Provide two administrative commands (for ops only):  
     - Reload plugin configuration without restarting the server  
     - Purge conversation history for a given username or for all players (in coordination with the Node.js bot)  

### 1.2 chathook-flask (Flask Receiver)
- **Purpose**  
  Expose a single HTTP endpoint that receives incoming JSON messages from the Paper plugin, validates them, and stores them in an SQLite table as a lightweight “pending queue.”  
- **Responsibilities**  
  1. Define a `POST /incoming-chat` route that expects a JSON object with exactly four string fields: `id`, `player`, `message`, `timestamp`.  
  2. Return a simple JSON response indicating success or failure:  
     - **Success**: Status “received” and echo back the message ID  
     - **Duplicate**: Status “duplicate” if that ID already exists  
     - **Bad Request**: Status “error” with a brief explanation if validation fails  
     - **Server Error**: Status “error” if any database insertion fails unexpectedly  
  3. On valid requests, insert a new row into an `inbound_messages` table, setting the initial status to `PENDING`.  
  4. Log any internal errors to a dedicated `flask-error.log` file.  
  5. Keep the Flask app as a minimal queueing layer—no GPT calls or session logic on this side.

### 1.3 bot.js (Node.js + Mineflayer + GPT Integration)
- **Purpose**  
  Poll the SQLite database for newly arrived messages, maintain per-player sessions, build and send prompts to the OpenAI API, and relay GPT replies back into Minecraft via `/tell`.
- **Responsibilities**  
  1. Open a connection as the Minecraft account `ZaraSprite` using Mineflayer.  
  2. Periodically (every 2 seconds or so) query the `inbound_messages` table for rows with status `PENDING`, ordered by timestamp.  
  3. For each pending message, mark it `IN_PROGRESS`, then either attach it to an existing session (if the player has chatted within the last 20 minutes) or create a new session.  
  4. Record the inbound chat in a `conversation_history` table (tagged as `INBOUND`).  
  5. Assemble a GPT prompt using the last N entries from that player’s conversation history plus any evergreen profile data.  
  6. Send the assembled prompt to OpenAI (e.g., GPT-4) and await a response.  
  7. When a reply arrives:  
     - Store it in `conversation_history` as `OUTBOUND`  
     - Chunk lengthy replies if necessary (Minecraft chat has a character limit)  
     - Send the reply back in-game with a `/tell`.  
     - Mark the original inbound row as `COMPLETE` (or `FAILED` if the AI call failed).  
  8. Every five minutes, identify any session that has seen no new inbound messages in the last 20 minutes; treat it as expired, send its full transcript to GPT for a short summary, and archive that session with summary text.  
  9. Listen for slash commands in Minecraft:  
     - `/zarasprite purge` clears that player’s conversation history in Node’s DB and confirms with an in-game message.  
     - Any other `/zarasprite <text>` is treated as a fresh GPT request and is re-queued into `inbound_messages`.  

---

## 2. Technical Summary

- **Java & Bukkit**  
  - OpenJDK 17+  
  - Maven for building the Paper plugin under `/chathook`  
  - Paper API (version 1.20+)  
  - Built-in Java 11 HttpClient for HTTP POSTs  
  - SnakeYAML for reading `config.yml`  
  - Gson or Jackson for JSON serialization  

- **Python & Flask**  
  - Python 3.10+  
  - Flask 2.x (for the HTTP receiver)  
  - Waitress (production WSGI server)  
  - SQLite (zero-dependency file‐based database)  
  - `requirements.txt` pins Flask and Waitress  

- **Node.js & Mineflayer**  
  - Node.js 20+  
  - Mineflayer (to connect as the Minecraft bot)  
  - `sqlite3` (or `better-sqlite3`) to read/write the same SQLite file used by Flask  
  - Official `openai` SDK for GPT calls  
  - `uuid` for generating unique inbound IDs  
  - A simple JSON-based config or `.env` for OpenAI API key  

- **Data Store**  
  - Single SQLite file (`/chathook-flask/data/chat.db`)  
  - Tables:  
    - `inbound_messages` (id, player, message, timestamp, status)  
    - `sessions` (session_id, player, start_time, end_time, summary_text, is_archived)  
    - `conversation_history` (msg_id, player, direction, text, timestamp, session_id)  

- **Hosting & Network**  
  - LiquidWeb Cloud VPS (Ubuntu 22.04 LTS)  
  - DuckDNS domain `zarachat.duckdns.org` pointing to the VPS IP  
  - UFW firewall rules allowing SSH (22) and Flask (5000)  
  - (Optional future step: front Flask with NGINX + Let’s Encrypt for TLS)

---

## 3. Folder & File Layout

/ → Repository root
├─ README.md → This overview file (project summary, tech stack, workflow)
├─ .env → Environment variables (e.g., OPENAI_API_KEY)
├─ bot.js → Node.js entry point: Mineflayer + GPT integration
├─ package.json → Node.js project metadata & dependencies
├─ package-lock.json → Locked Node dependencies
├─ /chathook → Paper plugin module
│ ├─ src/main/java → Java source code (listener, commands, utils)
│ ├─ config.yml → Plugin settings (endpoint URL, timeouts, retry limits, debug)
│ └─ plugin.yml → Bukkit plugin descriptor (name, version, commands)
├─ /chathook-flask → Flask service for incoming messages
│ ├─ app.py → Defines POST /incoming-chat & initializes the inbound_messages table
│ ├─ requirements.txt→ Flask and Waitress dependencies
│ └─ data/ → Contains the SQLite file (zara.db)
├─ /logs → Centralized log files for troubleshooting
│ ├─ flask-error.log → Flask server errors
│ ├─ bot-error.log → Node bot runtime errors
│ └─ chathook-error.log → Plugin HTTP or JSON errors
├─ /prompts → System prompts, example templates, few-shot files for GPT
├─ /diagrams → Architecture diagrams, sequence flows (Mermaid or image files)
└─ /troubleshooting → Notes, screenshots, common-error docs (for DB locks, rate limits, etc.)

---

## 4. Full Workflow (Text-Based)


1. **Player DM**  
   A Minecraft player opens chat and sends a private message to `ZaraSprite`.

2. **Paper Plugin → Flask**  
   The `chathook` plugin sees the DM event (`AsyncChatEvent` with `ChatType.PRIVATE_MESSAGE`), verifies the recipient is `ZaraSprite`, extracts sender, message, timestamp, and a UUID, then POSTs a JSON object to the Flask service at `/incoming-chat`.

3. **Flask Validation & Storage**  
   Flask validates that all required fields (`id`, `player`, `message`, `timestamp`) are present and are strings.  
   - If valid and not a duplicate, Flask inserts a new row into the `inbound_messages` table with status `PENDING` and returns a JSON acknowledgement.  
   - If the `id` already exists, Flask returns a “duplicate” status.  
   - If any field is missing or malformed, Flask returns a “400 Bad Request” with a brief error message.  
   - Any database failure yields a “500 Internal Server Error” and logs to `flask-error.log`.

4. **Node Bot Polling**  
   Independently, `bot.js` wakes up every two seconds and queries the `inbound_messages` table for rows marked `PENDING`, ordered by timestamp ascending.  
   - For each pending row, the bot marks it `IN_PROGRESS` (to avoid duplication) and invokes `processMessage`.

5. **Session Management**  
   In `processMessage`, Node determines whether the player already has an active session (no more than 20 minutes since their last inbound message).  
   - If an active session exists, the new message is appended to that session.  
   - Otherwise, a new session row is created in the `sessions` table (with `start_time = now`) and the message is attached to it.

6. **Conversation History & GPT Prompt**  
   Node writes the inbound chat into `conversation_history` (tagged as `INBOUND`).  
   It then gathers the last N history rows (both inbound and outbound) for that session, constructs a structured prompt array, and calls the OpenAI API with those messages as context.

7. **OpenAI Call**  
   When GPT replies with a text completion:  
   - Node writes the response into `conversation_history` (as `OUTBOUND`).  
   - If the API call fails or times out, Node handles the error (logs it and sends a fallback apology to the player, marking that row `FAILED`).  
   - Assuming success, the text is now ready to be sent back to the user.

8. **Form and Chunk Reply to User**  
   Before sending any reply back in-game, Node splits the GPT response into chunks that respect Minecraft’s chat limit (typically 256 characters per message). When chunking:  
   - Break at logical sentence or clause boundaries whenever possible, rather than strictly at the character limit.  
   - Do not split within a word or leave an awkward few words on a line by themselves.  
   - Each chunk should simulate a typing pause: send the first segment, wait a brief moment (e.g., 300–500 ms), then send the next. This “pausing” avoids flooding the server and mimics natural typing.  
   - If a single coherent thought slightly exceeds the limit, adjust the break point so it ends cleanly (for instance, move a few words back) rather than truncating mid-sentence.  

   Once chunked, Node issues:
   `/tell <player> <chunk>`
    for each part, respecting the simulated pause between them.

9. **Session Expiry & Summarization**  
    Separately, every five minutes, Node runs a query to find any session whose last inbound timestamp is more than 20 minutes ago (and is not yet archived).  
    - For each expired session, Node loads all `conversation_history` rows for that session, concatenates them into a short transcript, and sends a summarization prompt to GPT.  
    - When GPT returns a brief, 2–3 sentence summary, Node updates the `sessions` row with `end_time`, `summary_text`, and sets `is_archived = 1`.

10. **Slash Commands in Minecraft**  
    While the bot is online, it also listens for slash commands prefixed with `/zarasprite`:  
    - **`/zarasprite purge`** (no arguments)  
      - Deletes all conversation history for the issuing player from `conversation_history`.  
      - Replies in-game: “Your chat history has been cleared.”  
    - **`/zarasprite come here`**  
      - Issues a `/tpa <player>` request to the player.  
    - **`/zarasprite go home`**  
      - Issues an in-game home command (`/home alpha`).  
    - **`/zarasprite go to <username>`**  
      - Issues an in-game teleport request (`/tpa <username>`).  
    - **Any other `/zarasprite <text>`**  
      - Treated as a new GPT request and enqueued into `inbound_messages` exactly as if it were a direct DM (allowing command-style queries).  

    Each of these commands is handled just like an inbound chat: validated, logged, and—if needed—processed by the GPT workflow above.

---

## 5. Roadmap & Milestones

1. **Bootstrap & Infrastructure**  
   - Initialize Git repository with the folder layout described above.  
   - Provision LiquidWeb VPS (Ubuntu 22.04), configure UFW to allow ports 22 and 5000, install DuckDNS auto-updater.  
   - Create a Python virtual environment under `/chathook-flask`, install Flask and Waitress, and verify SQLite is available.

2. **Flask Receiver (chathook-flask)**  
   - Write and test `app.py` to accept `POST /incoming-chat`, validate JSON fields, and insert into SQLite.  
   - Confirm that duplicate inbound IDs are detected and return “duplicate.”  
   - Deploy Flask on the VPS with Waitress on port 5000 and test with `curl`.

3. **Paper Plugin (chathook)**  
   - Scaffold a Maven project under `/chathook`.  
   - Implement an AsyncChatEvent listener that filters for DM to ZaraSprite, extracts data, and HTTP-POSTs to Flask using Java 11 HttpClient.  
   - Add `config.yml` for `endpoint-url`, `timeout-ms`, `retry-limit`, and `debug`.  
   - Test locally on a dev Paper server: DM ZaraSprite → verify Flask database entry.  
   - Add admin commands (`reload`, `purge <username|all>`) that signal Node to clear conversation logs for a player or all players.

4. **Node Bot (bot.js)**  
   - Initialize `package.json` under project root, install `mineflayer`, `sqlite3`, `openai`, and `uuid`.  
   - Connect to Theatria as ZaraSprite, open a connection to the same SQLite file used by Flask.  
   - Implement the polling loop to fetch `PENDING` rows, mark them `IN_PROGRESS`, and hand them to `processMessage`.  
   - Build GPT prompt logic using the last N `conversation_history` rows; send OpenAI request.  
   - On GPT response, write to DB, send `/tell` back into Minecraft, and mark inbound row `COMPLETE`.  
   - Test by manually inserting rows into `inbound_messages` and confirming in-game replies.

5. **Session Expiry & Summaries**  
   - Add a scheduled task (every 5 minutes) to identify expired sessions (no inbound in 20 minutes).  
   - Generate a transcript, call OpenAI to create a summary, and update the session row with `end_time`, `summary_text`, and mark `is_archived`.  
   - Verify archived summaries appear in the database.

6. **Slash Commands & Miscellaneous**  
   - Handle `/zarasprite purge`, `/zarasprite warp <location>`, and `/zarasprite home` entirely within the Node bot.  
   - Confirm that arbitrary `/zarasprite <text>` commands enqueue a new `PENDING` row.  
   - Add error-handling to all database operations and OpenAI calls, logging exceptions to `bot-error.log`.

7. **Observability & Troubleshooting**  
   - Finalize logging:  
     - `chathook-error.log` for plugin HTTP or JSON errors  
     - `flask-error.log` for Flask failures  
     - `bot-error.log` for Node runtime errors  
   - Create a brief troubleshooting guide listing common issues (database locks, plugin timeouts, GPT rate-limit errors).  
   - (Optional) Fire a Discord webhook or email notification if repeated GPT failures are detected.

8. **Documentation & Polish**  
   - Update this `README.md` to reflect final folder structure, workflow, and steps.  
   - Write a short “Troubleshooting” markdown in `/troubleshooting` with commands to inspect SQLite, logs, and verify service health.  
   - Add a diagram file under `/diagrams/architecture.md` (Mermaid or image) showing the full message flow from Minecraft → Flask → Node → GPT → Minecraft.

---