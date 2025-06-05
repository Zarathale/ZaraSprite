package com.playtheatria.chathook.utils;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class FileLogger {
    // The single Logger instance for the entire plugin
    private static Logger logger;

    /**
     * Call this once on plugin startup to set up the rolling log.
     * @param dataFolder The plugin’s data folder (i.e., plugins/Chathook)
     */
    public static void initialize(File dataFolder) {
        try {
            // Ensure "logs" directory exists
            File logsDir = new File(dataFolder, "logs");
            if (!logsDir.exists()) {
                logsDir.mkdirs();
            }

            // Create or retrieve a Logger named "Chathook"
            logger = Logger.getLogger("Chathook");
            logger.setLevel(Level.INFO);

            // Pattern: plugins/Chathook/logs/chathook-%g.log
            String pattern = new File(logsDir, "chathook-%g.log").getAbsolutePath();
            // Rotate at 10 MB per file, keep up to 5 files
            int limit = 10_000_000;  // 10 MB
            int fileCount = 5;
            boolean append = true;

            FileHandler handler = new FileHandler(pattern, limit, fileCount, append);
            handler.setFormatter(new SimpleFormatter());
            logger.addHandler(handler);

        } catch (IOException e) {
            // If the log folder cannot be created or the FileHandler fails, log to console
            e.printStackTrace();
        }
    }

    /** Convenience method for INFO‐level logging. */
    public static void logInfo(String message) {
        if (logger != null) {
            logger.info(message);
        }
    }

    /** Convenience method for WARNING‐level logging. */
    public static void logWarning(String message) {
        if (logger != null) {
            logger.warning(message);
        }
    }

    /** Convenience method for SEVERE‐level logging. */
    public static void logError(String message, Throwable t) {
        if (logger != null) {
            logger.log(Level.SEVERE, message, t);
        }
    }
}
