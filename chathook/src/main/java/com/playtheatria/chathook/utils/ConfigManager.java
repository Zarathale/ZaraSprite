package com.playtheatria.chathook.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigManager {

    private final String endpointUrl;
    private final int timeoutMs;
    private final int retryLimit;
    private final String logFolderName;
    private final String botName;

    public ConfigManager(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();

        this.endpointUrl = config.getString("endpoint-url", "http://localhost:5000/receive");
        this.timeoutMs = config.getInt("timeout-ms", 3000);
        this.retryLimit = config.getInt("retry-limit", 3);

        String folder = config.getString("log-folder", "logs");
        if (folder == null || folder.trim().isEmpty()) {
            throw new IllegalArgumentException("log-folder must not be empty");
        }
        this.logFolderName = folder;

        String bot = config.getString("botName", "ZaraSprite");
        if (bot == null || bot.trim().isEmpty()) {
            throw new IllegalArgumentException("botName must not be empty");
        }
        this.botName = bot;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public int getRetryLimit() {
        return retryLimit;
    }

    public String getLogFolderName() {
        return logFolderName;
    }

    public String getBotName() {
        return botName;
    }
}
