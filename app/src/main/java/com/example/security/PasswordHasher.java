package com.example.security;

import android.util.Base64;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Cryptographic utility providing PBKDF2-HMAC-SHA256 password hashing.
 * Uses cryptographically secure random salt, configurable iteration count,
 * and constant-time equality comparison to protect against timing attacks.
 */
public final class PasswordHasher {

    // Documented work factor appropriate for Android mobile execution
    public static final int DEFAULT_ITERATIONS = 15000;
    public static final int KEY_LENGTH_BITS = 256;
    public static final int SALT_BYTES = 16;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    private final SecureRandom secureRandom;

    public PasswordHasher() {
        this.secureRandom = new SecureRandom();
    }

    /**
     * Container for hash output along with salt and work factor.
     */
    public static class HashResult {
        private final String hashBase64;
        private final String saltBase64;
        private final int iterations;

        public HashResult(String hashBase64, String saltBase64, int iterations) {
            this.hashBase64 = hashBase64;
            this.saltBase64 = saltBase64;
            this.iterations = iterations;
        }

        public String getHashBase64() {
            return hashBase64;
        }

        public String getSaltBase64() {
            return saltBase64;
        }

        public int getIterations() {
            return iterations;
        }
    }

    /**
     * Hashes a plaintext password using a newly generated secure salt and standard work factor.
     */
    public HashResult hashPassword(char[] password) {
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        return hashWithSalt(password, salt, DEFAULT_ITERATIONS);
    }

    /**
     * Hashes a plaintext password using an existing salt and specified iteration count.
     */
    public HashResult hashWithSalt(char[] password, byte[] salt, int iterations) {
        try {
            KeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();
            String hashBase64 = Base64.encodeToString(hash, Base64.NO_WRAP);
            String saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP);
            return new HashResult(hashBase64, saltBase64, iterations);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error calculating PBKDF2 password hash", e);
        }
    }

    /**
     * Verifies a candidate password against a stored base64 hash, salt, and iteration count.
     * Uses constant-time MessageDigest.isEqual to prevent timing attacks.
     */
    public boolean verifyPassword(char[] candidatePassword, String storedHashBase64, String storedSaltBase64, int iterations) {
        if (candidatePassword == null || storedHashBase64 == null || storedSaltBase64 == null) {
            return false;
        }
        try {
            byte[] salt = Base64.decode(storedSaltBase64, Base64.NO_WRAP);
            byte[] expectedHash = Base64.decode(storedHashBase64, Base64.NO_WRAP);

            KeySpec spec = new PBEKeySpec(candidatePassword, salt, iterations, KEY_LENGTH_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] computedHash = factory.generateSecret(spec).getEncoded();

            return MessageDigest.isEqual(expectedHash, computedHash);
        } catch (Exception e) {
            return false;
        }
    }
}
