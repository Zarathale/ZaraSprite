// == ZaraSprite: bot.js ==
// DM parsing using PM signature pattern

const mineflayer = require('mineflayer');

// --- Config ---
const config = {
  host: 'mc.playtheatria.com',
  port: 25565,
  username: 'ZaraSprite',
  auth: 'microsoft',
  version: '1.20.4',
  DEBUG_MODE: false
};

// --- Logging ---
function logInfo(label, message) {
  console.log(`[${new Date().toISOString()}] [INFO] [${label}] ${message}`);
}
function logError(label, message) {
  console.error(`[${new Date().toISOString()}] [ERROR] [${label}] ${message}`);
}
function logDebug(label, obj) {
  if (config.DEBUG_MODE) console.dir({ [label]: obj }, { depth: null });
}

// --- Connect ---
function connectToServer(cfg) {
  try {
    logInfo("Startup", `Connecting as ${cfg.username} to ${cfg.host}:${cfg.port}...`);
    const bot = mineflayer.createBot(cfg);
    bot.once('login', () => logInfo("Connection", `Successfully logged in as ${bot.username}`));
    bot.on('end', () => logInfo("Connection", `Bot has disconnected.`));
    bot.on('error', (err) => logError("Connection", err.message));
    return bot;
  } catch (err) {
    logError("Connection", err.message);
    return null;
  }
}

// --- DM Parser ---
function parsePMMessage(jsonMsg) {
  try {
    const base = jsonMsg.json;
    if (!base || !Array.isArray(base.extra)) return null;

    const extra = base.extra;
    if (!(extra[0]?.text === '[' && extra[1]?.text === 'PM')) return null;

    let sender = null;
    let receiver = null;
    let foundArrow = false;
    let foundClose = false;
    let messageParts = [];

    for (let i = 2; i < extra.length; i++) {
      const part = extra[i];
      if (part.text === ' -> ') {
        foundArrow = true;
      } else if (foundArrow && !receiver && part.color === 'light_purple') {
        receiver = part.text.trim();
      } else if (!foundArrow && part.color === 'gold') {
        sender = part.text.trim();
      } else if (part.text === '] ') {
        foundClose = true;
      } else if (foundClose && part.text?.trim()) {
        messageParts.push(part.text.trim());
      }
    }

    let message = messageParts.join(' ').replace(/\sflp[ms]_[0-9a-f\-]+\s*/g, '').trim();

    if (sender && message) {
      return { sender, receiver, message };
    }
    return null;
  } catch (err) {
    logError("DMParser", `Exception during parse: ${err.message}`);
    return null;
  }
}

// --- Listeners ---
function setupDirectMessageListener(bot) {
  bot.on('message', (jsonMsg) => {
    const parsed = parsePMMessage(jsonMsg);
    if (parsed) {
      logInfo("DM", `From: ${parsed.sender} | Message: ${parsed.message}`);
    }
  });
}

function setupMessageProbes(bot) {
  if (!config.DEBUG_MODE) return;
  bot.on('message', (jsonMsg) => {
    logInfo("RawMessage", jsonMsg.toString());
    logDebug("ParsedMessage", jsonMsg);
  });
}

// --- Entrypoint ---
function main() {
  const bot = connectToServer(config);
  if (!bot) {
    logError("Main", "Bot creation failed.");
    return;
  }
  setupMessageProbes(bot);
  setupDirectMessageListener(bot);
}

main();
