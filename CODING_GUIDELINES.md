# CODING_GUIDELINES.md

*ZaraSprite Coding Standards – Modular, Readable, and Scalable*

---

## 🔤 Naming Conventions

| Element Type | Convention | Example |
|--------------|------------|---------|
| Class         | PascalCase | `SessionManagerAgent`, `FriendlyPersona` |
| File Name     | Matches class | `SessionManagerAgent.java`, `Logger.java` |
| Package       | lower.snake_case | `com.playtheatria.zarasprite.agent` |
| Static Utility Method | camelCase | `logInfo(...)`, `sanitizeInput(...)` |
| Constant      | UPPER_SNAKE_CASE | `DEFAULT_TIMEOUT_MS`, `DEBUG_MODE` |

---

## 🏗️ Class Structure Guidelines

### ✅ General Principles

- One class per file, named to match the file.
- Limit class length to 200 lines when possible.
- Use **single-responsibility principle**: each class should own one piece of behavior.
- Separate concerns: Agents are stateful, Utilities are stateless.
- Prefer clarity over cleverness. Comment complex logic.

---

### 🧠 Core Class Types

#### ➕ **Agents** (Stateful)

- Live instance with memory or task queues.
- Communicate via event bus or shared database tables.
- Methods are typically instance methods (not static).

Examples:
```java
MovementManagerAgent.followPlayer(...)
SessionManagerAgent.getProgress(player)
```

#### ➕ **Utilities** (Stateless)

- Use static methods only.
- Never maintain internal state.
- Ideal for config, logging, formatting, math.

Examples:
```java
Logger.logInfo("Session started")
ConfigManager.get("timeout-ms")
WikiQuery.search("shrine quests")
```

---

## ⚙️ Static vs. Instance: Rule of Thumb

| Use Case | Use This |
|----------|----------|
| No memory, helper logic | `static` method in a Utility class |
| Tracks ongoing state | instance method on an Agent class |
| One copy shared across all instances | `class variable` |
| Unique to an instance | `instance variable` |

---

## 🧬 Polymorphism & Extensibility

### 🔸 TutorialModule Interface

Define a base class with shared method signatures:
```python
class TutorialModule:
    def start(self): ...
    def next_step(self): ...
```

Subclass for specific tutorials:
```python
class ShrineQuestModule(TutorialModule): ...
class ShopSetupModule(TutorialModule): ...
```

Call uniformly:
```python
current_module.next_step()
```

### 🔸 CommandHandler Registration

Each command is a handler subclass:
```python
class CommandHandler:
    def can_handle(self, command): ...
    def handle(self, args): ...
```

Dynamically dispatch:
```python
for handler in all_handlers:
    if handler.can_handle(cmd): return handler.handle(args)
```

---

## 📁 Directory Structure

```
zarasprite/
├── agent/
│   ├── InputHandlerAgent.java
│   ├── SessionManagerAgent.java
│   └── ...
├── util/
│   ├── Logger.java
│   ├── ConfigManager.java
│   └── ...
├── module/
│   ├── TutorialModule.java
│   ├── ShrineQuestModule.java
│   └── ...
└── command/
    ├── CommandHandler.java
    ├── HelpCommand.java
    └── ...
```

---

## 🧪 Testing & Readability

- Each class should include basic test hooks or logging.
- Use `FileLogger.logInfo()` to trace activity during runtime.
- Keep method names descriptive and action-based: `handleMessage`, `queueTeleport`, `startSession`.

---

## ✅ Summary Checklist

- [ ] Class matches file name (e.g., `WikiQuery.java`)
- [ ] Follows single-responsibility principle
- [ ] No runtime state in utility classes
- [ ] Uses polymorphism for tutorial and command modules
- [ ] Static where appropriate, instance where necessary
- [ ] Adheres to naming, packaging, and file structure standards

---

*Write clearly. Build modularly. Teach joyfully.*
