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

// --- Deep Flatten and Hybrid Sender/Message Extractor ---
function flattenAllText(node, result = [], inheritedColor = null) {
  if (!node) return result;
  if (typeof node === 'string') {
    result.push({ text: node, color: inheritedColor });
    return result;
  }

  if (Array.isArray(node)) {
    node.forEach(n => flattenAllText(n, result, inheritedColor));
    return result;
  }

  if (typeof node === 'object') {
    const color = node.color || inheritedColor;
    if (typeof node.text === 'string') {
      result.push({ text: node.text, color });
    }

    if (node?.toString?.name === 'ChatMessage') {
      flattenAllText(node.json, result, color);
    }

    ['extra', 'json', 'with', 'contents'].forEach(key => {
      if (node[key]) flattenAllText(node[key], result, color);
    });

    return result;
  }

  return result;
}

function extractDeepWhisper(jsonMsg) {
  try {
    const flat = flattenAllText(jsonMsg);

    // Try normal sender detection
    let sender = flat.find(f => f.color === 'gold')?.text?.trim();

    // Fallback: search hover metadata if sender not found
    if (!sender && jsonMsg.hoverEvent?.contents?.extra) {
      const hoverFlat = flattenAllText(jsonMsg.hoverEvent.contents.extra);
      const match = hoverFlat.find(f => f.text?.includes('Sender:'));
      const next = hoverFlat[hoverFlat.indexOf(match) + 1];
      if (next && config.testers.includes(next.text?.trim())) {
        sender = next.text.trim();
        logDebug("SenderFallback", { sender });
      }
    }

    const allPurples = flat.filter(f => f.color === 'light_purple' && f.text?.trim());
    const message = (allPurples.length > 0
      ? allPurples[allPurples.length - 1].text
      : flat.find(f => f.text?.trim() && f.text.length > 10)?.text
    )?.trim();

    if (!message) {
      logDebug("MessageFallback", flat);
    }

    return sender && message ? { sender, message } : null;
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
