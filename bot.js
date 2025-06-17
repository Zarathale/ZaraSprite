// == ZaraSprite: bot.js ==
// Proof-of-concept with logging, command parsing, and config modularization

const mineflayer = require('mineflayer');

// --- Config (Inline for now) ---
const config = {
  host: 'play.theatria.net',
  port: 25565,
  username: 'ZaraSprite',
  version: false,
  DEBUG_MODE: true,
  testers: ['Zarathale']
};

// --- Utility Logger ---
function logInfo(label, message) {
  console.log(`[${new Date().toISOString()}] [INFO] [${label}] ${message}`);
}

function logError(label, error) {
  console.error(`[${new Date().toISOString()}] [ERROR] [${label}]`, error);
}

function logDebug(label, data) {
  if (config.DEBUG_MODE) {
    console.log(`[${new Date().toISOString()}] [DEBUG] [${label}]`);
    console.dir(data, { depth: null });
  }
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
      const isWhisper = (position === 2);
      const senderName = sender ? sender.username : 'Unknown';

      if (isWhisper) {
        logInfo("DM", `From: ${senderName} | Type: ${position}`);
        logInfo("DM", `Raw Message Object:\n${JSON.stringify(raw, null, 2)}`);
        logDebug("DM", { jsonMsg, position, sender });
      }
    } catch (err) {
      logError("DM Listener", err);
    }
  });
}

// --- Safe Command Parser ---
function isValidCommand(cmd) {
  // Block semicolons, pipes, newlines, and escape characters
  return !/[;\n\r|\\]/.test(cmd);
}

// --- Command Listener Logic ---
function setupCommandListener(bot) {
  const COMMAND_PREFIX_REGEX = /^ZaraSprite\s+\/(.+)/;

  bot.on('chat', (username, message) => {
    try {
      if (!config.testers.includes(username)) return;

      const match = message.match(COMMAND_PREFIX_REGEX);
      if (match) {
        const fullCommand = match[1];

        if (!isValidCommand(fullCommand)) {
          logError("CommandParser", `Blocked potentially unsafe command: ${fullCommand}`);
          return;
        }

        logInfo("CommandParser", `Received command from ${username}: /${fullCommand}`);
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
