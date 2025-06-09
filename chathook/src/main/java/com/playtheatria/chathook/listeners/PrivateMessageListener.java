package com.playtheatria.chathook.listeners;

import com.playtheatria.chathook.utils.ConfigManager;
import com.playtheatria.chathook.utils.FileLogger;
import com.playtheatria.chathook.utils.HttpPostTask;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.UUID;

/**
 * Listens for /msg or /tell directed at our bot name and
 * forwards them to the external Flask endpoint.
 */
public class PrivateMessageListener implements Listener {
    private final JavaPlugin plugin;
    private final ConfigManager cfg;
    private final FileLogger fileLogger;

    public PrivateMessageListener(JavaPlugin plugin,
                                  ConfigManager cfg,
                                  FileLogger fileLogger) {
        this.plugin     = plugin;
        this.cfg        = cfg;
        this.fileLogger = fileLogger;
    }

    @EventHandler
    public void onPlayerPrivateMessage(PlayerCommandPreprocessEvent event) {
        String raw = event.getMessage();             // e.g. "/msg ZaraSprite hello"
        String[] parts = raw.split(" ", 3);
        if (parts.length < 3) return;                // not enough pieces

        String cmd = parts[0].substring(1).toLowerCase();
        if (!cmd.equals("msg") && !cmd.equals("tell")) return;

        String target = parts[1];
        if (!target.equalsIgnoreCase(cfg.getBotName())) return;

        // we’ve confirmed: /msg ZaraSprite <content>
        String content = parts[2];
        Player sender = event.getPlayer();

        // build JSON payload
        String id        = UUID.randomUUID().toString();
        String timestamp = Instant.now().toString();
        String json = String.format(
            "{\"id\":\"%s\",\"player\":\"%s\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
            id, sender.getName(), escapeJson(content), timestamp
        );

        // log to file and forward
        fileLogger.logToUserFile(sender.getName(), json, "SENT");
        HttpPostTask.postJson(plugin, json, cfg);

    }

    private String escapeJson(String raw) {
        return raw.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
