# ZaraSprite Chat Relay

This project bridges the Minecraft server **Theatria** with a GPT-powered assistant named **ZaraSprite**. Private messages (PMs) to ZaraSprite are logged, interpreted, answered via GPT, and delivered back in-game — enabling rich, responsive interactions without server-side AI compute.

---

## 📁 Project Structure

```
/chathook         → Paper plugin: sends ZaraSprite PMs to the external Flask server
/chathook-flask   → Flask app: receives POSTs, logs messages, and stores them for processing
/chat-logger      → (Deprecated) legacy version of the chat pipeline

/data             → Stored message logs and conversation artifacts
/diagrams         → Architecture diagrams and system flows
/node_modules     → Node dependencies for bot runtime
/prompts          → System prompts, few-shot examples, and templates
/src              → JavaScript source for ZaraSprite logic
/tools            → Helper scripts and utilities
/troubleshooting  → Debug notes, logs, and screenshots

.env              → Environment variables (API keys, etc.)
.gitattributes    → Line-ending and diff configuration
.gitignore        → Files/folders excluded from Git
bot.js            → Entry point for the ZaraSprite Node bot
LICENSE           → Project license
package.json      → Node.js project metadata
package-lock.json → Dependency lock file
README.md         → This file
```

---

## 🧠 System Components

- **Trigger Handling System**  
  A watcher script polls the message log or listens via webhook to detect valid prompts (e.g., `/zarasprite how do I...`). This determines when to trigger a GPT query.

- **GPT Integration Layer**  
  Detected queries are passed to the OpenAI API using structured system + user prompts. Responses are parsed and queued for in-game delivery.

- **Bot Response Delivery**  
  The ZaraSprite bot (using Mineflayer or similar) sends the GPT reply via `/tell <player> <message>`. Messages are chunked and rate-limited to stay server-safe.

- **Conversation Session Management**  
  Tracks ongoing player conversations. Maintains context across turns and ends the session after inactivity.

- **Feedback & Logging**  
  All interactions (queries, responses, errors, and feedback) are logged for review. Helps detect issues, improve prompts, and monitor behavior.

---

## 🌐 VPS & Network

**Domain**: `zarachat.duckdns.org`  
**Public IP**: `50.28.105.83`  
**Host**: LiquidWeb Cloud VPS (Ubuntu 22.04 LTS)

**Installed stack:**
- Python 3.10+ (Flask + Waitress)
- Node.js 20+ (Mineflayer bot)
- UFW firewall (ports 22, 5000 open)
- `duckdns` auto-updater (via cron)

---

## 🔁 Full Workflow

1. A Minecraft player DMs `ZaraSprite`.
2. `chathook` plugin extracts plain text and POSTs it to the Flask endpoint.
3. Flask logs the message.
4. Trigger watcher evaluates if the message should be sent to GPT.
5. If yes, it formats the prompt and sends it to OpenAI.
6. GPT responds with a helpful reply.
7. The bot delivers the response in-game using `/tell`.
8. Session context and feedback are stored for future refinement.

---

Questions or collaboration ideas? Message **Zarathale** on Theatria or Discord.
