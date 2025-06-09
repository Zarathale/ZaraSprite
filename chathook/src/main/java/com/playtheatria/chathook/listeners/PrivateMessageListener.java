package com.playtheatria.chathook.listeners;

import com.playtheatria.chathook.utils.ConfigManager;
import com.playtheatria.chathook.utils.HttpPostTask;
import com.playtheatria.chathook.utils.FileLogger;

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
 * Listens for true private-message events (DMs) to our bot and forwards them.
 */
public class PrivateMessageListener implements Listener {
    private final JavaPlugin plugin;
    private final ConfigManager cfg;

    public PrivateMessageListener(JavaPlugin plugin, ConfigManager cfg) {
        this.plugin = plugin;
        this.cfg    = cfg;
    }

    @EventHandler
    public void onAsyncChat(AsyncChatEvent event) {
        // Only handle genuine DMs
        if (event.getMessageType() != ChatType.PRIVATE_MESSAGE) return;
        // Ensure recipient is our bot
        boolean toBot = event.getRecipients().stream()
                          .anyMatch(p -> p.getName().equalsIgnoreCase(cfg.getBotName()));
        if (!toBot) return;

        Player sender = event.getPlayer();
        Component comp = event.message();
        String message = PlainTextComponentSerializer.plainText().serialize(comp);

        String timestamp = Instant.now().toString();
        String id        = UUID.randomUUID().toString();
        String json = String.format(
            "{\"id\":\"%s\",\"player\":\"%s\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
            id, sender.getName(), escapeJson(message), timestamp
        );

        // Static logger
        FileLogger.logInfo(sender.getName(), message);
        // Forward the DM
        HttpPostTask.postJson(plugin, json, cfg);
    }

    private String escapeJson(String raw) {
        return raw.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
