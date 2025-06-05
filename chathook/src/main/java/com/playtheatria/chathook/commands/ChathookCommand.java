package com.playtheatria.chathook.commands;

import com.playtheatria.chathook.utils.ConfigManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class ChathookCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final ConfigManager cfg;

    public ChathookCommand(JavaPlugin plugin, ConfigManager cfg) {
        this.plugin = plugin;
        this.cfg = cfg;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Only players or console with the “chathook.admin” permission can run these subcommands
        if (!sender.hasPermission("chathook.admin")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§7/chathook reload");
            sender.sendMessage("§7/chathook purge all");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                // Reload config.yml and update ConfigManager
                plugin.reloadConfig();
                cfg.reload(plugin.getConfig());
                sender.sendMessage("§aChathook configuration reloaded.");
                return true;

            case "purge":
                // Only “/chathook purge all” is allowed now
                if (args.length == 2 && args[1].equalsIgnoreCase("all")) {
                    File logsDir = new File(plugin.getDataFolder(), "logs");
                    File[] files = logsDir.listFiles((dir, name) ->
                        name.startsWith("chathook-") && name.endsWith(".log")
                    );
                    if (files != null) {
                        for (File f : files) {
                            f.delete();
                        }
                    }
                    sender.sendMessage("§aAll chathook rolling logs have been purged.");
                    plugin.getLogger().info("All chathook logs deleted by " + sender.getName());
                    return true;
                } else {
                    sender.sendMessage("§cUsage: /chathook purge all");
                    return true;
                }

            default:
                sender.sendMessage("§7/chathook reload");
                sender.sendMessage("§7/chathook purge all");
                return true;
        }
    }
}
