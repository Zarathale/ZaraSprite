const mineflayer = require('mineflayer');
// const { ChatMessage } = require('minecraft-chat-message'); // removed

// Add readline for Manual Quit
const readline = require('readline');
const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
});

rl.on('SIGINT', () => {
  console.log('🛑 Shutting down ZaraSprite...');
  process.exit();
});

// Text Extractor
function extractText(obj) {
  if (typeof obj === 'string') return obj;
  if (!obj) return '';
  let out = '';
  if (typeof obj.text === 'string') out += obj.text;
  if (Array.isArray(obj.extra)) out += obj.extra.map(extractText).join('');
  return out;
}

// The Bot
function createBot() {
  const bot = mineflayer.createBot({
    host: 'mc.playtheatria.com',
    port: 25565,
    username: 'ZaraSprite@outlook.com', // Microsoft account email
    version: '1.21.4', // Match the Theatria server version
    auth: 'microsoft' // Using Microsoft authentication
  });

  bot.on('login', () => {
    console.log('✅ ZaraSprite logged in.');

    // Send /tell zarathale every 60 seconds
    setInterval(() => {
      bot.chat('/tell zarathale heyo');
    }, 60000);
  });

  bot.on('end', () => {
    console.log('⚠️ Disconnected. Reconnecting in 5s...');
    setTimeout(createBot, 5000);
  });

  bot.on('error', (err) => {
    console.error('❌ Error:', err);
  });

 bot.on('whisper', (username, message, translate, jsonMsg, matches) => {
  console.log(`📩 PM from ${username}: ${message}`);
});







}

createBot();
