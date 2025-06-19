// == ZaraSprite: bot.js ==
// Clean DM parsing using message event and PM pattern matching

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

// --- DM Parsing ---
function parsePMFromJSON(jsonMsg) {
  const raw = jsonMsg?.toString?.();
  const pmRegex = /^\[PM\] \[([^\]]+?) -> ([^\]]+?)\] (.+)$/;
  const match = raw?.match(pmRegex);
  if (match) {
    const [, sender, receiver, message] = match;
    const cleanMessage = message.replace(/\sflp[ms]_[0-9a-f-]+\s*/g, '').trim();
    return { sender, receiver, message: cleanMessage };
  }
  return null;
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
      const parsed = parsePMFromJSON(jsonMsg);
      if (parsed) {
        logInfo("DM", `From: ${parsed.sender} → ${parsed.receiver} | ${parsed.message}`);
      } else if (config.DEBUG_MODE) {
        logDebug("ChatMessage", jsonMsg);
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
