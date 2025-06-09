package com.playtheatria.chathook;

import com.playtheatria.chathook.commands.ChathookCommand;
import com.playtheatria.chathook.listeners.PrivateMessageListener;
import com.playtheatria.chathook.utils.ConfigManager;
import com.playtheatria.chathook.utils.FileLogger;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class Chathook extends JavaPlugin {

    private ConfigManager configManager;

    @Override
    public void onEnable() {
        // 1. Save (or copy) the default config.yml if it doesn't exist
        saveDefaultConfig();

        // 2. Initialize ConfigManager by passing 'this' so it can load and validate
        configManager = new ConfigManager(this);

        // 3. Initialize FileLogger with rolling‐file settings:
        //    • plugin data folder (plugins/Chathook)
        //    • log folder name (from config)
        //    • debug flag (from config)
        //    • max bytes per file (from config)
        //    • number of files to keep (from config)
        File dataFolder = this.getDataFolder();
        String logFolderName = configManager.getLogFolderName();
        FileLogger.initialize(
            dataFolder,
            logFolderName,
            configManager.isDebug(),
            configManager.getLogMaxBytes(),
            configManager.getLogFileCount()
        );

        // 4. Register the PrivateMessageListener
        //    FileLogger.getInstance() returns the singleton static logger.
        getServer().getPluginManager().registerEvents(
            new PrivateMessageListener(this, configManager, FileLogger.getInstance()),
            this
        );

        // 5. Register the /chathook command
        getCommand("chathook").setExecutor(new ChathookCommand(this, configManager));

        // 6. Log that we enabled successfully (include endpoint for sanity)
        getLogger().info("Chathook enabled. Endpoint: " + configManager.getEndpointUrl());
    }

    @Override
    public void onDisable() {
        getLogger().info("Chathook disabled.");
    }
}
