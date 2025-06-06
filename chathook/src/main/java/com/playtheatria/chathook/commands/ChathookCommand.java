package com.playtheatria.chathook.commands;

import com.playtheatria.chathook.utils.ConfigManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * Handles the /chathook command. Only two subcommands are supported:
 *   - reload       (reloads config.yml)
 *   - purge all    (deletes all log files under plugins/Chathook/logs/)
 *
 * Both subcommands require the "chathook.admin" permission.
 */
public class ChathookCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final ConfigManager cfg;

    public ChathookCommand(JavaPlugin plugin, ConfigManager cfg) {
        this.plugin = plugin;
        this.cfg = cfg;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // No arguments: show usage
        if (args.length == 0) {
            sender.sendMessage("§cUsage: /chathook <reload|purge all>");
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "reload":
                // /chathook reload
                if (!sender.hasPermission("chathook.admin")) {
                    sender.sendMessage("§cYou lack permission to run /chathook reload.");
                    return true;
                }
                cfg.reload();
                plugin.getLogger().info("Chathook configuration reloaded by " + sender.getName());
                sender.sendMessage("§aChathook configuration reloaded.");
                return true;

            case "purge":
                // Expect exactly "purge all"
                if (args.length != 2 || !args[1].equalsIgnoreCase("all")) {
                    sender.sendMessage("§cUsage: /chathook purge all");
                    return true;
                }
                if (!sender.hasPermission("chathook.admin")) {
                    sender.sendMessage("§cYou lack permission to run /chathook purge all.");
                    return true;
                }

                // Locate the logs directory under the plugin’s data folder
                File logsDir = new File(plugin.getDataFolder(), "logs");
                if (!logsDir.exists() || !logsDir.isDirectory()) {
                    sender.sendMessage("§eNo log directory found to purge.");
                    return true;
                }

                File[] files = logsDir.listFiles();
                if (files == null || files.length == 0) {
                    sender.sendMessage("§eNo log files to purge.");
                    return true;
                }

                int deletedCount = 0;
                for (File f : files) {
                    if (f.isFile()) {
                        if (f.delete()) {
                            deletedCount++;
                        } else {
                            plugin.getLogger().warning("Failed to delete log file: " + f.getAbsolutePath());
                        }
                    }
                }

                sender.sendMessage("§aPurged " + deletedCount + " log file(s).");
                plugin.getLogger().info("All chathook logs were purged by " + sender.getName());
                return true;

            default:
                // Unknown subcommand: show usage
                sender.sendMessage("§cUnknown subcommand. Usage:");
                sender.sendMessage("§7/chathook reload");
                sender.sendMessage("§7/chathook purge all");
                return true;
        }
    }
}
