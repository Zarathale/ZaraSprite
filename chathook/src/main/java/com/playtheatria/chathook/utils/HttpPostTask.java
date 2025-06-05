package com.playtheatria.chathook;

import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;

/**
 * Builds and sends the HTTP POST request asynchronously.
 * Retries up to retryLimit if failures occur.
 * Logs success or error to both the per-user log and error.log if needed.
 */
public class HttpPostTask extends BukkitRunnable {

    private final ChatHookPlugin plugin;
    private final String senderName;
    private final String jsonPayload;

    public HttpPostTask(ChatHookPlugin plugin, String senderName, String jsonPayload) {
        this.plugin = plugin;
        this.senderName = senderName;
        this.jsonPayload = jsonPayload;
    }

    @Override
    public void run() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofMillis(plugin.getTimeoutMs()))
                .build();

        int attempts = 0;
        boolean success = false;
        HttpResponse<String> response = null;
        Exception lastException = null;

        while (attempts < plugin.getRetryLimit() && !success) {
            attempts++;
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(plugin.getEndpointUrl()))
                        .timeout(java.time.Duration.ofMillis(plugin.getTimeoutMs()))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .build();

                response = client.send(request, HttpResponse.BodyHandlers.ofString());
                int statusCode = response.statusCode();
                if (statusCode >= 200 && statusCode < 300) {
                    success = true;
                } else {
                    lastException = new IOException("Non-2xx status: " + statusCode);
                }
            } catch (IOException | InterruptedException e) {
                lastException = e;
            }
        }

        // After retries, log accordingly
        if (success && response != null) {
            // Info log to user file with HTTP status code
            appendToUserLog(senderName, jsonPayload, String.valueOf(response.statusCode()));
            plugin.getLogger().info("Successfully forwarded DM from " + senderName +
                    " (HTTP " + response.statusCode() + ")");
        } else {
            // Failure: log to user file and to error.log
            String errorMsg = lastException != null ? lastException.getMessage() : "Unknown failure";
            appendToUserLog(senderName, jsonPayload, "ERROR: " + errorMsg);
            logError("Failed to POST DM from " + senderName + ": " + errorMsg, lastException);
        }
    }

    /**
     * Appends a line to <sender>.log under plugins/chathook/logs/
     */
    private void appendToUserLog(String senderName, String jsonString, String status) {
        String entry = String.format("[%s] %s → %s%n",
                java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now()),
                jsonString, status);
        try {
            Files.writeString(
                    plugin.getLogsFolder().toPath().resolve(senderName + ".log"),
                    entry,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING,
                    "Could not append to log file for user " + senderName, e);
        }
    }

    /**
     * Logs errors to plugins/chathook/logs/error.log, including full stack trace if debug=true.
     */
    private void logError(String message, Exception ex) {
        // Write to console
        if (plugin.isDebug()) {
            plugin.getLogger().log(Level.SEVERE, message, ex);
        } else {
            plugin.getLogger().log(Level.SEVERE, message);
        }

        // Append to error.log
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(java.time.format.DateTimeFormatter.ISO_INSTANT
                .format(java.time.Instant.now())).append("] ").append(message).append("\n");
        if (plugin.isDebug() && ex != null) {
            sb.append("StackTrace:\n");
            for (StackTraceElement el : ex.getStackTrace()) {
                sb.append("    ").append(el.toString()).append("\n");
            }
        }

        try {
            Files.writeString(
                    plugin.getLogsFolder().toPath().resolve("error.log"),
                    sb.toString(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to write to error.log", e);
        }
    }
}
