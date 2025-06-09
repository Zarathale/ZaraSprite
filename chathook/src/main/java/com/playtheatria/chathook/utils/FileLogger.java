package com.playtheatria.chathook.utils;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton utility for rolling‐file logging.
 *
 * Usage (in your main plugin class’s onEnable()):
 *   FileLogger.initialize(
 *       getDataFolder(),
 *       configManager.isDebug(),
 *       configManager.getLogMaxBytes(),
 *       configManager.getLogFileCount()
 *   );
 *
 * Then anywhere else:
 *   FileLogger.getInstance().logToUserFile(...);
 *   FileLogger.getInstance().logError(...);
 *   FileLogger.getInstance().purgeAllLogs();
 *   // etc.
 */
public class FileLogger {
    private static FileLogger instance = null;

    private final JavaPlugin plugin;
    private final Logger consoleLogger;
    private final File logsFolder;

    // Rolling‐file parameters for error.log
    private final int maxBytes;
    private final int fileCount;
    private final boolean debugMode;

    private static final String ERROR_LOG_BASE = "error.log";
    private static final DateTimeFormatter ISO_TS = DateTimeFormatter.ISO_INSTANT;

    /** 
     * Private constructor—use initialize(...) instead.
     */
    private FileLogger(JavaPlugin pluginInstance,
                       File logsDir,
                       boolean debugMode,
                       int maxBytes,
                       int fileCount) {
        this.plugin = pluginInstance;
        this.consoleLogger = pluginInstance.getLogger();
        this.logsFolder = logsDir;
        this.debugMode = debugMode;
        this.maxBytes = maxBytes;
        this.fileCount = fileCount;
    }

/**
 * @param dataFolder    the plugin’s data folder (i.e. plugin.getDataFolder())
 * @param logFolderName sub-directory name under dataFolder where logs are written
 * @param debug         whether to enable full stack-trace logging
 * @param retainDays    number of days’ worth of logs to keep
 * @param maxSizeMb     maximum size per log file (in MB)
 */
    public static void initialize(
        JavaPlugin pluginInstance,
        File dataFolder,
        String logFolderName,
        boolean debugMode,
        int maxBytesPerFile,
        int maxFileCount
    ) {
        File logsDir = new File(dataFolder, logFolderName);
        logsDir.mkdirs();
        // create & store the singleton
        instance = new FileLogger(pluginInstance, logsDir, debugMode, maxBytesPerFile, maxFileCount);
    }

    /** 
     * Retrieve the singleton instance (after having called initialize()). 
     * 
     * @throws IllegalStateException if initialize(...) was never called.
     */
    public static FileLogger getInstance() {
        if (instance == null) {
            throw new IllegalStateException("FileLogger has not been initialized. Call FileLogger.initialize(...) first.");
        }
        return instance;
    }

    /**
     * Append a timestamped line to "<senderName>.log" under logsFolder.
     * This method is synchronized to prevent interleaved writes.
     *
     * @param senderName e.g. "Player123"
     * @param jsonString The JSON payload you want to store
     * @param status     A short tag like "SENT" or "ERROR"
     */
    public synchronized void logToUserFile(String senderName, String jsonString, String status) {
        File userLogFile = new File(logsFolder, senderName + ".log");
        String timestamp = ISO_TS.format(Instant.now());
        String line = String.format("[%s] %s → %s%n", timestamp, jsonString, status);

        try (FileWriter writer = new FileWriter(userLogFile, true)) {
            writer.write(line);
        } catch (IOException e) {
            consoleLogger.log(
                Level.WARNING,
                "Chathook: Could not write to user log file for " + senderName,
                e
            );
        }
    }

    /**
     * Log an error message (and stack trace if debugMode=true) to BOTH:
     *  1) Console (via plugin.getLogger()), and 
     *  2) A rolling "error.log" in logsFolder.
     *
     * This method is synchronized to avoid race conditions during rotation.
     *
     * @param message   A human‐readable error description.
     * @param ex        The Exception that triggered this error (may be null).
     * @param debugMode If true, include full stack trace in both console and file.
     */
    public synchronized void logError(String message, Exception ex, boolean debugMode) {
        // 1) Console logging
        if (debugMode && ex != null) {
            consoleLogger.log(Level.SEVERE, message, ex);
        } else {
            consoleLogger.log(Level.SEVERE, message);
        }

        // 2) File logging (with rollover)
        try {
            rotateIfNeeded(); // may rename old files

            File errorFile = new File(logsFolder, ERROR_LOG_BASE);
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(ISO_TS.format(Instant.now())).append("] ").append(message).append("\n");
            if (debugMode && ex != null) {
                sb.append("StackTrace:\n");
                for (StackTraceElement el : ex.getStackTrace()) {
                    sb.append("    ").append(el.toString()).append("\n");
                }
            }
            Files.writeString(
                errorFile.toPath(),
                sb.toString(),
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            consoleLogger.log(Level.WARNING, "Chathook: Failed to write to error.log", e);
        }
    }

    /**
     * If "error.log" ≥ maxBytes, rotate:
     *   - error.(fileCount-1).log → delete
     *   - error.(i).log → rename to error.(i+1).log (for i = fileCount-2 down to 1)
     *   - error.log → error.1.log
     * 
     * If error.log does not exist or is smaller than maxBytes, do nothing.
     */
    private void rotateIfNeeded() throws IOException {
        File errorFile = new File(logsFolder, ERROR_LOG_BASE);
        if (!errorFile.exists()) {
            return; // nothing to rotate
        }
        if (errorFile.length() < maxBytes) {
            return; // still under size limit
        }

        // Delete the oldest (error.<fileCount-1>.log) if it exists
        File oldest = new File(logsFolder, String.format("error.%d.log", fileCount - 1));
        if (oldest.exists()) {
            oldest.delete();
        }

        // Shift down: error.(i).log → error.(i+1).log
        for (int i = fileCount - 2; i >= 1; i--) {
            File src = new File(logsFolder, String.format("error.%d.log", i));
            if (!src.exists()) continue;
            File dst = new File(logsFolder, String.format("error.%d.log", i + 1));
            Files.move(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        // Finally, rename "error.log" → "error.1.log"
        File rotated = new File(logsFolder, "error.1.log");
        Files.move(errorFile.toPath(), rotated.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Delete every file under logsFolder.
     */
    public synchronized void purgeAllLogs() {
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
     * @param username The base name of the user’s log (without “.log”).
     * @return true if deletion succeeded, false if the file was missing or couldn’t be deleted.
     */
    public synchronized boolean purgeUserLog(String username) {
        File userLog = new File(logsFolder, username + ".log");
        if (userLog.exists()) {
            return userLog.delete();
        }
        return false;
    }
}
