# ZaraSprite Tutorial Assistant – Multi-Agent Architecture  
*Guiding players through Theatria, one Trailstones lesson at a time.*

ZaraSprite is an **in-game tutorial assistant** for the public Minecraft server **Theatria**.  
Its core mission is to **teach**—delivering small, interactive lessons (called **Trailstones**) while also handling everyday questions about ranks, commands, the economy, and more.  
A **modular multi-agent system** keeps ZaraSprite responsive, knowledgeable, and able to move around the world to demonstrate concepts live.

---

## 🧠 Architecture at a Glance

| Layer | What it Does |
|-------|--------------|
| **Agents** | Independent workers—each owns one piece of ZaraSprite’s behavior. |
| **Shared Utilities** | Cross-cutting helpers (database, logging, config). |
| **SQLite + Event Bus** | Lightweight persistence and internal messaging. |
| **Minecraft Runtime Hooks** | Paper 1.20+ events for chat, DMs, player actions, and movement. |

Agents exchange tasks via a small **in-memory event bus** or by writing rows to **SQLite tables**.  
This keeps concerns separate yet lets every part of ZaraSprite “see” new data quickly.

---

## 🗺️ Tutorial Module System – *Trailstones*

| Feature | Details |
|---------|---------|
| **Goal** | Teach one focused skill (e.g., “Buy an item from a ChestDB shop”). |
| **Structure** | Ordered steps → checkpoints → completion flag. |
| **Triggers** | Player asks, clicks hint, reaches a tag-matched event, or ZaraSprite recommends it. |
| **Interactivity** | Pauses for in-world actions, quizzes, or chat responses. |
| **Adaptation** | Checks player verbosity preference & stores progress for resuming later. |

> **Example Trailstones**  
> * `shop-buy` – Buy from a player shop  
> * `shop-sell` – Create & configure a ChestDB shop sign  
> * `sleep`   – Understand sleeping rules in mining worlds  
> * `shrine`  – Complete a daily Shrine quest  

---

## 🔧 Agents (Detailed)

| # | Agent | Core Responsibility |
|---|-------|---------------------|
| 1 | **📨 InputHandlerAgent** | Listens for DMs & mentions → writes to `inbound_messages`. |
| 2 | **💬 SessionManagerAgent** | Opens/closes sessions, manages expirations & escalations, notifies players. |
| 3 | **🧭 IntentAgent** | Classifies each message (tutorial request, question, TP, etc.) and routes tasks. |
| 4 | **🎭 PersonalityAgent** *(opt.)* | Wraps raw GPT text in ZaraSprite’s warm, concise voice. |
| 5 | **🧍 MovementManagerAgent** | Teleports, follows, queues movement, tracks “busy” state. |
| 6 | **💡 ResponseEngineAgent** | Builds GPT prompts, checks wiki facts, reviews & filters final output. |
| 7 | **📚 WikiLookupAgent** | Searches the Theatria Wiki; returns snippets or links. |
| 8 | **🗺️ GuidanceAgent** (*Trailstones*) | Launches, pauses, resumes tutorial modules; emits tutorial prompts & hints. |
| 9 | **🥰 MemoryManagerAgent** | Stores long-term player prefs (verbosity, completed lessons, past questions). |
|10 | **⏰ SchedulerAgent** | Runs timed jobs—queue updates, session timeouts, periodic nudges. |
|11 | **📢 CommunicatorAgent** | Sends all outbound `/tell` messages, queue notices, and tutorial steps. |

> **Future Agents**  
> * **EmotionAgent** – adjust tone for urgency, celebration, or confusion  
> * **SafetyAgent** – scan messages for risky or prohibited requests  

---

## 📚 Conversational Knowledge Domains

| Domain | ZaraSprite Must… | Example Follow-ups |
|--------|------------------|--------------------|
| **Ranks** | Identify player’s rank & perks; outline upgrade steps. | “Want a breakdown of costs for the next rank?” |
| **Commands** | Provide syntax, examples, cooldowns. | “Would `/sethome` help here?” |
| **Economy** | Explain Denarii sources & sinks, market trends. | “Curious where ores sell best right now?” |

---

## 🔗 Shared Utilities

| Utility | Purpose |
|---------|---------|
| `DatabaseManager` | Single SQLite interface for agents. |
| `Logger` | Static logging (aligns with **FileLogger** conventions). |
| `ConfigManager` | Loads timeouts, endpoint URLs, personality toggles. |
| `EventBus` | In-memory publish/subscribe for fast task hand-off. |

---

## 🧪 Example Flows

### 1 ️⃣ Guided Shop Tour (*Trailstone*)

1. **Player** → “ZaraSprite, explain player shops to SirMonkeyBoy.”  
2. **InputHandler** logs the message.  
3. **SessionManager** opens/updates session.  
4. **IntentAgent** detects *tutorial* request (`shop-tour`).  
5. **MovementManager** queues `/warp market`.  
6. **Communicator** → “Meet me at /warp market and we’ll start!”  
7. **MemoryManager** asks for verbosity (if unknown).  
8. **GuidanceAgent** runs `shop-tour` Trailstones lesson:  
   - step messages, in-world pauses, question checks.  
9. On completion, **MemoryManager** records lesson; **Communicator** suggests next topics.

---

### 2 ️⃣ Quick Question (Non-tutorial)

> **Player**: “ZaraSprite how do I earn Denarii?”

1. Input → Session → Intent (`economy-question`).  
2. ResponseEngine builds prompt, queries Wiki.  
3. Communicator sends concise answer + optional Trailstones “shrine” or “jobs” suggestion.  
4. MemoryManager logs topic for future follow-up.

---

### 3 ️⃣ Teleport Request

> **Player**: “ZaraSprite can you come here?”

1. Intent (`tp-request`) → Movement queue.  
2. Communicator updates player on queue position.  
3. Scheduler emits periodic “you’re next” notices.  
4. MovementManager executes TP when idle.

---

## 🌱 Planned Enhancements

* **Refined EmotionAgent** – tone-aware prompts (“Congrats!” vs. “Let’s fix that”).  
* **SafetyAgent** – policy checks for exploits or personal data.  
* **Plugin Metrics** – anonymous usage to improve tutorial coverage.  

---

### Contributing

Follow the project’s **naming & static utility guidelines**:

* Class names match module (e.g., `Chathook.java`).  
* All logging via `FileLogger.logInfo(...)` static calls.  
* Keep each class < 200 lines; break out utilities when needed.  
* See `/chathook/CODING_GUIDELINES.md` for static vs. instance advice.

---

*Happy learning and pathfinding with ZaraSprite 🧡*
