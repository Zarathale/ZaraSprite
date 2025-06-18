// == ZaraSprite: bot.js ==
// Robust DM parsing for Theatria messages with deep nested structures

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

// --- Deep Chat Traversal ---
function flatten(msgNode, depth = 0, arr = []) {
  if (!msgNode || typeof msgNode !== 'object') return arr;
  if (typeof msgNode.text === 'string' && msgNode.text.trim()) {
    arr.push({ text: msgNode.text.trim(), color: msgNode.color, depth });
  }
  if (msgNode[''] && typeof msgNode[''] === 'string') {
    arr.push({ text: msgNode[''].trim(), color: msgNode.color, depth });
  }
  if (Array.isArray(msgNode.extra)) {
    msgNode.extra.forEach(e => flatten(e, depth + 1, arr));
  }
  if (msgNode.json && typeof msgNode.json === 'object') {
    flatten(msgNode.json, depth + 1, arr);
  }
  return arr;
}

function parsePrivateMessage(jsonMsg) {
  try {
    const flat = flatten(jsonMsg);
    const start = flat.findIndex(e => e.text === 'PM');
    if (start === -1) return null;

    const arrowIdx = flat.findIndex(e => e.text === '->');
    if (arrowIdx < 2) return null;

    const sender = flat[arrowIdx - 1]?.text?.trim() || 'Unknown';
    const receiver = flat[arrowIdx + 1]?.text?.trim() || 'ZaraSprite';

    const messageParts = flat.slice(arrowIdx + 2)
      .map(e => e.text)
      .filter(Boolean)
      .join(' ')
      .replace(/\sflp[ms]_[0-9a-f\-]+\s*/g, '')
      .trim();

    if (!messageParts) return null;

    return { sender, receiver, body: messageParts };
  } catch (err) {
    logError("Parse", err.message);
    return null;
  }
}

// --- Listeners ---
function setupMessageListener(bot) {
  bot.on('message', (jsonMsg) => {
    const parsed = parsePrivateMessage(jsonMsg);
    if (parsed) {
      logInfo("DM", `From: ${parsed.sender} → ${parsed.receiver} | ${parsed.body}`);
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
