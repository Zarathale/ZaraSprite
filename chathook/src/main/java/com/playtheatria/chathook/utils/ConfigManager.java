package com.playtheatria.chathook.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigManager {

    private final JavaPlugin plugin;
    private final FileConfiguration config;

    // Config keys
    private static final String ENDPOINT_URL_KEY = "endpoint-url";
    private static final String TIMEOUT_MS_KEY = "timeout-ms";
    private static final String RETRY_LIMIT_KEY = "retry-limit";
    private static final String DEBUG_KEY = "debug";

    // Defaults
    private static final int DEFAULT_TIMEOUT_MS = 5000;
    private static final int DEFAULT_RETRY_LIMIT = 3;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.plugin.saveDefaultConfig();
        this.config = plugin.getConfig();

        validateConfig();
    }

    private void validateConfig() {
        boolean changed = false;

        if (!config.contains(ENDPOINT_URL_KEY)) {
            config.set(ENDPOINT_URL_KEY, "http://localhost:5000/chat");
            changed = true;
        }

        if (!config.contains(TIMEOUT_MS_KEY)) {
            config.set(TIMEOUT_MS_KEY, DEFAULT_TIMEOUT_MS);
            changed = true;
        }

        if (!config.contains(RETRY_LIMIT_KEY)) {
            config.set(RETRY_LIMIT_KEY, DEFAULT_RETRY_LIMIT);
            changed = true;
        }

        if (!config.contains(DEBUG_KEY)) {
            config.set(DEBUG_KEY, false);
            changed = true;
        }

        if (changed) {
            plugin.saveConfig();
        }
    }

    public String getEndpointUrl() {
        return config.getString(ENDPOINT_URL_KEY);
    }

    public int getTimeoutMs() {
        return config.getInt(TIMEOUT_MS_KEY, DEFAULT_TIMEOUT_MS);
    }

    public int getRetryLimit() {
        return config.getInt(RETRY_LIMIT_KEY, DEFAULT_RETRY_LIMIT);
    }

    public boolean isDebug() {
        return config.getBoolean(DEBUG_KEY, false);
    }
}
