package com.minimart.server.log;

import com.minimart.common.model.SystemLog;
import com.minimart.server.dao.LogDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Structured logger that writes events to both the SLF4J/file log
 * and the {@code system_logs} database table for in-app log viewing.
 *
 * <p>All methods are static for convenient use throughout the server.</p>
 */
public final class StructuredLogger {

    private static final Logger slf4j  = LoggerFactory.getLogger("AUDIT");
    private static final LogDAO logDAO = new LogDAO();

    private StructuredLogger() { /* utility class */ }

    public static void info(String action, String username, String message, String ip) {
        write("INFO", action, username, message, ip);
    }

    public static void warn(String action, String username, String message, String ip) {
        write("WARN", action, username, message, ip);
    }

    public static void error(String action, String username, String message, String ip) {
        write("ERROR", action, username, message, ip);
    }

    private static void write(String level, String action, String username, String message, String ip) {
        // Write to SLF4J (file appender configured in logback.xml)
        String formatted = String.format("[%s] action=%s user=%s ip=%s msg=%s", level, action, username, ip, message);
        switch (level) {
            case "INFO"  -> slf4j.info(formatted);
            case "WARN"  -> slf4j.warn(formatted);
            case "ERROR" -> slf4j.error(formatted);
        }

        // Write to DB (non-blocking; LogDAO catches its own exceptions)
        SystemLog entry = new SystemLog(level, action, username, message, ip);
        logDAO.insert(entry);
    }
}
