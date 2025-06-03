package com.example.chathook;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class ChatListener implements Listener {
    private final JavaPlugin plugin;
    // Replace with your actual Flask URL:
    private static final String FLASK_URL = "http://your-flask-app.example.com/receive";

    public ChatListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String sender = event.getPlayer().getName();

        // Extract plain text from the Component
        String message = PlainTextComponentSerializer.plainText()
                            .serialize(event.message());

        // Build a simple JSON payload
        String json = String.format(
            "{\"sender\":\"%s\",\"message\":\"%s\",\"type\":\"chat\"}",
            escapeJson(sender),
            escapeJson(message)
        );

        // Fire off the HTTP request asynchronously
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(FLASK_URL))
            .header("Content-Type", "application/json")
            .POST(BodyPublishers.ofString(json, StandardCharsets.UTF_8))
            .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
              .whenComplete((resp, err) -> {
                  if (err != null) {
                      plugin.getLogger().warning("Failed to POST chat: " + err.getMessage());
                  }
              });
    }

    // very minimal JSON-escaping for quotes/backslashes:
    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
