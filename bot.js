// == ZaraSprite: bot.js ==
// Proof-of-concept with logging, command parsing, and config modularization

const mineflayer = require('mineflayer');

// --- Config (Inline for now) ---
const config = {
  host: 'mc.playtheatria.com',
  port: 25565,
  username: 'ZaraSprite',
  auth: 'microsoft',
  version: '1.20.4',
  DEBUG_MODE: true,
  testers: ['Zarathale']
};

// --- Utility Logger ---
function logInfo(label, message) {
  console.log(`[${new Date().toISOString()}] [INFO] [${label}] ${message}`);
}

function logError(label, message) {
  console.error(`[${new Date().toISOString()}] [ERROR] [${label}] ${message}`);
}

function logDebug(label, obj) {
  if (config.DEBUG_MODE) {
    console.dir({ [label]: obj }, { depth: null });
  }
}

// --- Connection ---
function connectToServer(cfg) {
  try {
    logInfo("Startup", `Connecting as ${cfg.username} to ${cfg.host}:${cfg.port}...`);
    const bot = mineflayer.createBot(cfg);

    bot.once('login', () => {
      logInfo("Connection", `Successfully logged in as ${bot.username}`);
    });

    bot.on('end', () => {
      logInfo("Connection", `Bot has disconnected.`);
    });

    bot.on('error', (err) => {
      logError("Connection", err.message);
    });

    return bot;
  } catch (err) {
    logError("Connection", err.message);
    return null;
  }
}

// --- Message Debug Probe ---
function setupMessageProbes(bot) {
  bot.on('message', (jsonMsg) => {
    logInfo("RawMessage", jsonMsg.toString());
    logDebug("ParsedMessage", jsonMsg);
  });
}

// --- Recursive Whisper Parser ---
function flattenExtras(node, list = []) {
  if (!node) return list;
  if (Array.isArray(node)) {
    node.forEach(n => flattenExtras(n, list));
  } else {
    if (typeof node.text === 'string') {
      list.push({ text: node.text, color: node.color });
    }
    if (node.extra) {
      flattenExtras(node.extra, list);
    }
  }
  return list;
}

function extractDeepWhisper(jsonMsg) {
  try {
    const flat = flattenExtras(jsonMsg?.json?.extra);
    const messageText = flat.map(e => e.text).join('').trim();

    const senderPart = flat.find(e => e.color === 'gold');
    const sender = senderPart?.text?.trim() || null;

    if (sender && messageText.includes(sender)) {
      const msg = messageText.split(sender).pop().replace(/^\W+/, '').trim();
      return { sender, message: msg };
    }

    return sender ? { sender, message: messageText } : null;
  } catch (err) {
    return null;
  }
}

function setupDirectMessageListener(bot) {
  bot.on('message', (jsonMsg) => {
    try {
      const parsed = extractDeepWhisper(jsonMsg);
      if (parsed) {
        logInfo("DM", `From: ${parsed.sender} | Message: ${parsed.message}`);
        logDebug("DM.full", jsonMsg);
      }
    } catch (err) {
      logError("DMListener", `Failed to parse whisper: ${err.message}`);
    }
  });
}

// --- Command Listener ---
function setupCommandListener(bot) {
  bot.on('message', (jsonMsg) => {
    try {
      const base = jsonMsg.json;
      if (!base || !Array.isArray(base.extra)) return;

      const firstPart = base.extra[0];
      if (!firstPart?.text || !config.testers.includes(firstPart.text.trim())) return;

      const text = base.extra.map(p => p.text).join('').trim();
      const match = text.match(/^ZaraSprite \/(.+)/);

      if (match) {
        const command = match[1];
        logInfo("CommandExec", `Executing: /${command}`);
        bot.chat(`/${command}`);
      }
    } catch (err) {
      logError("CommandListener", `Failed to parse command: ${err.message}`);
    }
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
  setupCommandListener(bot);
}

main();
