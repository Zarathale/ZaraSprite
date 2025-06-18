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

// --- Recursive Flattening Utility ---
function flattenAllText(node, result = [], inheritedColor = null, path = 'root') {
  if (!node) return result;
  if (typeof node === 'string') {
    result.push({ text: node.trim(), color: inheritedColor, path });
    return result;
  }
  if (Array.isArray(node)) {
    node.forEach((n, i) => flattenAllText(n, result, inheritedColor, `${path}[${i}]`));
    return result;
  }
  if (typeof node === 'object') {
    const color = node.color || inheritedColor;
    if (typeof node.text === 'string' && node.text.trim()) {
      result.push({ text: node.text.trim(), color, path });
    }
    if (node?.toString?.name === 'ChatMessage') {
      flattenAllText(node.json, result, color, `${path}.json`);
    }
    if (node.json && typeof node.json === 'object') {
      flattenAllText(node.json, result, color, `${path}.json`);
    }
    if (Array.isArray(node.extra)) {
      flattenAllText(node.extra, result, color, `${path}.extra`);
    }
    if (node.hoverEvent?.contents) {
      flattenAllText(node.hoverEvent.contents, result, color, `${path}.hoverEvent.contents`);
    }
    Object.entries(node).forEach(([key, value]) => {
      if (typeof value === 'object' || Array.isArray(value)) {
        flattenAllText(value, result, color, `${path}.${key}`);
      }
    });
    return result;
  }
  return result;
}

function extractSender(jsonMsg) {
  const flat = flattenAllText(jsonMsg);
  const idx = flat.findIndex(f => f.text?.toLowerCase().includes('sender:'));
  if (idx !== -1) {
    const context = flat.slice(Math.max(0, idx - 2), idx + 8);
    logDebug("SenderContext", context);
    const candidates = context.map(f => f.text?.trim().toLowerCase()).filter(Boolean);
    logDebug("SenderScan", { from: idx, candidates });
    const match = candidates.find(c => config.testers.some(t => t.toLowerCase() === c));
    if (match) {
      logDebug("SenderFound", match);
      return config.testers.find(t => t.toLowerCase() === match);
    }
    // Fallback to immediate next fragment
    if (flat[idx + 1] && flat[idx + 1].text?.trim()) {
      const raw = flat[idx + 1].text.trim();
      logDebug("SenderFallback", raw);
      return raw;
    }
  }
  return null;
}

function extractMessage(jsonMsg) {
  const flat = flattenAllText(jsonMsg);
  const lightPurples = flat.filter(f => f.color === 'light_purple' && f.text?.trim());
  if (lightPurples.length > 0) {
    const msg = lightPurples[lightPurples.length - 1].text.trim();
    logDebug("LightPurpleMsg", msg);
    return msg;
  }
  const fallback = flat.find(f => f.text?.trim() && f.text.length > 10);
  if (fallback) {
    logDebug("MsgFallback", fallback.text);
    return fallback.text.trim();
  }
  return null;
}

function extractDeepWhisper(jsonMsg) {
  try {
    const sender = extractSender(jsonMsg);
    const message = extractMessage(jsonMsg);
    logDebug("WhisperParts", { sender, message });
    return { sender: sender || 'Unknown', message: message || null };
  } catch (err) {
    logError("WhisperParse", err.message);
    return null;
  }
}

function setupDirectMessageListener(bot) {
  bot.on('message', (jsonMsg) => {
    try {
      const parsed = extractDeepWhisper(jsonMsg);
      if (parsed && parsed.message) {
        logInfo("DM", `From: ${parsed.sender} | Message: ${parsed.message}`);
        logDebug("DM.full", jsonMsg);
      } else {
        logDebug("DM.skip", parsed);
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
