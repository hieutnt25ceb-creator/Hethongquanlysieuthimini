package com.minimart.server.service;

import com.minimart.common.dto.LoginResponse;
import com.minimart.common.model.User;
import com.minimart.server.dao.UserDAO;
import com.minimart.server.security.BCryptUtil;
import com.minimart.server.security.TokenManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Business service for authentication.
 * Validates credentials with BCrypt and issues session tokens.
 */
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserDAO userDAO = new UserDAO();

    /**
     * Attempts to authenticate a user.
     *
     * @param username the submitted username
     * @param password the submitted plain-text password
     * @return a {@link LoginResponse} with token + user profile on success
     * @throws SecurityException if credentials are invalid or user is inactive
     * @throws SQLException      if a database error occurs
     */
    public LoginResponse login(String username, String password) throws SQLException {
        Optional<User> opt = userDAO.findByUsername(username);

        if (opt.isEmpty()) {
            log.warn("Login failed: username '{}' not found", username);
            throw new SecurityException("Invalid username or password");
        }

        User user = opt.get();

        if (!user.isActive()) {
            log.warn("Login failed: account '{}' is disabled", username);
            throw new SecurityException("Account is disabled. Contact administrator.");
        }

        if (!BCryptUtil.verifyPassword(password, user.getPassword())) {
            log.warn("Login failed: wrong password for '{}'", username);
            throw new SecurityException("Invalid username or password");
        }

        // Clear the password hash before sending user data to client
        user.setPassword(null);

        String token = TokenManager.generateToken(user);
        log.info("Login successful: username='{}', role='{}'", username, user.getRole());
        return new LoginResponse(token, user);
    }

    /**
     * Invalidates a user's session (logout).
     */
    public void logout(String token) {
        User user = TokenManager.getUser(token);
        TokenManager.invalidate(token);
        log.info("User logged out: {}", user != null ? user.getUsername() : "unknown");
    }
}
