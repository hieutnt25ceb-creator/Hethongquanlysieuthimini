package com.minimart.server.service;

import com.minimart.common.model.User;
import com.minimart.server.dao.UserDAO;
import com.minimart.server.security.BCryptUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Business service for User management (Admin-only operations).
 */
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserDAO userDAO = new UserDAO();

    public List<User> findAll() throws SQLException {
        List<User> users = userDAO.findAll();
        // Never send password hashes to the client
        users.forEach(u -> u.setPassword(null));
        return users;
    }

    public Optional<User> findById(int id) throws SQLException {
        Optional<User> opt = userDAO.findById(id);
        opt.ifPresent(u -> u.setPassword(null));
        return opt;
    }

    public User add(User user, String rawPassword) throws SQLException {
        validateUser(user);
        if (userDAO.findByUsername(user.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại");
        }
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        user.setPassword(BCryptUtil.hashPassword(rawPassword));
        user.setActive(true);
        int newId = userDAO.insert(user);
        user.setId(newId);
        user.setPassword(null); // Don't return hash
        return user;
    }

    public User update(User user) throws SQLException {
        validateUser(user);
        if (!userDAO.update(user)) {
            throw new SQLException("User not found: id=" + user.getId());
        }
        user.setPassword(null);
        return user;
    }

    public void delete(int id) throws SQLException {
        if (!userDAO.delete(id)) {
            throw new SQLException("User not found: id=" + id);
        }
    }

    public void changePassword(int userId, String newRawPassword) throws SQLException {
        if (newRawPassword == null || newRawPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        userDAO.updatePassword(userId, BCryptUtil.hashPassword(newRawPassword));
    }

    private void validateUser(User user) {
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (user.getFullName() == null || user.getFullName().isBlank()) {
            throw new IllegalArgumentException("Full name is required");
        }
    }
}
