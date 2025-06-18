// == ZaraSprite: bot.js ==
// Proof-of-concept with logging, command parsing, and config modularization

const mineflayer = require('mineflayer');

// --- Config (Inline for now) ---
const config = {
  host: 'mc.playtheatria.com',
  port: 25565,
  username: 'ZaraSprite',
  auth: 'microsoft', // Use 'microsoft' for Microsoft accounts
  version: '1.20.4',
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

// --- Message Probes ---
function setupMessageProbes(bot) {
  // Basic Chat
  bot.on('chat', (username, message) => {
    logInfo("chat", `From: ${username} | ${message}`);
  });

  // Whisper DM
  bot.on('whisper', (username, message, rawMessage) => {
    logInfo("whisper", `From: ${username} | ${message}`);
    logDebug("whisper.raw", rawMessage);
  });

  // General Message JSON (may include system/DM)
  bot.on('message', (jsonMsg, position, sender) => {
    logInfo("message", `Pos: ${position} | From: ${sender?.username || 'N/A'}`);
    logDebug("message.raw", jsonMsg);
  });

  // System Chat
  bot._client.on('system_chat', (packet) => {
    logInfo("system_chat", `Packet type: system_chat`);
    logDebug("system_chat.packet", packet);
  });

  // Generic packet hook (for deep digging)
  bot._client.on('packet', (data, meta) => {
    if (meta.name === 'chat' || meta.name === 'system_chat' || meta.name === 'player_chat') {
      logInfo("packet", `Incoming packet: ${meta.name}`);
      logDebug("packet.data", data);
    }
  });

  // Raw incoming text as fallback
  bot.on('messagestr', (message) => {
    logInfo("messagestr", `Raw text: ${message}`);
  });
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
  bot.on('message', (jsonMsg) => {
    try {
      const text = jsonMsg?.extra?.map(e => e.text).join('') || '';
      if (text.includes('whispers to you:')) {
        logInfo("DM", `Parsed whisper from raw message: ${text}`);
        logDebug("DM.full", jsonMsg);
      }
    } catch (err) {
      logError("DM", `Error parsing message: ${err}`);
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

  // Setup message probes
  setupMessageProbes(bot);

  // Functional liseteners
  setupDirectMessageListener(bot);
  setupCommandListener(bot);
}

main();
