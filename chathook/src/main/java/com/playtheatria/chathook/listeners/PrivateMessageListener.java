package com.playtheatria.chathook.listeners;

import com.playtheatria.chathook.utils.ConfigManager;
import com.playtheatria.chathook.utils.FileLogger;
import com.playtheatria.chathook.utils.HttpPostTask;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncChatEvent;
import org.bukkit.event.player.AsyncChatEvent.ChatType;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.UUID;

/**
 * Listens for private‐message (DM) events to our bot and forwards them.
 */
public class PrivateMessageListener implements Listener {
    private final JavaPlugin plugin;
    private final ConfigManager cfg;
    private final FileLogger fileLogger;

    public PrivateMessageListener(JavaPlugin plugin,
                                  ConfigManager cfg,
                                  FileLogger fileLogger) {
        this.plugin     = plugin;
        this.cfg        = cfg;
        this.fileLogger = fileLogger;
    }

    @EventHandler
    public void onPlayerPrivateMessage(AsyncChatEvent event) {
        // 1) Only process true DMs
        if (event.getMessageType() != ChatType.PRIVATE_MESSAGE) return;
        // 2) Make sure the DM is addressed to our bot
        boolean toBot = event.getRecipients().stream()
            .anyMatch(r -> r.getName().equalsIgnoreCase(cfg.getBotName()));
        if (!toBot) return;

        // 3) Extract sender + plain‐text message
        Player sender = event.getPlayer();
        Component comp = event.message();
        String message = PlainTextComponentSerializer.plainText().serialize(comp);

        // 4) Build JSON payload
        String id        = UUID.randomUUID().toString();
        String timestamp = Instant.now().toString();
        String json = String.format(
            "{\"id\":\"%s\",\"player\":\"%s\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
            id, sender.getName(), escapeJson(message), timestamp
        );

        // 5) Log to user‐file and forward via HTTP
        fileLogger.logToUserFile(sender.getName(), json, "SENT");  // uses singleton instance :contentReference[oaicite:3]{index=3}
        HttpPostTask.postJson(plugin, json, cfg);
    }

    private String escapeJson(String raw) {
        return raw.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
