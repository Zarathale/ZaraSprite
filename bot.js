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

  bot.on('message', (jsonMsg) => {
    const msg = extractText(jsonMsg).trim();
    let sender = null;
   
    console.dir(jsonMsg, { depth: null });

    try {
      const hover = jsonMsg.hoverEvent?.contents?.extra;
      const senderLine = hover?.find(
        line => extractText(line).includes('Sender: ')
      );
      const senderText = extractText(senderLine);
      sender = senderText.replace('Sender: ', '').trim();
    } catch (e) {
      // Silent fallback
    }

    if (sender) {
      console.log(`📩 PM from ${sender}: ${msg}`);
    } else if (msg) {
      console.log('💬 Chat:', msg);
    } else {
      console.log('📭 Chat: (no visible text)');
    }
  });






}

createBot();
