package com.playtheatria.chathook.listeners;

import com.playtheatria.chathook.utils.ConfigManager;
import com.playtheatria.chathook.utils.HttpPostTask;
import com.playtheatria.chathook.utils.FileLogger;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.chat.AsyncChatEvent;
import org.bukkit.event.chat.AsyncChatEvent.ChatType;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.UUID;

/**
 * Listens for private‐message (DM) events to our bot and forwards them.
 */
public class PrivateMessageListener implements Listener {
    private final JavaPlugin plugin;
    private final ConfigManager cfg;

    public PrivateMessageListener(JavaPlugin plugin, ConfigManager cfg) {
        this.plugin = plugin;
        this.cfg    = cfg;
    }

    @EventHandler
    public void onPlayerPrivateMessage(AsyncChatEvent event) {
        // 1) Only process real DMs
        if (event.getMessageType() != ChatType.PRIVATE_MESSAGE) return;
        // 2) Make sure the DM is to our bot
        boolean toBot = event.getRecipients().stream()
            .anyMatch(r -> r.getName().equalsIgnoreCase(cfg.getBotName()));
        if (!toBot) return;

        // 3) Extract sender + text
        Player sender = event.getPlayer();
        Component comp = event.message();
        String message = PlainTextComponentSerializer.plainText().serialize(comp);

        // 4) Build JSON
        String timestamp = Instant.now().toString();
        String id        = UUID.randomUUID().toString();
        String json = String.format(
            "{\"id\":\"%s\",\"player\":\"%s\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
            id, sender.getName(), escapeJson(message), timestamp
        );

        // 5) Log and forward
        FileLogger.logInfo(sender.getName(), message);
        HttpPostTask.postJson(plugin, json, cfg);
    }

    private String escapeJson(String raw) {
        return raw.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
