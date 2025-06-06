package com.playtheatria.chathook.utils;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Manages loading and reloading of plugin configuration (config.yml).
 *
 * Usage:
 *   In your main class (Chathook.java):
 *       cfg = new ConfigManager(this);
 *   In your /chathook reload handler:
 *       cfg.reload();
 */
public class ConfigManager {
    private final JavaPlugin plugin;

    private String endpointUrl;
    private int timeoutMs;
    private int retryLimit;
    private String botName;
    private boolean debug;

    // New: Directory name under plugins/Chathook/ where logs are stored
    private String logFolderName;

    private int logMaxBytes;
    private int logFileCount;

    /**
     * Constructor: immediately loads (and validates) all values from config.yml.
     * If any required field is missing/invalid, the plugin is disabled.
     *
     * @param plugin Reference to the JavaPlugin (so we can load/reload config).
     */
    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;

        // Ensure a default config.yml is copied if missing
        plugin.saveDefaultConfig();

        // Load and validate
        loadConfig();
    }

    /**
     * Reloads config.yml from disk and re-validates.
     * Call this when handling "/chathook reload".
     */
    public void reload() {
        // Reload the in-memory configuration
        plugin.reloadConfig();
        loadConfig();
    }

    /**
     * Reads all values from plugin.getConfig() and applies defaults/validation.
     * If any required key is missing or clearly invalid, disables the plugin.
     */
    private void loadConfig() {
        // Grab the FileConfiguration
        var config = plugin.getConfig();

        // ————————————————
        // Load values (with defaults where appropriate)
        // ————————————————
        this.debug = config.getBoolean("debug", false);
        this.endpointUrl = config.getString("endpoint-url", "").trim();
        this.timeoutMs = config.getInt("timeout-ms", 5000);
        this.retryLimit = config.getInt("retry-limit", 3);
        this.botName = config.getString("botName", "").trim();

        // Honor the configured folder name for logs (default = "logs")
        this.logFolderName = config.getString("log-folder", "logs").trim();

        // Rolling‐file settings
        this.logMaxBytes = config.getInt("log-max-bytes", 10_000_000);
        this.logFileCount = config.getInt("log-file-count", 5);

        // ————————————————
        // Validation
        // ————————————————
        boolean hasError = false;

        if (endpointUrl.isEmpty()) {
            plugin.getLogger().severe("Config error: 'endpoint-url' is missing or empty in config.yml");
            hasError = true;
        }

        if (botName.isEmpty()) {
            plugin.getLogger().severe("Config error: 'botName' is missing or empty in config.yml");
            hasError = true;
        }

        if (timeoutMs < 1) {
            plugin.getLogger().warning("Invalid 'timeout-ms' (" + timeoutMs + "); falling back to 5000");
            timeoutMs = 5000;
        }

        if (retryLimit < 0) {
            plugin.getLogger().warning("Invalid 'retry-limit' (" + retryLimit + "); falling back to 3");
            retryLimit = 3;
        }

        if (logFolderName.isEmpty()) {
            plugin.getLogger().warning("Invalid 'log-folder' (empty); falling back to 'logs'");
            logFolderName = "logs";
        }

        if (logMaxBytes < 1) {
            plugin.getLogger().warning("Invalid 'log-max-bytes' (" + logMaxBytes + "); falling back to 10_000_000");
            logMaxBytes = 10_000_000;
        }

        if (logFileCount < 1) {
            plugin.getLogger().warning("Invalid 'log-file-count' (" + logFileCount + "); falling back to 5");
            logFileCount = 5;
        }

        if (hasError) {
            plugin.getLogger().severe("=======================");
            plugin.getLogger().severe("Disabling Chathook due to config errors.");
            plugin.getLogger().severe("=======================");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        }
    }

    /** @return true if debug stack traces should be written to logs. */
    public boolean isDebug() {
        return debug;
    }

    /** @return The HTTP endpoint URL to which DMs are POSTed. */
    public String getEndpointUrl() {
        return endpointUrl;
    }

    /** @return Connection/read timeout (milliseconds) for each HTTP POST. */
    public int getTimeoutMs() {
        return timeoutMs;
    }

    /** @return Number of retry attempts if a POST fails. */
    public int getRetryLimit() {
        return retryLimit;
    }

    /** @return The in-game bot name (e.g., "ZaraSprite"). */
    public String getBotName() {
        return botName;
    }

    /**
     * @return The folder name under 'plugins/Chathook/' where logs should be stored.
     *         (Defaults to "logs" if unset or invalid.)
     */
    public String getLogFolderName() {
        return logFolderName;
    }

    /** @return Maximum size (bytes) of each rolling log file. */
    public int getLogMaxBytes() {
        return logMaxBytes;
    }

    /** @return Number of rolling log files to keep before overwriting oldest. */
    public int getLogFileCount() {
        return logFileCount;
    }
}
