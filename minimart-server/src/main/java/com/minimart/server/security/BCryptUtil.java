package com.minimart.server.security;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utility class for BCrypt password hashing and verification.
 *
 * <p>Uses a cost factor of 12 (2^12 = 4096 iterations), balancing security
 * and performance. Increasing the cost by 1 roughly doubles the computation time.</p>
 */
public final class BCryptUtil {

    /** BCrypt cost factor — higher = more secure but slower */
    private static final int BCRYPT_COST = 12;

    private BCryptUtil() { /* utility class */ }

    /**
     * Hashes a plain-text password using BCrypt.
     *
     * @param plainPassword the raw password to hash
     * @return the BCrypt hash string (60 chars)
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password must not be null or empty");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_COST));
    }

    /**
     * Verifies a plain-text password against a stored BCrypt hash.
     *
     * @param plainPassword  the user-supplied raw password
     * @param hashedPassword the stored BCrypt hash
     * @return {@code true} if the password matches the hash
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) return false;
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (Exception e) {
            // Invalid hash format
            return false;
        }
    }
}
