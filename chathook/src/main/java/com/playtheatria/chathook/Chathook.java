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
        // 1) Default config
        saveDefaultConfig();
        configManager = new ConfigManager(this);

        // 2) Initialize the singleton FileLogger
        FileLogger.initialize(
            this,
            getDataFolder(),
            configManager.getLogFolderName(),
            configManager.isDebug(),
            configManager.getLogMaxBytes(),
            configManager.getLogFileCount()
        );

        // 3) Grab the instance and register our listener (3 args)
        FileLogger logger = FileLogger.getInstance();
        getServer().getPluginManager().registerEvents(
            new PrivateMessageListener(this, configManager, logger),
            this
        );

        // 4) Command registration
        getCommand("chathook").setExecutor(
            new ChathookCommand(this, configManager)
        );

        getLogger().info("Chathook enabled. Endpoint: " + configManager.getEndpointUrl());
    }

    @Override
    public void onDisable() {
        getLogger().info("Chathook disabled.");
    }
}
