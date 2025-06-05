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

public class PrivateMessageListener implements Listener {
    private final JavaPlugin plugin;
    private final ConfigManager cfg;
    private final FileLogger fileLogger; // NEW field

    /**
     * UPDATED constructor. Now accepts FileLogger instance.
     */
    public PrivateMessageListener(JavaPlugin plugin, ConfigManager cfg, FileLogger fileLogger) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.fileLogger = fileLogger;
    }

    @EventHandler
    public void onPrivateMessage(AsyncChatEvent event) {
        if (event.getChatType() != ChatType.PRIVATE_MESSAGE) return;

        String recipient = event.getRecipient().getName();
        if (!recipient.equalsIgnoreCase("ZaraSprite")) return;

        String senderName = event.getSender().getName();
        String rawMessage = event.message().toPlainText();

        String jsonString = String.format(
            "{\"username\":\"%s\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
            senderName,
            rawMessage.replace("\"", "\\\""),
            Instant.now().toString()
        );

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            HttpPostTask.postJson(jsonString, cfg);
            fileLogger.logToUserFile(senderName, jsonString, "SENT");
        });
    }
}
