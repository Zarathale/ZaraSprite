package com.playtheatria.chathook;

import com.playtheatria.chathook.commands.ChathookCommand;
import com.playtheatria.chathook.listeners.PrivateMessageListener;
import com.playtheatria.chathook.utils.ConfigManager;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class Chathook extends JavaPlugin {

    private ConfigManager configManager;

    @Override
    public void onEnable() {
        // 1. Save default config if not present
        saveDefaultConfig();

        // 2. Create /logs directory if missing
        File logsDir = new File(getDataFolder(), "logs");
        logsDir.mkdirs();

        // 3. Load config and validate keys
        configManager = new ConfigManager(this);

        // 4. Register PM listener
        getServer().getPluginManager().registerEvents(
            new PrivateMessageListener(this, configManager),
            this);

        // 5. Register /chathook command
        getCommand("chathook").setExecutor(new ChathookCommand(this, configManager));

        // 6. Log plugin enable message
        getLogger().info("Chathook enabled. Config loaded with endpoint: " + configManager.getEndpointUrl()););
    }

    @Override
    public void onDisab
le() {
        // Cleanup if needed
        getLogger().info("Chathook disabled.");
    }