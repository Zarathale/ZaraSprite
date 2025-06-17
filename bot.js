// == ZaraSprite: bot.js ==
// Proof-of-concept startup and message handler

const mineflayer = require('mineflayer');

// --- Config (Read from env or hardcoded during early dev) ---
const config = {
  host: 'play.theatria.net', // DO NOT HARDCODE in prod
  port: 25565,
  username: 'ZaraSprite',
  version: false // auto-detect
};

// --- Utility Logger ---
function logInfo(label, message) {
  console.log(`[${new Date().toISOString()}] [INFO] [${label}] ${message}`);
}

function logError(label, error) {
  console.error(`[${new Date().toISOString()}] [ERROR] [${label}]`, error);
}

// --- Connection Logic ---
function connectToServer(config) {
  try {
    logInfo("Startup", `Connecting as ${config.username} to ${config.host}:${config.port}...`);
    const bot = mineflayer.createBot(config);

    bot.once('login', () => {
      logInfo("Connection", `Successfully logged in as ${bot.username}`);
    });

    bot.on('error', (err) => {
      logError("Connection", `Error: ${err.message}`);
    });

    bot.on('end', () => {
      logInfo("Connection", `Bot has disconnected.`);
    });

    return bot;
  } catch (err) {
    logError("Connection", err);
  }
}

// --- DM Listener Logic ---
function setupDirectMessageListener(bot) {
  bot.on('message', (jsonMsg, position, sender) => {
    try {
      const raw = jsonMsg;
      const isWhisper = (position === 2); // Position 2 likely = whisper
      const senderName = sender ? sender.username : 'Unknown';

      if (isWhisper) {
        logInfo("DM", `From: ${senderName} | Type: ${position}`);
        logInfo("DM", `Raw Message Object:\n${JSON.stringify(raw, null, 2)}`);
      }
    } catch (err) {
      logError("DM Listener", err);
    }
  });
}

// --- Command Listener Logic ---
function setupCommandListener(bot) {
  const COMMAND_PREFIX_REGEX = /^ZaraSprite\s+\/(.+)/;

  bot.on('chat', (username, message) => {
    try {
      if (username !== 'Zarathale') return;

      const match = message.match(COMMAND_PREFIX_REGEX);
      if (match) {
        const fullCommand = match[1];
        logInfo("CommandParser", `Received command from Zarathale: /${fullCommand}`);
        bot.chat(`/${fullCommand}`);
        logInfo("CommandExec", `Executed: /${fullCommand}`);
      }
    } catch (err) {
      logError("CommandExec", err);
    }
  });
}

// --- Main Entrypoint ---
function main() {
  const bot = connectToServer(config);
  if (!bot) {
    logError("Main", "Bot creation failed.");
    return;
  }

  setupDirectMessageListener(bot);
  setupCommandListener(bot);
}

main();
