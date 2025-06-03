# ZaraSprite Chat Relay

This project connects a Minecraft server to a lightweight GPT-powered assistant. Private messages (PMs) sent to **ZaraSprite** on Theatria are logged and forwarded to a backend service for review and processing.

---

## 📁 Project Structure

```
/chathook         → Paper plugin (Java) installed on Theatria to POST ZaraSprite PMs to Flask
/chathook-flask   → Python Flask server running on VPS to receive and log chat POSTs
/chat-logger      → Deprecated earlier attempt at logging system (archived)

/data             → Stored message logs or payloads
/diagrams         → Architecture diagrams and flowcharts
/node_modules     → Node dependencies for the bot
/prompts          → Few-shot examples and prompt templates for GPT
/src              → Main bot logic (JavaScript)
/tools            → Utilities and helper scripts
/troubleshooting  → Logs, screenshots, and debug notes

.env              → Environment variables (e.g. API keys, tokens)
.gitattributes    → Line-ending and diff configuration
.gitignore        → Files and folders excluded from Git
bot.js            → Mineflayer bot entrypoint for ZaraSprite
LICENSE           → Project license
package.json      → Node.js project metadata
package-lock.json → Dependency lock file
README.md         → This file
```

---

## 🖥️ VPS Setup

ZaraSprite runs on a dedicated VPS and uses Flask to catch incoming chat data from the server plugin.

**Domain**: `zarachat.duckdns.org`  
**Public IP**: `50.28.105.83`  
**Host**: LiquidWeb Cloud VPS (Ubuntu 22.04 LTS)

**Installed stack:**
- Python 3.10+  
- Flask  
- Waitress (for production serving)  
- Node.js 20+  
- Mineflayer  
- UFW firewall (ports 22, 5000 open)  

---

## 🛠️ Workflow Summary

1. A player DMs `ZaraSprite` in-game.
2. `chathook` plugin on Theatria extracts and POSTs the plain text message to Flask.
3. Flask (`chathook-flask`) receives the POST and stores the message.
4. ZaraSprite (Node bot) can use this log to trigger GPT interactions or moderation tools.

---

Questions? Ping Zarathale on Theatria or Discord.
