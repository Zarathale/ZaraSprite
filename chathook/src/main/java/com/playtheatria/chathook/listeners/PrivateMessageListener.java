package com.playtheatria.chathook.listeners;

import com.playtheatria.chathook.utils.ConfigManager;
import com.playtheatria.chathook.utils.HttpPostTask;
import com.playtheatria.chathook.utils.FileLogger;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.event.player.AsyncChatEvent.ChatType;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.UUID;

/**
 * Listens for private messages sent to the bot and forwards them.
 */
public class PrivateMessageListener implements Listener {
    private final JavaPlugin plugin;
    private final ConfigManager cfg;

    public PrivateMessageListener(JavaPlugin plugin, ConfigManager cfg) {
        this.plugin = plugin;
        this.cfg = cfg;
    }

    @EventHandler
    public void onAsyncChat(AsyncChatEvent event) {
        // Only handle genuine private messages to the bot
        if (event.chatType() != ChatType.PRIVATE_MESSAGE) return;
        if (!event.recipient().getName().equalsIgnoreCase(cfg.getBotName())) return;

        String sender = event.player().getName();
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        String timestamp = Instant.now().toString();
        String id = UUID.randomUUID().toString();

        String json = String.format(
            "{\"id\":\"%s\",\"player\":\"%s\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
            id, sender, escapeJson(message), timestamp
        );

        // Log locally and forward to the HTTP endpoint
        FileLogger.logInfo(sender, message);
        HttpPostTask.postJson(plugin, json, cfg);
    }

    private String escapeJson(String raw) {
        return raw.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
