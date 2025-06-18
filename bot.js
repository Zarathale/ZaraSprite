// == ZaraSprite: bot.js ==
// DM parsing using PM signature pattern + fallback logging

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

// --- Listeners ---
function setupDirectMessageListener(bot) {
  bot.on('message', (jsonMsg) => {
    try {
      const base = jsonMsg.json;
      const plain = jsonMsg.toString();

      if (!plain.includes('[PM] [')) return;

      logInfo("DM-Raw", plain);

      const whisper = /^\[PM\] \[([^\]]+?) -> ([^\]]+?)\] (.+)$/;
      const match = plain.match(whisper);
      if (match) {
        const [, sender, receiver, bodyRaw] = match;
        const body = bodyRaw.replace(/\sflp[ms]_[0-9a-f\-]+\s*/g, '').trim();
        logInfo("DM-Match", `From: ${sender} → ${receiver} | ${body}`);
      } else {
        logInfo("DM-Skip", "Message matched PM filter but regex failed.");
      }
    } catch (err) {
      logError("DM-Parse", err.message);
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
