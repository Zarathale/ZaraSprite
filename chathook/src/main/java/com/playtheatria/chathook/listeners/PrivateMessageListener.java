package com.playtheatria.chathook.listeners;

import com.playtheatria.chathook.utils.ConfigManager;
import com.playtheatria.chathook.utils.HttpPostTask;
import com.playtheatria.chathook.utils.FileLogger;

import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.event.player.AsyncChatEvent.ChatType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.UUID;

public class PrivateMessageListener implements Listener {
    private final JavaPlugin plugin;
    private final ConfigManager cfg;

    public PrivateMessageListener(JavaPlugin plugin, ConfigManager cfg) {
        this.plugin = plugin;
        this.cfg    = cfg;
    }

    @EventHandler
    public void onPlayerPrivateMessage(AsyncChatEvent event) {
        // 1) Only handle true DMs
        if (event.chatType() != ChatType.PRIVATE_MESSAGE) return;
        if (!event.recipient().getName().equalsIgnoreCase(cfg.getBotName())) return;

        // 2) Extract sender + message
        String sender  = event.player().getName();
        Component comp = event.message();
        String message = PlainTextComponentSerializer.plainText().serialize(comp);

        // 3) Build your JSON payload
        String timestamp = Instant.now().toString();
        String id        = UUID.randomUUID().toString();
        String json = String.format(
            "{\"id\":\"%s\",\"player\":\"%s\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
            id, sender, escapeJson(message), timestamp
        );

        // 4) Local log & HTTP POST
        FileLogger.logInfo(sender, message);
        HttpPostTask.postJson(plugin, json, cfg);
    }

    private String escapeJson(String raw) {
        return raw.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
