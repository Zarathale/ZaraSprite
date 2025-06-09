package com.playtheatria.chathook.listeners;

import com.playtheatria.chathook.utils.ConfigManager;
import com.playtheatria.chathook.utils.FileLogger;
import com.playtheatria.chathook.utils.HttpPostTask;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.event.player.AsyncChatEvent.ChatType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.UUID;

public class PrivateMessageListener implements Listener {
    private final JavaPlugin plugin;
    private final ConfigManager cfg;
    private final FileLogger fileLogger;

    public PrivateMessageListener(JavaPlugin plugin, ConfigManager cfg, FileLogger fileLogger) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.fileLogger = fileLogger;
    }

    @EventHandler
    public void onAsyncChat(AsyncChatEvent event) {
        // only forward true private messages
        if (event.chatType() != ChatType.PRIVATE_MESSAGE) return;

        String sender    = event.getPlayer().getName();
        String message   = event.message().toPlainText();
        String timestamp = Instant.now().toString();
        String id        = UUID.randomUUID().toString();

        String json = String.format(
            "{\"id\":\"%s\",\"player\":\"%s\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
            id, sender, escapeJson(message), timestamp
        );

        // local log
        fileLogger.logInfo(sender, message);

        // async HTTP POST with retries
        HttpPostTask.postJson(plugin, json, cfg);
    }

    private String escapeJson(String raw) {
        return raw.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
