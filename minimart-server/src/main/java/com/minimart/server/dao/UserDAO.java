package com.minimart.server.dao;

import com.minimart.common.model.User;
import com.minimart.server.config.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC DAO for the {@code users} table.
 * All SQL uses PreparedStatements to prevent SQL injection.
 */
public class UserDAO {

    private static final Logger log = LoggerFactory.getLogger(UserDAO.class);

    // ── SQL Constants ───────────────────────────────────────

    private static final String SQL_FIND_ALL =
            "SELECT id, username, password, full_name, role, is_active, created_at, updated_at FROM users ORDER BY id";

    private static final String SQL_FIND_BY_ID =
            "SELECT id, username, password, full_name, role, is_active, created_at, updated_at FROM users WHERE id = ?";

    private static final String SQL_FIND_BY_USERNAME =
            "SELECT id, username, password, full_name, role, is_active, created_at, updated_at FROM users WHERE username = ?";

    private static final String SQL_INSERT =
            "INSERT INTO users (username, password, full_name, role, is_active) VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE users SET username = ?, full_name = ?, role = ?, is_active = ? WHERE id = ?";

    private static final String SQL_UPDATE_PASSWORD =
            "UPDATE users SET password = ? WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM users WHERE id = ?";

    // ── Public DAO methods ──────────────────────────────────

    /**
     * Retrieves all users from the database.
     */
    public List<User> findAll() throws SQLException {
        List<User> users = new ArrayList<>();
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(mapRow(rs));
            }
        }
        log.debug("findAll: {} users retrieved", users.size());
        return users;
    }

    /**
     * Finds a user by primary key.
     */
    public Optional<User> findById(int id) throws SQLException {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    /**
     * Finds a user by username (used for authentication).
     */
    public Optional<User> findByUsername(String username) throws SQLException {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_USERNAME)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    /**
     * Inserts a new user and returns the generated ID.
     *
     * @param user user to insert (password must already be BCrypt-hashed)
     * @return the auto-generated primary key
     */
    public int insert(User user) throws SQLException {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());   // BCrypt hash
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getRole());
            ps.setBoolean(5, user.isActive());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int newId = keys.getInt(1);
                    log.info("User inserted: id={}, username={}", newId, user.getUsername());
                    return newId;
                }
            }
        }
        throw new SQLException("Insert user failed — no generated key");
    }

    /**
     * Updates a user's profile (does NOT update the password).
     */
    public boolean update(User user) throws SQLException {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getFullName());
            ps.setString(3, user.getRole());
            ps.setBoolean(4, user.isActive());
            ps.setInt(5, user.getId());
            int rows = ps.executeUpdate();
            log.info("User updated: id={}, rows={}", user.getId(), rows);
            return rows > 0;
        }
    }

    /**
     * Updates a user's password (already hashed with BCrypt).
     */
    public boolean updatePassword(int userId, String hashedPassword) throws SQLException {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_PASSWORD)) {
            ps.setString(1, hashedPassword);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Deletes a user by ID.
     */
    public boolean delete(int id) throws SQLException {
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            log.info("User deleted: id={}, rows={}", id, rows);
            return rows > 0;
        }
    }

    // ── Private mapping ─────────────────────────────────────

    /** Maps a ResultSet row to a User object. */
    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setFullName(rs.getString("full_name"));
        user.setRole(rs.getString("role"));
        user.setActive(rs.getBoolean("is_active"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (createdAt != null) user.setCreatedAt(createdAt.toLocalDateTime());
        if (updatedAt != null) user.setUpdatedAt(updatedAt.toLocalDateTime());
        return user;
    }
}
