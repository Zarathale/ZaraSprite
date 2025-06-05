🤖 AI Assistant Instructions for ZaraSprite Project

This file provides coding standards, naming rules, and scope boundaries for AI assistants (e.g., GitHub Copilot, ChatGPT VS Code extension) working on this project.

🧠 Project Summary

ZaraSprite is a GPT-integrated Minecraft bot for the Paper server Theatria. It handles private messages sent to the in-game player ZaraSprite, routes them to a Flask service, interprets them via GPT, and delivers responses back in-game.

🧱 Core Structure

/chathook: Paper plugin that intercepts PMs and sends POSTs to Flask

/chathook-flask: Flask server that logs messages for downstream GPT use

/src, bot.js: Node bot that reads messages, triggers GPT, and replies

🔧 Coding Standards

Java (Paper plugin)

Class names match file and plugin (Chathook.java, not ChathookPlugin.java)

Packages are lowercase (com.playtheatria.chathook)

Use static helper methods (e.g., FileLogger.logInfo(...)), do not instantiate utility classes

One class = one responsibility, keep under 200 lines

Use HttpClient from Java 11+ (no OkHttp)

Async tasks via Bukkit.getScheduler().runTaskAsynchronously(...)

JavaScript (bot.js)

Modular structure in /src

One file = one concern

Use Mineflayer for in-game chat

Python (Flask)

Keep app.py thin; offload logic to helpers

Log incoming messages, no AI logic here

📐 Naming Conventions

Class = CamelCase

Package = lowercase

Match plugin.yml main entry, class name, and file name exactly

All config values declared and validated in ConfigManager

✅ AI Preferences

Prioritize low-code, minimal-dependency solutions

Never include AI response logic in the Minecraft plugin

Respect async/thread safety in Bukkit

Keep Flask server stateless beyond logging

If logging, use FileLogger statically and write to plugins/chathook/logs/

💡 Context Clues

Player DMs /msg ZaraSprite ... → triggers GPT pipeline

Responses are sent using /tell <player> <message>

GPT logic lives downstream from the plugin — no reply logic inside Java

For deeper context, refer to README.md, bot.js, and architecture diagrams under /diagrams/.

