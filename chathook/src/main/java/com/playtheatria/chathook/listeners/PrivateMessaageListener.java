package com.playtheatria.chathook;

import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.chat.AsyncChatEvent;
import org.bukkit.event.chat.ChatType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Listens for AsyncChatEvent with ChatType.PRIVATE_MESSAGE.
 * When a DM to "ZaraSprite" is detected, extracts sender and message,
 * builds JSON payload, logs to file, and dispatches an HTTP POST asynchronously.
 */
public class PMListener implements Listener {

    private final ChatHookPlugin plugin;

    public PMListener(ChatHookPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPrivateMessage(AsyncChatEvent event) {
        // Only intercept private messages
        if (event.getChatType() != ChatType.PRIVATE_MESSAGE) {
            return;
        }

        // Check if recipient is exactly "ZaraSprite" (case-insensitive)
        if (!event.getRecipient().getName().equalsIgnoreCase("ZaraSprite")) {
            return;
        }

        String senderName = event.getSender().getName();
        String messageText = event.message().toPlainText();
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        String uuid = UUID.randomUUID().toString();

        // Build JSON payload using Gson
        JsonObject payload = new JsonObject();
        payload.addProperty("username", senderName);
        payload.addProperty("message", messageText);
        payload.addProperty("timestamp", timestamp);
        payload.addProperty("uuid", uuid);

        String jsonString = payload.toString();

        // Log to <sender>.log (append)
        logToUserFile(senderName, jsonString, "PENDING");

        // Dispatch HTTP POST in an asynchronous task
        BukkitRunnable httpTask = new HttpPostTask(plugin, senderName, jsonString);
        var bukkitTask = httpTask.runTaskAsynchronously(plugin);
        plugin.trackTask(bukkitTask);
    }

    /**
     * Appends a line to <sender>.log under plugins/chathook/logs/
     * Format: [ISO_TIMESTAMP] JSON_PAYLOAD → STATUS
     */
    private void logToUserFile(String senderName, String jsonString, String status) {
        File userLogFile = new File(plugin.getLogsFolder(), senderName + ".log");
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        String line = String.format("[%s] %s → %s%n", timestamp, jsonString, status);

        try (FileWriter writer = new FileWriter(userLogFile, true)) {
            writer.write(line);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING,
                    "Could not write to log file for user " + senderName, e);
        }
    }
}
