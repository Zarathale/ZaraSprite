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

    public PrivateMessageListener(JavaPlugin plugin, ConfigManager cfg, FileLogger fileLogger) {
        this.plugin     = plugin;
        this.cfg        = cfg;
        this.fileLogger = fileLogger;
    }

    @EventHandler
    public void onPlayerPrivateMessage(AsyncChatEvent event) {
        if (event.getMessageType() != ChatType.PRIVATE_MESSAGE) return;
        if (!event.getRecipient().getName().equalsIgnoreCase(cfg.getBotName())) return;

        String sender  = event.getSender().getName();
        String message = event.message().toPlainText();
        String timestamp = Instant.now().toString();
        String id      = UUID.randomUUID().toString();

        String json = String.format(
            "{\"id\":\"%s\",\"player\":\"%s\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
            id, sender, escapeJson(message), timestamp
        );

        // Log locally
        fileLogger.logInfo(sender, message);

        // Fire off the HTTP POST (self-scheduling/retrying)
        HttpPostTask.postJson(plugin, json, cfg);
    }

    private String escapeJson(String raw) {
        return raw.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
