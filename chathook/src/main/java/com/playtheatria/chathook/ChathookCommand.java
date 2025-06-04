package com.playtheatria.chathook;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.io.File;

/**
 * Handles "/chathook" commands:
 *   - reload         → requires chathook.mod or chathook.admin
 *   - purge all      → requires chathook.mod or chathook.admin
 *   - purge <user>   → allowed if (sender == <user>) OR (sender has chathook.mod) OR (sender has chathook.admin)
 */
public class ChathookCommand implements CommandExecutor {

    private final ChatHookPlugin plugin;

    public ChathookCommand(ChatHookPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Base permission check: requires at least chathook.mod
        if (!sender.hasPermission("chathook.mod") && !sender.hasPermission("chathook.admin")) {
            sender.sendMessage("§cYou do not have permission to use any /chathook commands.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§eUsage: /chathook reload | purge <username> | purge all");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "reload":
                // reload → only mod or admin
                if (!sender.hasPermission("chathook.mod") && !sender.hasPermission("chathook.admin")) {
                    sender.sendMessage("§cYou do not have permission to reload chathook.");
                    return true;
                }
                plugin.reloadConfig();
                plugin.loadConfigValues();
                sender.sendMessage("§aConfig reloaded.");
                plugin.getLogger().info("Config reloaded by " + sender.getName());
                break;

            case "purge":
                if (args.length < 2) {
                    sender.sendMessage("§eUsage: /chathook purge <username> | purge all");
                    return true;
                }

                String target = args[1];
                if (target.equalsIgnoreCase("all")) {
                    // purge all → only mod or admin
                    if (!sender.hasPermission("chathook.mod") && !sender.hasPermission("chathook.admin")) {
                        sender.sendMessage("§cYou do not have permission to purge all logs.");
                        return true;
                    }
                    purgeAllLogs();
                    sender.sendMessage("§aPurged all chathook logs.");
                    plugin.getLogger().info("All chathook logs purged by " + sender.getName());
                } else {
                    // purge <username> → allowed if sender == target OR has mod/admin
                    if (sender.getName().equalsIgnoreCase(target)
                            || sender.hasPermission("chathook.mod")
                            || sender.hasPermission("chathook.admin")) {
                        purgeUserLog(target, sender);
                    } else {
                        sender.sendMessage("§cYou can only purge your own logs or must have mod permissions.");
                    }
                }
                break;

            default:
                sender.sendMessage("§eUnknown subcommand. Usage: /chathook reload | purge <username> | purge all");
        }

        return true;
    }

    private void purgeAllLogs() {
        File logsDir = plugin.getLogsFolder();
        File[] files = logsDir.listFiles();
        if (files != null) {
            for (File f : files) {
                f.delete();
            }
        }
    }

    private void purgeUserLog(String username, CommandSender sender) {
        File userLog = new File(plugin.getLogsFolder(), username + ".log");
        if (userLog.exists()) {
            if (userLog.delete()) {
                sender.sendMessage("§aPurged logs for " + username + ".");
            } else {
                sender.sendMessage("§cFailed to purge logs for " + username + ".");
            }
        } else {
            sender.sendMessage("§eNo logs exist for user " + username + ".");
        }
    }
}
