package com.minimart.server.security;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES-128-CBC encryption/decryption utility for sensitive data (e.g., phone numbers).
 *
 * <p><b>Security note:</b> The secret key and IV should be loaded from environment
 * variables or a secure vault in production — never hardcoded. The values here are
 * defaults for development/demo only.</p>
 *
 * <p>Encrypted values are stored as Base64 strings prefixed with "ENC:" to distinguish
 * them from plaintext values in the database.</p>
 */
public final class AESUtil {

    /** 16-byte AES-128 key. CHANGE THIS in production via env var AES_SECRET_KEY */
    private static final String SECRET_KEY = System.getProperty("AES_SECRET_KEY", "MiniMart@Key1234");

    /** 16-byte Initialization Vector. CHANGE THIS in production via env var AES_IV */
    private static final String INIT_VECTOR = System.getProperty("AES_IV", "MiniMart@IV56789");

    private static final String ALGORITHM   = "AES/CBC/PKCS5PADDING";
    private static final String ENC_PREFIX  = "ENC:";

    private AESUtil() { /* utility class */ }

    /**
     * Encrypts a plain-text string using AES-128-CBC and returns a Base64-encoded result
     * prefixed with "ENC:".
     *
     * @param plainText the value to encrypt
     * @return "ENC:" + Base64(encrypted bytes)
     */
    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) return plainText;
        try {
            IvParameterSpec iv         = new IvParameterSpec(INIT_VECTOR.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec   skeySpec   = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher          cipher     = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return ENC_PREFIX + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("AES encryption failed", e);
        }
    }

    /**
     * Decrypts an "ENC:"-prefixed Base64-encoded AES-128-CBC string.
     *
     * @param encryptedText the "ENC:..." string from the database
     * @return the original plain-text value
     */
    public static String decrypt(String encryptedText) {
        if (encryptedText == null || !encryptedText.startsWith(ENC_PREFIX)) {
            return encryptedText; // Not encrypted — return as-is
        }
        try {
            String base64Part   = encryptedText.substring(ENC_PREFIX.length());
            IvParameterSpec iv  = new IvParameterSpec(INIT_VECTOR.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec skeySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher        cipher   = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(base64Part));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES decryption failed", e);
        }
    }

    /**
     * Returns {@code true} if the value is already AES-encrypted.
     */
    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith(ENC_PREFIX);
    }
}
