ZaraSprite Chat Relay
This project connects the Minecraft server Theatria with a GPT-powered assistant named ZaraSprite. Private messages (PMs) sent to ZaraSprite are logged by a lightweight Flask service, processed by a Node.js bot that handles session logic and GPT calls, and then delivered back in-game—ensuring responsive, stateful interactions without embedding AI directly on the server.

1. Components & Key Functions
1.1 /chathook (Paper Plugin)
Purpose
Intercept any private‐message (DM) sent to the in‐game player ZaraSprite and forward it immediately as JSON to the Flask service (/incoming-chat).

Key Responsibilities

Event Listener

Register a listener on Paper’s AsyncChatEvent with ChatType.PRIVATE_MESSAGE.

In the handler:

java
Copy
Edit
if (!event.getRecipient().getName().equalsIgnoreCase("ZaraSprite")) return;
String sender  = event.getSender().getName();
String message = event.message().toPlainText();
String timestamp = Instant.now().toString();
String id = UUID.randomUUID().toString();
HTTP POST

Build JSON payload:

jsonc
Copy
Edit
{
  "id": "uuid1",
  "player": "Alice",
  "message": "How do I warp home?",
  "timestamp": "2025-06-04T19:00:00Z"
}
POST to the Flask endpoint configured in config.yml (e.g. http://zarachat.duckdns.org:5000/incoming-chat), with:

timeout-ms: 5000

retry-limit: 3

debug: false

On failure, log the stack trace (if debug=true) to logs/chathook-error.log and retry with back-off.

Configuration (config.yml)

yaml
Copy
Edit
endpoint-url: "http://zarachat.duckdns.org:5000/incoming-chat"
timeout-ms: 5000
retry-limit: 3
debug: false
Permissions & Commands

Register /zarasprite reload and /zarasprite purge <username|all> under permission zarasprite.admin (ops only).

In onCommand, send an internal enqueue request to the Node bot (via database or another channel) so it can clear conversation history, then confirm in-game.

1.2 /chathook-flask (Flask Receiver)
Purpose
Receive, validate, and store incoming DMs in a single “pending” SQLite table. Flask remains a thin queue—no session logic or summarization on this side.

Key Responsibilities

HTTP Endpoint

Route: POST /incoming-chat

Expected JSON Payload:

json
Copy
Edit
{
  "id": "uuid1",
  "player": "SirMonkeyBoy",
  "message": "Where is the Smittiville Art Museum?",
  "timestamp": "2025-06-04T19:00:00Z"
}
Responses:

200 OK + {"status":"received", "id":"<uuid>"} on successful insert.

200 OK + {"status":"duplicate","id":"<uuid>"} if the ID already exists.

400 Bad Request + {"status":"error", "error":"… "} for malformed/missing fields.

500 Internal Server Error + {"status":"error", "error":"… "} on DB failure.

Data Validation

Ensure all four keys (id, player, message, timestamp) exist and are strings.

If any validation fails, return 400.

Data Storage

SQLite is used for its simplicity and concurrent-read support.

Schema (run at startup via init_db()):

sql
Copy
Edit
CREATE TABLE IF NOT EXISTS inbound_messages (
  id        TEXT   PRIMARY KEY,
  player    TEXT   NOT NULL,
  message   TEXT   NOT NULL,
  timestamp TEXT   NOT NULL,
  status    TEXT   NOT NULL CHECK(status IN ('PENDING','IN_PROGRESS','COMPLETE','FAILED'))
);
On each valid POST:

Insert a row into inbound_messages with status = 'PENDING'.

If id already exists, return duplicate.

On any DB exception (other than duplicate), return 500 and log to logs/flask-error.log.

Configuration (requirements.txt)

shell
Copy
Edit
Flask>=2.0
waitress>=2.0
Deployment

Use Waitress to serve on port 5000:

bash
Copy
Edit
python app.py
Expose only port 5000 in UFW (in addition to SSH).

DuckDNS domain (zarachat.duckdns.org) points to the VPS IP.

1.3 bot.js (Mineflayer + GPT Integration)
Purpose
Poll the SQLite DB for new DMs (PENDING rows), maintain per-player sessions, build OpenAI prompts, send GPT replies in-game, and manage session expiration & summarization—all in one Node process.

Key Responsibilities

Initial Connection & Setup

js
Copy
Edit
import { createBot } from 'mineflayer';
import sqlite3 from 'sqlite3';
import { openai } from 'openai';

const bot = createBot({
  host: 'mc.playtheatria.com',
  port: 25565,
  username: 'ZaraSprite',
  version: '1.20'
});

// Open SQLite (same DB file used by Flask)
const DB = new sqlite3.Database('/full/path/to/data/zara.db');
Database Schema (initialized at startup)

sql
Copy
Edit
-- inbound_messages already created by Flask
CREATE TABLE IF NOT EXISTS sessions (
  session_id   INTEGER PRIMARY KEY AUTOINCREMENT,
  player       TEXT,
  start_time   TEXT,
  end_time     TEXT,
  summary_text TEXT,
  is_archived  INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS conversation_history (
  msg_id     INTEGER PRIMARY KEY AUTOINCREMENT,
  player     TEXT   NOT NULL,
  direction  TEXT   NOT NULL CHECK(direction IN ('INBOUND','OUTBOUND')),
  text       TEXT   NOT NULL,
  timestamp  TEXT   NOT NULL,
  session_id INTEGER,
  FOREIGN KEY(session_id) REFERENCES sessions(session_id)
);
Polling Loop (every 2 seconds)

js
Copy
Edit
setInterval(() => {
  DB.all(
    "SELECT * FROM inbound_messages WHERE status = 'PENDING' ORDER BY timestamp ASC",
    [],
    (err, rows) => {
      if (err) {
        console.error("DB error fetching pending:", err);
        return;
      }
      rows.forEach(handleIncomingMessage);
    }
  );
}, 2000);
Handling Each Inbound DM

js
Copy
Edit
function handleIncomingMessage({ id, player, message, timestamp }) {
  // Mark as IN_PROGRESS to avoid duplication
  DB.run(
    "UPDATE inbound_messages SET status = 'IN_PROGRESS' WHERE id = ?",
    [id],
    (err) => {
      if (err) return console.error("Failed to mark IN_PROGRESS:", err);
      processMessage(id, player, message, timestamp);
    }
  );
}
Session Assignment & Message Storage

js
Copy
Edit
function processMessage(inboundId, player, message, timestamp) {
  // 1) Find most recent unarchived session for this player
  const timeoutMinutes = 20;
  const cutoff = new Date(Date.now() - timeoutMinutes * 60000).toISOString();

  DB.get(
    `SELECT s.session_id, MAX(c.timestamp) AS last_inbound
       FROM sessions s
       LEFT JOIN conversation_history c
         ON c.session_id = s.session_id AND c.direction = 'INBOUND'
      WHERE s.player = ? AND s.is_archived = 0
      GROUP BY s.session_id
      ORDER BY last_inbound DESC
      LIMIT 1`,
    [player],
    (err, row) => {
      if (err) return console.error("Error finding session:", err);

      if (row && row.last_inbound && row.last_inbound > cutoff) {
        // Continue existing session
        storeInbound(row.session_id, player, message, timestamp, inboundId);
      } else {
        // Create a new session
        const nowISO = new Date().toISOString();
        DB.run(
          "INSERT INTO sessions (player, start_time) VALUES (?, ?)",
          [player, nowISO],
          function (err) {
            if (err) return console.error("Error inserting session:", err);
            storeInbound(this.lastID, player, message, timestamp, inboundId);
          }
        );
      }
    }
  );
}
Saving Inbound, Calling GPT, & Sending Outbound

js
Copy
Edit
function storeInbound(session_id, player, message, timestamp, inboundId) {
  // a) Save the inbound message
  DB.run(
    `INSERT INTO conversation_history (player, direction, text, timestamp, session_id)
     VALUES (?, 'INBOUND', ?, ?, ?)`,
    [player, message, timestamp, session_id],
    (err) => {
      if (err) return console.error("Error inserting inbound chat:", err);

      // b) Build prompt from last N history rows + optional evergreen profile
      buildPrompt(player, session_id, (prompt) => {
        // c) Call OpenAI
        openai.chat.completions.create({
          model: "gpt-4o-mini",
          messages: prompt,
          timeout: 10000
        })
        .then((resp) => {
          const gptReply = resp.choices[0].message.content.trim();
          const replyTs = new Date().toISOString();

          // d) Save OUTBOUND reply
          DB.run(
            `INSERT INTO conversation_history (player, direction, text, timestamp, session_id)
             VALUES (?, 'OUTBOUND', ?, ?, ?)`,
            [player, gptReply, replyTs, session_id],
            (err) => {
              if (err) console.error("Error inserting outbound chat:", err);

              // e) Send reply in-game (chunk if > 200 chars)
              sendInGame(player, gptReply);

              // f) Mark inbound row as COMPLETE
              DB.run(
                "UPDATE inbound_messages SET status = 'COMPLETE' WHERE id = ?",
                [inboundId],
                (err) => {
                  if (err) console.error("Error marking COMPLETE:", err);
                }
              );
            }
          );
        })
        .catch((openaiErr) => {
          console.error("OpenAI error:", openaiErr);
          sendInGame(player, "Sorry, something went wrong. Please try again later.");
          DB.run(
            "UPDATE inbound_messages SET status = 'FAILED' WHERE id = ?",
            [inboundId]
          );
        });
      });
    }
  );
}
Session Expiry & Summarization

js
Copy
Edit
// Run every 5 minutes to archive timed-out sessions
setInterval(() => {
  const cutoff = new Date(Date.now() - 20 * 60000).toISOString();
  DB.all(
    `SELECT s.session_id
       FROM sessions s
       JOIN conversation_history c
         ON c.session_id = s.session_id AND c.direction = 'INBOUND'
      WHERE s.is_archived = 0
      GROUP BY s.session_id
      HAVING MAX(c.timestamp) <= ?`,
    [cutoff],
    (err, rows) => {
      if (err) return console.error("Error fetching expired sessions:", err);
      rows.forEach(({ session_id }) => summarizeSession(session_id));
    }
  );
}, 5 * 60 * 1000);

function summarizeSession(session_id) {
  DB.all(
    `SELECT direction, text
       FROM conversation_history
      WHERE session_id = ?
      ORDER BY timestamp ASC`,
    [session_id],
    (err, historyRows) => {
      if (err) return console.error("Error loading session for summary:", err);

      const transcript = historyRows
        .map(r => `${r.direction}: ${r.text}`)
        .join("\n");

      const summaryPrompt = [
        { role: "system", content: "You are summarizer for a Minecraft chat session." },
        { role: "user", content: `Summarize this conversation:\n${transcript}` }
      ];

      openai.chat.completions.create({
        model: "gpt-4o-mini",
        messages: summaryPrompt,
        timeout: 10000
      })
      .then((resp) => {
        const sessionSummary = resp.choices[0].message.content.trim();
        const endedAt = new Date().toISOString();

        DB.run(
          `UPDATE sessions
              SET end_time = ?, summary_text = ?, is_archived = 1
            WHERE session_id = ?`,
          [endedAt, sessionSummary, session_id],
          (err) => {
            if (err) console.error("Error archiving session:", err);
          }
        );
      })
      .catch((err) => {
        console.error("OpenAI summary error:", err);
      });
    }
  );
}
Slash-Command Handler

js
Copy
Edit
bot.on('chat', (username, message) => {
  if (username === bot.username) return; // ignore self
  if (!message.startsWith('/zarasprite ')) return;

  const args = message.slice(11).trim().split(' ');
  const sub = args.shift().toLowerCase();

  switch(sub) {
    case 'purge':
      // Delete all conversation_history rows for this player
      DB.run("DELETE FROM conversation_history WHERE player = ?", [username]);
      bot.chat(`/tell ${username} Your chat history has been cleared.`);
      break;

    case 'warp':
      if (args[0]) bot.chat(`/warp ${args[0]}`);
      else bot.chat(`/tell ${username} Usage: /zarasprite warp <location>`);
      break;

    case 'home':
      bot.chat(`/home`);
      break;

    default:
      // Treat as a fresh GPT request
      const inboundId = require('uuid').v4();
      const ts = new Date().toISOString();
      DB.run(
        `INSERT INTO inbound_messages (id, player, message, timestamp, status)
         VALUES (?, ?, ?, ?, 'PENDING')`,
        [inboundId, username, message, ts],
        (err) => {
          if (err) console.error("Error queueing command as GPT request:", err);
        }
      );
      break;
  }
});
2. Technical Summary
2.1 Project Stack
Java (17+) / Maven for /chathook Paper plugin

Target: Paper 1.20+

Dependencies:

Paper API

Java 11 HttpClient (built-in)

SnakeYAML (for config.yml)

Gson or Jackson (for JSON payloads)

Python 3.10+ / Flask 2.x / Waitress for /chathook-flask

Dependencies (in requirements.txt):

Flask

waitress

Node.js 20+ / Mineflayer for bot.js

Dependencies (package.json):

mineflayer

sqlite3 or better-sqlite3

openai (official Node SDK)

uuid

Data Store:

SQLite (single file zara.db under /data)

Chosen for zero-install, concurrent-read support (Flask writes, Node reads), and easy queries.

Hosting / OS:

LiquidWeb Cloud VPS, Ubuntu 22.04 LTS

DuckDNS domain: zarachat.duckdns.org → 94.156.149.73 (auto-updated via cron)

Open ports: 22 (SSH), 5000 (Flask)

ufw rules:

bash
Copy
Edit
sudo ufw allow 22
sudo ufw allow 5000
sudo ufw enable
(Optional) In future, front Flask with NGINX + Let’s Encrypt if TLS is required.

3. Folder & File Layout
bash
Copy
Edit
/ (repo root)
├─ README.md
├─ .env                  # Contains OPENAI_API_KEY and other secrets
├─ package.json          # For bot/ dependencies & scripts
├─ package-lock.json
├─ bot.js                # Node bot (Mineflayer + GPT logic)
/chathook                # Paper plugin
│   ├─ src/main/java/…   # Java source (ChatListener.java, CommandHandler.java, etc.)
│   ├─ config.yml        # Plugin settings (endpoint URL, timeouts, retry limits, debug)
│   └─ plugin.yml        # Bukkit plugin descriptor
/chathook-flask          # Flask receiver
│   ├─ app.py            # Defines POST /incoming-chat & `inbound_messages` table
│   ├─ requirements.txt  # Flask and Waitress
│   └─ data/             # Contains `zara.db` SQLite file
/logs                    # Central logs for debugging
│   ├─ flask-error.log
│   ├─ bot-error.log
│   └─ chathook-error.log
/prompts                 # System prompts, few-shot examples, template files
/diagrams                # Architecture diagrams and system flows
4. Full Workflow
Player DMs ZaraSprite in-game

/chathook Paper plugin intercepts the private message event, builds a JSON payload:

json
Copy
Edit
{
  "id": "uuid1",
  "player": "Alice",
  "message": "How do I warp home?",
  "timestamp": "2025-06-04T19:00:00Z"
}
Plugin POSTs to http://zarachat.duckdns.org:5000/incoming-chat with a 5 s timeout and up to 3 retries.

Flask (chathook-flask) receives the POST

Validates that id, player, message, and timestamp exist.

Inserts into inbound_messages with status = 'PENDING'.

Returns 200 OK (or 400/500 on error).

bot.js (Node) polls every 2 seconds

SELECT * FROM inbound_messages WHERE status = 'PENDING' ORDER BY timestamp ASC.

For each row:
a. Mark status = 'IN_PROGRESS'.
b. Find or create a session under sessions (20 min inactivity → new session).
c. Append the inbound message to conversation_history (INBOUND).
d. Build an OpenAI prompt using last N rows of conversation_history.
e. Call OpenAI; receive GPT reply.
f. Save GPT reply to conversation_history (OUTBOUND).
g. Send reply in-game via /tell <player> <gptReply> (chunk >200 chars, rate-limit).
h. Mark the original inbound_messages row as COMPLETE (or FAILED if GPT/API error).

Session Expiry & Summarization

Every 5 minutes, Node runs a query:

sql
Copy
Edit
SELECT s.session_id
  FROM sessions s
  JOIN conversation_history c
    ON c.session_id = s.session_id AND c.direction = 'INBOUND'
 WHERE s.is_archived = 0
 GROUP BY s.session_id
HAVING MAX(c.timestamp) <= datetime('now', '-20 minutes')
For each expired session:
a. Load all messages in that session (INBOUND & OUTBOUND).
b. Construct a summarization prompt:

makefile
Copy
Edit
Summarize this conversation between ZaraSprite and <player>:
INBOUND: Hello
OUTBOUND: Hi there! How can I help?
INBOUND: …
c. Call OpenAI to get a 2–3 sentence summary.
d. Update sessions with end_time, summary_text, and is_archived = 1.

Slash Commands

If a player types /zarasprite purge in chat:

Node runs DELETE FROM conversation_history WHERE player = ?

Replies in-game: “Your chat history has been cleared.”

If a player types /zarasprite warp <location>:

Node issues /warp <location> as ZaraSprite.

If a player types /zarasprite home:

Node issues /home.

Any other /zarasprite … text becomes a new GPT request, inserted into inbound_messages.

5. Roadmap (Sequential Milestones)
Project Bootstrap

Initialize Git repo with the folder structure above.

Provision LiquidWeb VPS: Ubuntu 22.04, UFW (22 & 5000 allowed), DuckDNS setup, install Python 3, Node 20, sqlite3, git.

Flask Receiver (§1.2)

Write app.py with POST /incoming-chat and inbound_messages schema.

Test locally with curl to ensure valid inserts and error handling.

Deploy on VPS; confirm zara.db is created under chathook-flask/data.

Paper Plugin (§1.1)

Scaffold Maven project under /chathook with ChatListener.java.

Implement POST to Flask, passing id, player, message, timestamp.

Test on Theatria dev server: DM ZaraSprite → inspect Flask’s DB for new rows.

Basic bot.js Setup (§1.3)

npm init; install mineflayer, sqlite3, openai, uuid.

Connect to Theatria as ZaraSprite.

Build DB helper that opens /data/zara.db.

Test: manually INSERT a row into inbound_messages; confirm bot.js picks it up, marks COMPLETE.

GPT Integration & In-Game Delivery

Flesh out the polling loop: load last 10 conversation_history rows, call OpenAI.

Save GPT’s reply, send via /tell. Confirm functionality in-game.

Session Expiry & Summarization Logic

Implement the “archive every 5 minutes” routine.

Confirm that sessions get end_time and summary_text after inactivity.

Slash-Command Handler Updates

Add /zarasprite purge, /zarasprite warp <location>, /zarasprite home.

Confirm that “pure GPT requests” (/zarasprite <text>) enqueue into inbound_messages.

Error-Handling & Observability

Finalize logs/flask-error.log, logs/bot-error.log, logs/chathook-error.log.

Add an admin command /zarasprite stats to show counts of:

Total PENDING inbound messages

Active sessions

Summarized sessions

(Optional) Hook into a Discord webhook on repeated GPT failures.

Documentation & Polish

Update this README.md to reflect final folder layout and workflows.

Write a short “Troubleshooting” doc for common errors: DB locks, plugin timeouts, GPT rate limits.

Add a Mermaid diagram under /diagrams/architecture.md illustrating the flow:

bash
Copy
Edit
Player DM → /chathook Plugin → Flask /incoming-chat (inbound_messages PENDING) 
            ↓                       ↑
            ↓— bot.js polls every 2s—→
            ↓                       ↑
bot.js writes → conversation_history (INBOUND/OUTBOUND); sends /tell in-game 
            ↓
Session expires → bot.js → Summarize → sessions (archived)
6. Deployment & Run Commands
6.1 Flask Receiver
SSH into VPS:

bash
Copy
Edit
ssh root@94.156.149.73
Navigate to the Flask folder and pull latest:

bash
Copy
Edit
cd ~/ZaraSprite/chathook-flask
git pull origin main
Ensure data/ exists and install dependencies:

bash
Copy
Edit
mkdir -p data
source venv/bin/activate
pip install -r requirements.txt
Run the Flask app:

bash
Copy
Edit
python app.py
Verify with curl:

bash
Copy
Edit
curl -i -X POST http://localhost:5000/incoming-chat \
  -H "Content-Type: application/json" \
  -d '{"id":"test123","player":"Tester","message":"hello","timestamp":"2025-06-04T20:00:00Z"}'
6.2 Node Bot
SSH into VPS (if not already):

bash
Copy
Edit
ssh root@94.156.149.73
Navigate to bot folder and pull latest:

bash
Copy
Edit
cd ~/ZaraSprite
git pull origin main
Install dependencies:

bash
Copy
Edit
cd ~/ZaraSprite
npm install
Ensure the same SQLite path is referenced in bot.js (e.g. /home/ubuntu/ZaraSprite/chathook-flask/data/zara.db).

Run the bot:

bash
Copy
Edit
node bot.js
7. Summary
Player DMs ZaraSprite → /chathook Paper plugin → Flask /incoming-chat → stored in inbound_messages (PENDING).

bot.js polls inbound queue → assigns/continues a session → saves inbound to conversation_history → calls GPT → saves outbound to conversation_history → sends reply in-game → marks inbound row COMPLETE.

Session expiry after 20 min idle → Node summarizes entire conversation via GPT → saves summary to sessions → marks session is_archived = 1.

Slash commands (/zarasprite purge, /zarasprite warp <...>, /zarasprite home) handled entirely in bot.js.

By keeping Flask as a minimal “pending queue” and centralizing all session, history, and summarization logic in Node, you maintain clear separation of concerns, simplify troubleshooting, and meet the project’s low-code, maintainable goals.

Questions or collaboration ideas? Message Zarathale on Theatria or Discord.
