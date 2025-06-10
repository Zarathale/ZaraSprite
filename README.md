# ZaraSprite Multi-Agent Architecture

ZaraSprite is an in-game assistant for Theatria, powered by a modular, multi-agent system. Each agent focuses on a specific domain of interaction—enabling ZaraSprite to maintain stateful, responsive, and friendly conversations across multiple players while also demonstrating actions, navigating in-game, and enforcing safety checks on its answers.

---

## 🧠 Agent-Based Architecture Overview

Each component listed below is designed to operate as a distinct "agent"—independently responsible for a specific part of ZaraSprite's behavior. Agents communicate through a shared database, task queues, or function/event calls depending on runtime implementation.

---

### 1. 📨 InputHandlerAgent
**Purpose**: Listens for all messages directed at or mentioning ZaraSprite.

- Triggers on:
  - DMs to ZaraSprite
  - Mentions in global chat
- Writes new messages to the `inbound_messages` table
- Notifies `SessionManagerAgent` of activity

---

### 2. 💬 SessionManagerAgent
**Purpose**: Tracks and manages ongoing player sessions.

- Starts and ends sessions per player
- Handles session escalation (e.g. player requests Zara to visit them)
- Manages:
  - Session expiration
  - Queueing of movement tasks
  - Notifies players of session state

---

### 3. 🧭 IntentAgent
**Purpose**: Infers the player's objective from chat content.

- Extracts intent (e.g., "ask question", "request help", "request tp")
- Maps to appropriate actions/modules (e.g., Movement, Wiki Lookup, Guidance)
- Passes intent to the appropriate agent(s)

---

### 4. 🎭 PersonalityAgent
**Purpose**: Styles ZaraSprite’s tone and delivery.

- Adds personality, consistent speech patterns, warmth
- Adjusts message length and clarity based on audience or complexity
- Wraps final GPT output in ZaraSprite’s voice

---

### 5. 🧍 MovementManagerAgent
**Purpose**: Handles ZaraSprite's physical location and navigation.

- Maintains a teleport queue
- Responds to `follow`, `come to`, `wait here`, or `cancel` commands
- Reports current activity to other agents
- Manages “busy” state and estimates

---

### 6. 💡 ResponseEngineAgent
**Purpose**: Orchestrates prompt construction, GPT interaction, and quality control.

- Includes:
  - `PromptBuilder`: Builds prompt using session history + intent
  - `KnowledgeChecker`: Optionally checks against Theatria Wiki or internal rules
  - `ResponseReviewer`: Filters or rewrites output before delivery
- Sends styled message to `CommunicatorAgent`

---

### 7. 📚 WikiLookupAgent
**Purpose**: Retrieves info from the Theatria wiki or documentation.

- Uses search or predefined topics
- Optional fallback when GPT yields uncertain results
- Can be referenced by `ResponseEngineAgent` or `IntentAgent`

---

### 8. 🗺️ GuidanceAgent *(New!)*
**Codename**: "Trailstones"  
**Purpose**: Delivers pre-built interactive tutorials or demos.

- Each "Trailstone" is a guided module:
  - Scripted multi-step lesson
  - Includes pauses, checkpoints, and hints
  - Reacts to:
    - Player inputs (chat, command)
    - In-world actions (e.g. block placement, location changes)
- Supports:
  - Stopping, restarting, resuming modules
  - Encouragement nudges
  - “I’m stuck” helper logic

---

### 9. 🕰️ SchedulerAgent
**Purpose**: Manages timing, retry intervals, and polling tasks.

- Oversees async loops (e.g., polling new messages, checking queue)
- Times out expired sessions or teleport requests
- Sends periodic reminders when needed

---

### 10. 📢 CommunicatorAgent
**Purpose**: Sends all outbound messages or updates to players.

- Handles:
  - `/tell` messages
  - Session updates
  - Queue position reminders
  - Guided module prompts or nudges

---

## 🔧 Shared Utilities

| Utility         | Purpose |
|----------------|---------|
| `DatabaseManager` | Read/write to shared SQLite tables |
| `Logger`          | Central logging across agents |
| `ConfigManager`   | Loads personality settings, timing rules, etc |
| `EventBus` (optional) | In-memory queue for internal task handoff |

---

## 🧪 Example Flows

### 📦 Flow #1: Player asks ZaraSprite to teleport

> **Player**: `ZaraSprite can you come here?`

1. `InputHandlerAgent` logs message.
2. `SessionManagerAgent` sees this is an ongoing session.
3. `IntentAgent` identifies a teleport request.
4. `MovementManagerAgent` adds the player to TP queue.
5. If Zara is busy, `CommunicatorAgent` replies:  
   _“I’m helping someone else at the moment! You’re next in line 🧭 I’ll keep you posted!”_
6. Scheduler triggers regular queue updates.
7. Once Zara is free, `MovementManagerAgent` executes the teleport and updates session status.

---

### 💰 Flow #2: Player asks how to earn Denarii

> **Player**: `ZaraSprite how do I earn Denarii?`

1. `InputHandlerAgent` logs the message.
2. `SessionManagerAgent` confirms or creates a session.
3. `IntentAgent` identifies an economic/help request.
4. `ResponseEngineAgent`:
    - Builds prompt from session history
    - Queries wiki via `WikiLookupAgent` if needed
    - Gets GPT response:  
      _“There are three main ways to earn Denarii: completing jobs, trading with other players, and selling rare resources. Would you like a quick guide on the most profitable methods right now?”_
5. `PersonalityAgent` adjusts tone to make it warm and playful.
6. `CommunicatorAgent` delivers the message in-game.

---

## 🌱 Future Expansion Ideas

- **MemoryManagerAgent** for long-term player facts/preferences
- **SafetyAgent** to detect risky behavior or filter inappropriate requests
- **EmotionAgent** to adapt tone further based on context (e.g., urgent, celebratory)

---

Happy pathfinding with ZaraSprite 🧡
