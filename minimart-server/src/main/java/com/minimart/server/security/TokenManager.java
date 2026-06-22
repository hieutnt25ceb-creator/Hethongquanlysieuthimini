package com.minimart.server.security;

import com.minimart.common.model.User;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory session token manager.
 *
 * <p>On successful login, the server generates a UUID token and stores the associated
 * User object. Every subsequent authenticated request supplies this token for validation.</p>
 *
 * <p>Tokens are invalidated on LOGOUT or server restart.</p>
 * <p>For production, replace with a persistent or JWT-based approach.</p>
 */
public final class TokenManager {

    /** Thread-safe map: token → authenticated User */
    private static final Map<String, User> TOKEN_STORE = new ConcurrentHashMap<>();

    private TokenManager() { /* utility class */ }

    /**
     * Generates a new UUID session token and stores the user.
     *
     * @param user the authenticated user
     * @return the generated token string
     */
    public static String generateToken(User user) {
        String token = UUID.randomUUID().toString();
        TOKEN_STORE.put(token, user);
        return token;
    }

    /**
     * Retrieves the User associated with a token.
     *
     * @param token the session token from the Request
     * @return the User, or {@code null} if the token is invalid/expired
     */
    public static User getUser(String token) {
        if (token == null) return null;
        return TOKEN_STORE.get(token);
    }

    /**
     * Validates whether a token is active.
     *
     * @param token the session token
     * @return {@code true} if token is valid
     */
    public static boolean isValid(String token) {
        return token != null && TOKEN_STORE.containsKey(token);
    }

    /**
     * Invalidates a session token (logout).
     *
     * @param token the session token to remove
     */
    public static void invalidate(String token) {
        if (token != null) TOKEN_STORE.remove(token);
    }

    /**
     * Returns the number of active sessions (for monitoring).
     */
    public static int activeSessionCount() {
        return TOKEN_STORE.size();
    }
}
