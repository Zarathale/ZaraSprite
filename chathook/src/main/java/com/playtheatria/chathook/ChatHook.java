package com.playtheatria.chathook;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Main plugin class for chathook.
 * Responsible for loading configuration, registering listeners and commands,
 * and tracking asynchronous HTTP tasks for graceful shutdown.
 */
public class ChatHookPlugin extends JavaPlugin {

    // Configuration fields (defaults overridden by config.yml)
    private String endpointUrl;
    private int timeoutMs;
    private int retryLimit;
    private boolean debug;

    // List of pending BukkitTask IDs to cancel on disable
    private final List<BukkitTask> pendingTasks = new ArrayList<>();

    // Folder where logs will be written: plugins/chathook/logs/
    private File logsFolder;

    @Override
    public void onEnable() {
        // Ensure plugin data folder exists
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // Create logs directory if absent
        logsFolder = new File(getDataFolder(), "logs");
        if (!logsFolder.exists()) {
            logsFolder.mkdirs();
        }

        // Save default config if missing
        saveDefaultConfig();
        loadConfigValues();

        // Register listener for private messages
        getServer().getPluginManager().registerEvents(new PMListener(this), this);

        // Register command executor
        getCommand("chathook").setExecutor(new ChathookCommand(this));

        getLogger().info("chathook enabled. Forwarding DMs to: " + endpointUrl);
    }

    @Override
    public void onDisable() {
        // Cancel any pending asynchronous tasks
        for (BukkitTask task : pendingTasks) {
            task.cancel();
        }
        getLogger().info("chathook disabled—cancelling pending HTTP requests.");
    }

    /**
     * Load configuration values from config.yml into memory.
     */
    public void loadConfigValues() {
        endpointUrl = getConfig().getString("endpoint-url", "http://zarachat.duckdns.org:5000/chat");
        timeoutMs   = getConfig().getInt("timeout-ms", 5000);
        retryLimit  = getConfig().getInt("retry-limit", 3);
        debug       = getConfig().getBoolean("debug", false);

        getLogger().info("Config loaded: endpoint-url=" + endpointUrl +
                ", timeout-ms=" + timeoutMs + ", retry-limit=" + retryLimit + ", debug=" + debug);
    }

    // Accessor methods for other classes

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public int getRetryLimit() {
        return retryLimit;
    }

    public boolean isDebug() {
        return debug;
    }

    public File getLogsFolder() {
        return logsFolder;
    }

    /**
     * Register a BukkitTask so we can cancel it on disable.
     */
    public void trackTask(BukkitTask task) {
        pendingTasks.add(task);
    }
}
