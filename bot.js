// == ZaraSprite: bot.js ==
// DM parsing using message event with pattern matching

const mineflayer = require('mineflayer');

// --- Config ---
const config = {
  host: 'mc.playtheatria.com',
  port: 25565,
  username: 'ZaraSprite',
  auth: 'microsoft',
  version: '1.20.4',
  DEBUG_MODE: true
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

// --- Message Flattening ---
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

function setupMessageListener(bot) {
  bot.on('message', (jsonMsg) => {
    try {
      const flat = flattenAllText(jsonMsg);
      if (flat[0]?.text !== '[' || flat[1]?.text !== 'PM') return;

      const arrowIdx = flat.findIndex(f => f.text === ' -> ');
      if (arrowIdx < 2) return;

      const sender = flat[arrowIdx - 1]?.text;
      const receiver = flat[arrowIdx + 1]?.text;
      const messageParts = flat.slice(arrowIdx + 2);

      let body = messageParts.map(p => p.text).join(' ').trim();
      body = body.replace(/\sflp[ms]_[0-9a-f\-]+\s*/g, '');

      if (sender && receiver && body) {
        logInfo("DM", `From: ${sender} → ${receiver} | ${body}`);
      }
    } catch (err) {
      logError("MessageParse", err.message);
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
  setupMessageListener(bot);
}

main();
