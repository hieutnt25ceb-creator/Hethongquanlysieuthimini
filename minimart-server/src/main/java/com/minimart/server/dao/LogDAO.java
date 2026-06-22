package com.minimart.server.dao;

import com.minimart.common.model.SystemLog;
import com.minimart.server.config.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC DAO for the {@code system_logs} table.
 */
public class LogDAO {

    private static final Logger log = LoggerFactory.getLogger(LogDAO.class);

    private static final String SQL_INSERT =
            "INSERT INTO system_logs (level, action, username, message, ip_address) VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_FIND_ALL =
            "SELECT id, log_time, level, action, username, message, ip_address " +
            "FROM system_logs ORDER BY log_time DESC LIMIT 500";

    private static final String SQL_FIND_BY_LEVEL =
            "SELECT id, log_time, level, action, username, message, ip_address " +
            "FROM system_logs WHERE level = ? ORDER BY log_time DESC LIMIT 200";

    // ── Public DAO methods ──────────────────────────────────

    /**
     * Inserts a structured log entry.
     */
    public void insert(SystemLog entry) {
        // Logging should NEVER crash the main flow — catch all exceptions
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
            ps.setString(1, entry.getLevel());
            ps.setString(2, entry.getAction());
            ps.setString(3, entry.getUsername());
            ps.setString(4, entry.getMessage());
            ps.setString(5, entry.getIpAddress());
            ps.executeUpdate();
        } catch (Exception e) {
            // Fallback: write to SLF4J so the event is not completely lost
            log.error("Failed to write system_log to DB: {}", entry, e);
        }
    }

    /**
     * Returns the most recent 500 log entries (newest first).
     */
    public List<SystemLog> findAll() throws SQLException {
        return queryLogs(SQL_FIND_ALL, null);
    }

    /**
     * Returns up to 200 log entries filtered by level ("INFO", "WARN", "ERROR").
     */
    public List<SystemLog> findByLevel(String level) throws SQLException {
        return queryLogs(SQL_FIND_BY_LEVEL, level);
    }

    // ── Private helpers ─────────────────────────────────────

    private List<SystemLog> queryLogs(String sql, String param) throws SQLException {
        List<SystemLog> list = new ArrayList<>();
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (param != null) ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    private SystemLog mapRow(ResultSet rs) throws SQLException {
        SystemLog entry = new SystemLog();
        entry.setId(rs.getInt("id"));
        Timestamp logTime = rs.getTimestamp("log_time");
        if (logTime != null) entry.setLogTime(logTime.toLocalDateTime());
        entry.setLevel(rs.getString("level"));
        entry.setAction(rs.getString("action"));
        entry.setUsername(rs.getString("username"));
        entry.setMessage(rs.getString("message"));
        entry.setIpAddress(rs.getString("ip_address"));
        return entry;
    }
}
