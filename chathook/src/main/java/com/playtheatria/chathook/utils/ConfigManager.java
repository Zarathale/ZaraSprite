package com.playtheatria.chathook.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import com.playtheatria.chathook.Chathook;

import java.io.File;

public class ConfigManager {
    private final Chathook plugin;
    private final File configFile;
    private FileConfiguration config;

    private boolean debug;
    private String endpointUrl;
    private int timeoutMs;
    private int retryLimit;

    /**
     * Constructor—call this early in onEnable():
     *   ConfigManager configManager = new ConfigManager(this);
     */
    public ConfigManager(Chathook pluginInstance) {
        this.plugin = pluginInstance;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        loadFromFile();
    }

    /**
     * Load values from config.yml into fields.
     */
    private void loadFromFile() {
        // Assume saveDefaultConfig() was already called in the main plugin.
        this.config = YamlConfiguration.loadConfiguration(configFile);
        this.debug = config.getBoolean("debug", false);
        this.endpointUrl = config.getString("endpoint-url", "");
        this.timeoutMs = config.getInt("timeout-ms", 5000);
        this.retryLimit = config.getInt("retry-limit", 3);
    }

    /** Re-read config.yml from disk. */
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
