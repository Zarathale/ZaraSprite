package com.playtheatria.chathook.listeners;

import com.playtheatria.chathook.utils.ConfigManager;
import com.playtheatria.chathook.utils.FileLogger;
import com.playtheatria.chathook.utils.HttpPostTask;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.chat.ChatType;
import org.bukkit.event.player.AsyncChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.UUID;

public class PrivateMessageListener implements Listener {
    private final JavaPlugin plugin;
    private final ConfigManager cfg;
    private final FileLogger fileLogger;

    /**
     * Constructor now accepts FileLogger instance.
     */
    public PrivateMessageListener(JavaPlugin plugin, ConfigManager cfg, FileLogger fileLogger) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.fileLogger = fileLogger;
    }

    @EventHandler
    public void onPrivateMessage(AsyncChatEvent event) {
        // Only handle PRIVATE_MESSAGE packets
        if (event.getMessageType() != ChatType.PRIVATE_MESSAGE) {
            return;
        }

        // Check recipient against configured bot name (case-insensitive)
        String botName = cfg.getBotName();
        if (!event.getRecipient().getName().equalsIgnoreCase(botName)) {
            return;
        }

        String senderName = event.getSender().getName();
        String rawMessage = event.message().toPlainText();

        // Build a UUID for this message
        String uuid = UUID.randomUUID().toString();
        String timestamp = Instant.now().toString();

        // Escape any quotes in the message
        String escaped = rawMessage.replace("\"", "\\\"");

        // Construct JSON payload with username, message, timestamp, and uuid
        String jsonString = String.format(
            "{\"username\":\"%s\",\"message\":\"%s\",\"timestamp\":\"%s\",\"uuid\":\"%s\"}",
            senderName,
            escaped,
            timestamp,
            uuid
        );

        // Schedule the HTTP POST + logging off the main thread
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            // Send the JSON to your Flask endpoint
            HttpPostTask.postJson(jsonString, cfg);
            // Also append to a per-user log file
            fileLogger.logToUserFile(senderName, jsonString, "SENT");
        });
    }
}
