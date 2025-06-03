package com.example.chathook;

import org.bukkit.plugin.java.JavaPlugin;

public class ChatHookPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        // register the listener
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getLogger().info("ChatHook enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("ChatHook disabled.");
    }
}
