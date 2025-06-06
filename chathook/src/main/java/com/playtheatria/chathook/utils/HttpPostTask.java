package com.playtheatria.chathook.utils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Utility for sending JSON payloads to a configured endpoint, with retry logic.
 * 
 * Usage:
 *   HttpPostTask.postJson(jsonPayload, configManager);
 * 
 * This runs synchronously (intended to be called off the main server thread).
 */
public class HttpPostTask {
    // A single, reusable HttpClient instance:
    private static HttpClient sharedClient = null;

    /**
     * Send a JSON payload to the endpoint defined in cfg.
     * Retries up to cfg.getRetryLimit() times on failure, pausing briefly between attempts.
     *
     * Must be called off the main Minecraft thread (e.g., via Bukkit.getScheduler().runTaskAsynchronously()).
     *
     * @param jsonPayload The JSON string to POST.
     * @param cfg         The ConfigManager, providing endpoint URL, timeout, retry limit, and debug flag.
     */
    public static void postJson(String jsonPayload, ConfigManager cfg) {
        // Initialize the shared client if not already done:
        if (sharedClient == null) {
            synchronized (HttpPostTask.class) {
                if (sharedClient == null) {
                    sharedClient = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofMillis(cfg.getTimeoutMs()))
                        .build();
                }
            }
        }

        // Build the HTTP POST request once
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(cfg.getEndpointUrl()))
            .timeout(Duration.ofMillis(cfg.getTimeoutMs()))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .build();

        int attempts = 0;
        int maxAttempts = Math.max(cfg.getRetryLimit(), 1);
        boolean success = false;

        while (attempts < maxAttempts) {
            attempts++;
            try {
                HttpResponse<String> response = sharedClient.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();
                if (status >= 200 && status < 300) {
                    // Success – exit early
                    success = true;
                    break;
                } else {
                    // Non-2xx response: log a warning and retry
                    String warnMsg = String.format(
                        "HTTP POST returned status %d (attempt %d/%d) for endpoint %s",
                        status, attempts, maxAttempts, cfg.getEndpointUrl()
                    );
                    FileLogger.getInstance().logError(warnMsg, null, cfg.isDebug());
                }
            } catch (IOException | InterruptedException e) {
                // Network or interruption error: log and retry
                String errMsg = String.format(
                    "Exception during HTTP POST (attempt %d/%d) to %s",
                    attempts, maxAttempts, cfg.getEndpointUrl()
                );
                FileLogger.getInstance().logError(errMsg, e, cfg.isDebug());
            }

            // If not the last attempt, wait briefly before retrying
            if (attempts < maxAttempts) {
                try {
                    Thread.sleep(500L); // 500ms pause between retries
                } catch (InterruptedException ie) {
                    // Restore interrupt flag and break out
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        if (!success) {
            String finalMsg = String.format(
                "All %d HTTP POST attempts failed for endpoint %s",
                maxAttempts, cfg.getEndpointUrl()
            );
            FileLogger.getInstance().logError(finalMsg, null, cfg.isDebug());
        }
    }
}
