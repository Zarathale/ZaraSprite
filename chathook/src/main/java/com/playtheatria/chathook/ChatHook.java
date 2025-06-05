package com.playtheatria.chathook;

import com.playtheatria.chathook.commands.ChathookCommand;
import com.playtheatria.chathook.listeners.PrivateMessageListener;
import com.playtheatria.chathook.utils.ConfigManager;
import com.playtheatria.chathook.utils.FileLogger;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class Chathook extends JavaPlugin {
    private ConfigManager configManager;
    private FileLogger fileLogger;

    @Override
    public void onEnable() {
        // 1) Ensure config.yml is present
        saveDefaultConfig();

        // 2) Instantiate ConfigManager via new constructor
        configManager = new ConfigManager(this);

        // 3) Create logs folder and instantiate FileLogger via new constructor
        File logsDir = new File(getDataFolder(), "logs");
        logsDir.mkdirs();
        fileLogger = new FileLogger(this, logsDir);

        // 4) Register the private‐message listener, passing in both helpers
        getServer().getPluginManager().registerEvents(
            new PrivateMessageListener(this, configManager, fileLogger),
            this
        );

        // 5) Register /chathook command, passing in both helpers
        getCommand("chathook").setExecutor(new ChathookCommand(configManager, fileLogger));
    }

    @Override
    public void onDisable() {
        // No extra cleanup necessary now
    }
}
