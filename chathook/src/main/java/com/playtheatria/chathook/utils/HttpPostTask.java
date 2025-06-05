package com.playtheatria.chathook.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class HttpPostTask {
    /**
     * Send a JSON payload to the endpoint defined in cfg.
     * Retries up to cfg.getRetryLimit() on failure.
     */
    public static void postJson(String jsonPayload, ConfigManager cfg) {
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(cfg.getTimeoutMs()))
            .build();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(cfg.getEndpointUrl()))
            .timeout(Duration.ofMillis(cfg.getTimeoutMs()))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .build();

        int attempts = 0;
        while (attempts < cfg.getRetryLimit()) {
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return; // Success
                }
            } catch (Exception e) {
                FileLogger.logError("HTTP POST failed", e, cfg.isDebug());
            }
            attempts++;
        }
        // If we reach here, all retries failed; caller may log or ignore.
    }
}
