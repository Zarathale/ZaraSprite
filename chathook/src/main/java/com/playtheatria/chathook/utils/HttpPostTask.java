package com.playtheatria.chathook.utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Sends a JSON payload to the configured endpoint, with automatic
 * async scheduling and recursive retry (no blocking sleeps).
 */
public class HttpPostTask {
    private static HttpClient sharedClient = null;

    /**
     * Begin an asynchronous POST of the given JSON.  This will
     * schedule itself off the main thread and retry up to cfg.getRetryLimit()
     * with a 10‐tick backoff.
     *
     * @param plugin      Your main JavaPlugin instance
     * @param jsonPayload The JSON string to POST
     * @param cfg         ConfigManager (endpoint-url, timeout-ms, retry-limit, debug)
     */
    public static void postJson(JavaPlugin plugin, String jsonPayload, ConfigManager cfg) {
        ensureClient(cfg);
        // Kick off first attempt immediately (off the main thread)
        Bukkit.getScheduler()
              .runTaskAsynchronously(plugin, () -> attemptPost(plugin, jsonPayload, cfg, 1));
    }

    private static void ensureClient(ConfigManager cfg) {
        if (sharedClient == null) {
            synchronized (HttpPostTask.class) {
                if (sharedClient == null) {
                    sharedClient = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofMillis(cfg.getTimeoutMs()))
                        .build();
                }
            }
        }
    }

    private static void attemptPost(JavaPlugin plugin,
                                    String jsonPayload,
                                    ConfigManager cfg,
                                    int attempt) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(cfg.getEndpointUrl()))
            .timeout(Duration.ofMillis(cfg.getTimeoutMs()))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .build();

        try {
            HttpResponse<String> response = sharedClient.send(request, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();
            if (code < 200 || code >= 300) {
                scheduleRetry(plugin, jsonPayload, cfg, attempt);
            }
        } catch (Exception e) {
            if (cfg.isDebug()) {
                e.printStackTrace();
            }
            scheduleRetry(plugin, jsonPayload, cfg, attempt);
        }
    }

    private static void scheduleRetry(JavaPlugin plugin,
                                      String jsonPayload,
                                      ConfigManager cfg,
                                      int attempt) {
        if (attempt < cfg.getRetryLimit()) {
            // retry after 10 ticks (~500ms)
            Bukkit.getScheduler()
                  .runTaskLaterAsynchronously(plugin,
                                              () -> attemptPost(plugin, jsonPayload, cfg, attempt + 1),
                                              10L);
        }
    }
}
