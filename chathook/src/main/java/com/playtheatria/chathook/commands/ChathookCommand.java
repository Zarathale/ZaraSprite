package com.playtheatria.chathook.commands;

import com.playtheatria.chathook.utils.ConfigManager;
import com.playtheatria.chathook.utils.FileLogger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.List;

public class ChathookCommand implements TabExecutor {
    private final ConfigManager cfg;
    private final FileLogger fileLogger; // NEW field

    /**
     * UPDATED constructor: accept both managers
     */
    public ChathookCommand(ConfigManager cfg, FileLogger fileLogger) {
        this.cfg = cfg;
        this.fileLogger = fileLogger;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§eUsage: /chathook <purgeall|purger <username>|reload>");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "purgeall":
                if (!sender.isOp() && !sender.hasPermission("chathook.purgeall")) {
                    sender.sendMessage("§cYou don’t have permission to purge all logs.");
                    return true;
                }
                fileLogger.purgeAllLogs();
                sender.sendMessage("§aAll user logs purged.");
                break;

            case "purger":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /chathook purger <username>");
                    return true;
                }
                String targetUser = args[1];
                boolean canPurge =
                    sender.isOp()
                    || sender.hasPermission("chathook.purgeall")
                    || sender.getName().equalsIgnoreCase(targetUser);
                if (!canPurge) {
                    sender.sendMessage("§cYou don’t have permission to purge logs for “" + targetUser + "”.");
                    return true;
                }
                boolean deleted = fileLogger.purgeUserLog(targetUser);
                if (deleted) {
                    sender.sendMessage("§aPurged logs for " + targetUser + ".");
                } else {
                    sender.sendMessage("§eNo logs exist for user " + targetUser + ".");
                }
                break;

            case "reload":
                if (!sender.isOp() && !sender.hasPermission("chathook.reload")) {
                    sender.sendMessage("§cYou don’t have permission to reload config.");
                    return true;
                }
                cfg.reload();
                sender.sendMessage("§aConfiguration reloaded.");
                break;

            default:
                sender.sendMessage("§cUnknown subcommand. Options: purgeall, purger, reload");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("purgeall", "purger", "reload");
        }
        return List.of();
    }
}
