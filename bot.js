// == ZaraSprite: bot.js ==
// Final tested method using .json.extra directly and PM block traversal

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

// --- Message Parsing ---
function extractPM(jsonMsg) {
  try {
    const extra = jsonMsg?.json?.extra;
    if (!Array.isArray(extra)) return null;

    // Must start with [PM
    if (!(extra[0]?.text === '[' && extra[1]?.text === 'PM')) return null;

    const arrowIdx = extra.findIndex(e => e.text === '->');
    if (arrowIdx < 2 || !extra[arrowIdx - 1] || !extra[arrowIdx + 1]) return null;

    const sender = extra[arrowIdx - 1].text.trim();
    const receiver = extra[arrowIdx + 1].text.trim();

    // Message body starts after the first closing bracket
    const closeIdx = extra.findIndex((e, i) => i > arrowIdx && e.text === ']');
    const messageParts = extra.slice(closeIdx + 1).map(e => e.text).join(' ').replace(/\sflp[ms]_[0-9a-f-]+\s*/g, '').trim();

    return { sender, receiver, body: messageParts };
  } catch (err) {
    logError("PMExtract", err.message);
    return null;
  }
}

// --- Connect ---
function connectToServer(cfg) {
  try {
    logInfo("Startup", `Connecting as ${cfg.username} to ${cfg.host}:${cfg.port}...`);
    const bot = mineflayer.createBot(cfg);

    bot.once('login', () => logInfo("Connection", `Successfully logged in as ${bot.username}`));
    bot.on('end', () => logInfo("Connection", `Bot has disconnected.`));
    bot.on('error', (err) => logError("Connection", err.message));

    bot.on('message', (jsonMsg) => {
      const parsed = extractPM(jsonMsg);
      if (parsed) {
        logInfo("DM", `From: ${parsed.sender} → ${parsed.receiver} | ${parsed.body}`);
      } else if (config.DEBUG_MODE) {
        logDebug("UnparsedMsg", jsonMsg);
      }
    });

    return bot;
  } catch (err) {
    logError("Connection", err.message);
    return null;
  }
}

// --- Entrypoint ---
function main() {
  const bot = connectToServer(config);
  if (!bot) {
    logError("Main", "Bot creation failed.");
    return;
  }
}

main();
