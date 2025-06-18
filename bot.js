// == ZaraSprite: bot.js ==
// DM parsing using explicit JSON path traversal for Theatria-style private messages

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

function setupMessageListener(bot) {
  bot.on('message', (msg) => {
    try {
      const base = msg?.json?.extra?.[0]?.extra;
      if (!Array.isArray(base)) return;

      // Must start with [PM
      const prefix = base.slice(0, 2).map(x => x.extra?.[0] || x[''] || '').join('').trim();
      if (!prefix.startsWith('[PM')) return;

      // Locate ' -> ' and navigate to sender/receiver
      let arrowIdx = base.findIndex(x => x.extra?.[0] === '-> ' || x[''] === '-> ');
      if (arrowIdx < 2) return;

      const sender = base[arrowIdx - 1]?.extra?.[0] || base[arrowIdx - 1]?.[''] || 'Unknown';
      const receiver = base[arrowIdx + 1]?.extra?.[0] || base[arrowIdx + 1]?.[''] || 'ZaraSprite';

      // Traverse forward to get the actual message
      const trail = base.slice(arrowIdx + 2);
      const messagePart = trail.flatMap(e => {
        if (Array.isArray(e.extra)) return e.extra;
        return [e];
      }).flatMap(e => typeof e === 'string' ? [e] : (e.text ? [e.text] : []));

      let body = messagePart.join(' ').replace(/\sflp[ms]_[0-9a-f\-]+\s*/g, '').trim();
      if (!body) return;

      logInfo("DM", `From: ${sender} → ${receiver} | ${body}`);
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
