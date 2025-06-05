package com.playtheatria.chathook.commands;

import com.playtheatria.chathook.utils.ConfigManager;
import com.playtheatria.chathook.utils.FileLogger;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class ChathookCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;

    public ChathookCommand(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) return false;

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("chathook.admin")) {
                    sender.sendMessage("§cYou don’t have permission to do that.");
                    return true;
                }
                plugin.reloadConfig();
                sender.sendMessage("§aChathook config reloaded.");
                return true;

            case "purge":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /chathook purge <username|all>");
                    return true;
                }

                String target = args[1];
                if (target.equalsIgnoreCase("all")) {
                    if (!sender.hasPermission("chathook.admin")) {
                        sender.sendMessage("§cYou don’t have permission to purge all logs.");
                        return true;
                    }
                    FileLogger.purgeAllLogs();
                    sender.sendMessage("§aAll user logs purged.");
                    return true;
                } else {
                    if (!sender.hasPermission("chathook.admin") &&
                        !(sender instanceof Player && sender.getName().equalsIgnoreCase(target))) {
                        sender.sendMessage("§cYou can only purge your own logs.");
                        return true;
                    }

                    FileLogger.purgeUserLog(target);
                    sender.sendMessage("§aLog for §e" + target + "§a purged.");
                    return true;
                }

            default:
                sender.sendMessage("§7/chathook reload");
                sender.sendMessage("§7/chathook purge <username|all>");
                return true;
        }
    }
}
