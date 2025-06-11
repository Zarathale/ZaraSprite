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
| **Goal** | Teach one focused skill (e.g., “Buy an item from a shop”). |
| **Structure** | Ordered steps → checkpoints → completion flag. |
| **Triggers** | Player asks, reaches a tag-matched event such as achieving a certain rank or completing an accomplishment, or ZaraSprite recommends it. |
| **Interactivity** | Pauses for in-world actions or chat responses. |
| **Adaptation** | Checks player verbosity preference. Stores progress for resuming later. |

> **Example Trailstones**  
> * `chestshop-buy` – Buy from a player shop  
> * `chestshop-sell` – Sell to a player shop  
> * `chestshop-create` – Create a chest shop of your own  
> * `shrine-quests` – Explain daily Shrine quests  
> * `tour-smittiville` – Get a guided tour of Smittiville  
> * `server-sell` – Sell items in your inventory to the server

---

## 🔧 Agents (Detailed)

| # | Agent | Core Responsibility |
|---|-------|---------------------|
| 1 | **📨 InputHandlerAgent** | Listens for DMs & mentions → writes to `inbound_messages`. |
| 2 | **💬 SessionManagerAgent** | Opens/closes sessions, manages expirations & escalations, notifies players. |
| 3 | **🧭 IntentAgent** | Classifies each message (tutorial request, question, TP, etc.) and routes tasks. |
| 4 | **🎭 PersonalityAgent** | Wraps raw GPT text in ZaraSprite’s warm, concise voice. |
| 5 | **🧍 MovementManagerAgent** | Teleports, follows, queues movement, tracks “busy” state. |
| 6 | **💡 ResponseEngineAgent** | Builds GPT prompts, checks wiki facts, reviews & filters final output. |
|6.1| > **Sub-agent: SafetyAgent** | Scans messages for risky or prohibited requests |
| 7 | **📚 WikiLookupAgent** | Searches the Theatria Wiki; returns snippets or links. |
| 8 | **🗺️ GuidanceAgent** (*Trailstones*) | Launches, pauses, resumes tutorial modules; suggests next Trailstones. |
| 9 | **🥰 MemoryManagerAgent** | Stores long-term player prefs (verbosity, completed lessons, past questions). |
|10 | **⏰ SchedulerAgent** | Runs timed jobs—queue updates, session timeouts, periodic nudges. |
|11 | **📢 CommunicatorAgent** | Sends all outbound `/tell` messages, queue notices, and tutorial steps. |
|12 | **🎲 IdeaSuggesterAgent** | Pulls and prioritizes player activity ideas based on mood, rank, or seasonal events. |

---

## 📚 Conversational Knowledge Domains

| Domain | ZaraSprite Must… | Example Follow-ups |
|--------|------------------|--------------------|
| **Ranks** | Identify player’s rank & perks; outline upgrade steps. | “Want a breakdown of costs for your next rank?” |
| **Commands** | Provide syntax, examples, cooldowns. | “Would `/sethome` help here?” |
| **Economy** | Explain how to make and spend Denarii. | “Shrine quests and player jobs are great ways to earn Denarii.” |

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

> **Player**: “ZaraSprite, explain player shops to SirMonkeyBoy.”

1. **InputHandlerAgent** logs message → `inbound_messages`.
2. **SessionManagerAgent** starts or updates session.
3. **IntentAgent** routes as a `tutorial-request`.
4. **GuidanceAgent** locates `shop-tour` Trailstones lesson.
5. **MovementManagerAgent** queues TP to `/warp market`.
6. **CommunicatorAgent** tells player: “Meet me at /warp market and we’ll start!”
7. **MemoryManagerAgent** checks player verbosity preference.
8. **GuidanceAgent** walks through tutorial steps.
9. On finish: **MemoryManagerAgent** logs progress, **CommunicatorAgent** suggests next Trailstones.

---

### 2 ️⃣ Player Question → Trailstones Suggestion

> **Player**: “ZaraSprite how do I earn Denarii?”

1. **InputHandlerAgent** writes to DB.
2. **SessionManagerAgent** manages session, notifies **IntentAgent**.
3. **IntentAgent** classifies as `economy-question`.
4. **ResponseEngineAgent** queries wiki via **WikiLookupAgent**, builds GPT prompt.
5. **PersonalityAgent** applies friendly tone to reply.
6. **CommunicatorAgent** responds:  
   “Shrine quests and player jobs are great ways to earn Denarii! Want to try one?”
7. If player accepts, **GuidanceAgent** launches corresponding Trailstones.
8. **MemoryManagerAgent** logs which ideas or tutorials were offered or completed.

---

### 3 ️⃣ “I’m Bored!” – Idea Suggestion Flow

> **Player**: “ZaraSprite, I’m bored. What could I do?”

1. **InputHandlerAgent** logs message.
2. **SessionManagerAgent** manages session state.
3. **IntentAgent** flags as `boredom-request`.
4. **IdeaSuggesterAgent** queries `fun_suggestions` table.
   - Filters by active status, player rank, tags.
   - Sorts by weight or seasonal/priority flags.
5. **CommunicatorAgent** replies:  
   “How about visiting the Ice Dragon? There's a bounty posted right now. Type /warp icelair to check it out!”
6. Optional: includes Trailstones link or clickable action.

---

## 🎲 Fun Suggestions Table

| Field | Description |
|-------|-------------|
| `id` | Unique idea ID |
| `title` | Activity title (e.g. “Slay the Ice Dragon”) |
| `player_text` | How ZaraSprite describes it (1–2 chat lines) |
| `trailstone_id` | Optional ID to launch tutorial |
| `wiki_url` | Optional link to more info |
| `tags` | e.g. `combat`, `market`, `seasonal`, `chill` |
| `weight` | Ranking score for how often to suggest |
| `expires_at` | Optional expiry (for events, contests, etc.) |
| `min_rank` | Optional rank gating |

> Alternate table name ideas: `fun_suggestions`, `boredom_fixes`, `spark_list`, `quick_quests`, `momentum_board`.

---

## 🌱 Future Enhancements

> * **Plugin Metrics** – analyze usage of Trailstones and interactions.  
> * **EmotionAgent** – adjust tone for urgency, celebration, or confusion.  
> * **EconomyAgent** – polls ChestDB listings and summarizes price trends.  
> * **McMMOAgent** – explains skills, XP levels, and ability progressions.  
> * **IdeaSuggesterAgent** – improves boredom-busting responses via DB-backed curation.

---

### Contributing

Follow the project’s **naming & static utility guidelines**:

* Class names match module (e.g., `Chathook.java`)  
* All logging via `FileLogger.logInfo(...)` static calls  
* Keep each class < 200 lines; break out utilities when needed  

---

*Happy learning and pathfinding with ZaraSprite 🧡*
