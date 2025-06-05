package com.playtheatria.chathook.listeners;

import com.google.gson.JsonObject;
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
    private final ConfigManager configManager;

    public PrivateMessageListener(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @EventHandler
    public void onPrivateMessage(AsyncChatEvent event) {
        if (event.getChatType() != ChatType.PRIVATE_MESSAGE) return;

        String recipient = event.getRecipient().getName();
        if (!recipient.equalsIgnoreCase("ZaraSprite")) return;

        String sender = event.getSender().getName();
        String message = event.message().toPlainText();

        JsonObject payload = new JsonObject();
        payload.addProperty("username", sender);
        payload.addProperty("message", message);
        payload.addProperty("timestamp", Instant.now().toString());

        String json = payload.toString();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            HttpPostTask.postJson(json, configManager);
            FileLogger.logToUserFile(sender, json, "SENT");
        });
    }
}
