package com.playtheatria.chathook.utils;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileLogger {
    private final JavaPlugin plugin;
    private final Logger consoleLogger;
    private final File logsFolder;

    /**
     * Constructor—call this early in onEnable():
     *   File logsDir = new File(plugin.getDataFolder(), "logs");
     *   logsDir.mkdirs();
     *   FileLogger fileLogger = new FileLogger(plugin, logsDir);
     */
    public FileLogger(JavaPlugin pluginInstance, File logsDir) {
        this.plugin = pluginInstance;
        this.consoleLogger = pluginInstance.getLogger();
        this.logsFolder = logsDir;
    }

    /**
     * Append a timestamped line to "<senderName>.log" under logsFolder.
     */
    public void logToUserFile(String senderName, String jsonString, String status) {
        File userLogFile = new File(logsFolder, senderName + ".log");
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        String line = String.format("[%s] %s → %s%n", timestamp, jsonString, status);

        try (FileWriter writer = new FileWriter(userLogFile, true)) {
            writer.write(line);
        } catch (IOException e) {
            consoleLogger.log(Level.WARNING,
                "Could not write to log file for user " + senderName, e);
        }
    }

    /**
     * Log an error message (and stack trace if debugMode=true) to console and to "error.log".
     */
    public void logError(String message, Exception ex, boolean debugMode) {
        if (debugMode && ex != null) {
            consoleLogger.log(Level.SEVERE, message, ex);
        } else {
            consoleLogger.log(Level.SEVERE, message);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[").append(DateTimeFormatter.ISO_INSTANT.format(Instant.now()))
          .append("] ").append(message).append("\n");
        if (debugMode && ex != null) {
            sb.append("StackTrace:\n");
            for (StackTraceElement el : ex.getStackTrace()) {
                sb.append("    ").append(el.toString()).append("\n");
            }
        }

        try {
            Files.writeString(
                logsFolder.toPath().resolve("error.log"),
                sb.toString(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            consoleLogger.log(Level.WARNING, "Failed to write to error.log", e);
        }
    }

    /**
     * Delete every file under logsFolder.
     */
    public void purgeAllLogs() {
        File[] files = logsFolder.listFiles();
        if (files != null) {
            for (File f : files) {
                f.delete();
            }
        }
    }

    /**
     * Delete "<username>.log" if it exists.
     *
     * @return true if deletion succeeded; false otherwise.
     */
    public boolean purgeUserLog(String username) {
        File userLog = new File(logsFolder, username + ".log");
        if (userLog.exists()) {
            return userLog.delete();
        }
        return false;
    }
}
