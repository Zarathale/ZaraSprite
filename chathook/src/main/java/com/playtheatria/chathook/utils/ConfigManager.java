package com.playtheatria.chathook.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class ConfigManager {
    private final File configFile;
    private FileConfiguration config;

    private boolean debug;
    private String endpointUrl;
    private int timeoutMs;
    private int retryLimit;

    /**
     * Call once in onEnable() of ChathookPlugin:
     *   configManager = new ConfigManager(getDataFolder());
     */
    public ConfigManager(File dataFolder) {
        this.configFile = new File(dataFolder, "config.yml");
        loadFromFile();
    }

    private void loadFromFile() {
        // If config.yml doesn’t exist, the plugin’s saveDefaultConfig() already created it.
        this.config = YamlConfiguration.loadConfiguration(configFile);
        this.debug = config.getBoolean("debug", false);
        this.endpointUrl = config.getString("endpoint-url", "");
        this.timeoutMs = config.getInt("timeout-ms", 5000);
        this.retryLimit = config.getInt("retry-limit", 3);
    }

    /** Re-read config.yml from disk and update fields. */
    public void reload() {
        loadFromFile();
    }

    public boolean isDebug() {
        return debug;
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
}
