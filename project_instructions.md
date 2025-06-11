## 🧽 ZaraSprite Project Instructions (2025 Edition)

> **Project Purpose:**\
> ZaraSprite is an in-game tutorial assistant for Theatria. It teaches players core gameplay systems (like ChestDB shops, shrine quests, and ranks) using small, modular “Trailstones” lessons. It also answers common questions and moves around the world to demonstrate actions and give tours.\
> This is a **multi-agent, modular system** that balances fun, learning, and architecture clarity.

---

### 🗂️ 1. Central Project Files

- 🧠 **README.md**\
  Defines ZaraSprite’s core architecture, agents, and tutorial system. This is our *source of truth*.
- 🧱 **CODING\_GUIDELINES.md**\
  Defines naming, class structure, and static utility conventions. Follow these in every commit.
- 📚 **TUTORIALS.md** (or `lessons.yaml`)\
  Lists current Trailstones tutorials, triggers, and steps.
- 📋 **WORKFLOW\.md**\
  Explains how chat flows through the system (Input → Session → Intent → Response → Output).
- 🧾 **WIKI\_INTEGRATION.md**\
  Describes how Theatria's public wiki is structured and read by ZaraSprite. Includes markup conventions for summary blocks, article tags, and transparency practices.
- 🔍 **TODO.m**\
  For future ideas, refactor plans, enhancements. **Don’t creep scope** into active code unless it aligns with the README.

> ✅ **When in doubt, update README.md first before adding new logic.**

---

### ⚙️ 2. Development Workflow

#### ✅ Day-to-Day Coding

- Keep each class < 200 lines when possible.
- Use **single-responsibility** for agents and utilities.
- Static for stateless helpers (e.g. `FileLogger.logInfo(...)`), instance methods for stateful classes (like agents).
- Match filenames to class names and plugin.yml.
- Prefer low-code, readable solutions.
- Test changes in your local or VPS Minecraft test server.

#### ⟲ Pull Requests & Commits

Each PR must include:

- 📅 1-line summary ("Add MovementManagerAgent teleport queue logic")

---

### 🧱 3. Agent & Utility Expectations

**Agents (e.g. ************************************************************`SessionManagerAgent`************************************************************)**

- Listen or act on data/events
- Use SQLite or the EventBus
- Output to `CommunicatorAgent`

**Utilities (e.g. ************************************************************`ConfigManager`************************************************************)**

- Stateless
- Use static methods
- Never carry runtime state

---

### 📊 4. Naming & Structure Conventions

- `Chathook.java`, not `ChathookPlugin.java`
- `FileLogger.logInfo(...)`, never `new FileLogger()`
- `com.playtheatria.zarasprite.agent.SessionManagerAgent` – lowercase packages, CamelCase classes
- `plugin.yml` main class must match file structure

---

### 🔒 5. Scope Discipline

- **Warm, welcoming guide = ZaraSprite’s core mission**
- Wiki integration must stay aligned with Trailstones focus (summary first, detail below)
- New systems (like metrics, external APIs, or emotions) must be proposed and reviewed against overall scope and timelines
- Use `README.md` > `Future Enhancements` section to park big ideas

---

### 🗓️ When Things Get Messy

- Recenter using the README. Align to project mission. Prioritize.
- If a new need feels big (e.g., "EmotionAgent"), brainstorm through it. Consider options. Prioritize. Outline plan. Act/build/code/test/run.

---

**🦡 ZaraSprite succeeds when she stays focused, clear, and modular—just like the players she guides.**

