package com.finance_tracker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class FieldEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int PBKDF2_ITERATIONS = 310_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final String KEY_VERSION_PREFIX = "v1:";

    private final SecureRandom secureRandom = new SecureRandom();
    private final String serverKey;

    public FieldEncryptionService(@Value("${field.encryption.key:}") String serverKey) {
        this.serverKey = serverKey;
    }

    /**
     * @return true if a server encryption key is configured (non-blank).
     */
    public boolean isEnabled() {
        return serverKey != null && !serverKey.isBlank();
    }

    /**
     * Encrypt plaintext using probabilistic mode (random IV).
     * Returns null/empty unchanged.
     */
    public String encrypt(String plaintext) {
        return encrypt(plaintext, null);
    }

    public String encrypt(String plaintext, String vaultKey) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        if (!isEnabled()) {
            return plaintext;
        }
        if (isEncrypted(plaintext)) {
            return plaintext;
        }

        try {
            String serverEncrypted = doEncrypt(plaintext, serverKey, false);
            if (vaultKey != null && !vaultKey.isBlank()) {
                String vaultEncrypted = doEncrypt(serverEncrypted, vaultKey, false);
                return "v2:" + vaultEncrypted.substring(KEY_VERSION_PREFIX.length());
            }
            return serverEncrypted;
        } catch (Exception e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    public String encryptDeterministic(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        if (!isEnabled()) {
            return plaintext;
        }
        if (isEncrypted(plaintext)) {
            return plaintext;
        }

        try {
            return doEncrypt(plaintext, serverKey, true);
        } catch (Exception e) {
            throw new IllegalStateException("Deterministic encryption failed", e);
        }
    }

    public String decrypt(String ciphertext) {
        return decrypt(ciphertext, null);
    }

    public String decrypt(String ciphertext, String vaultKey) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return ciphertext;
        }
        if (!isEnabled()) {
            return ciphertext;
        }
        if (!isEncrypted(ciphertext)) {
            return ciphertext;
        }

        try {
            if (ciphertext.startsWith("v2:")) {
                if (vaultKey == null || vaultKey.isBlank()) {
                    throw new IllegalStateException("Vault key required to decrypt v2: ciphertext");
                }
                String vaultDecrypted = doDecrypt(KEY_VERSION_PREFIX + ciphertext.substring(3), vaultKey);
                return doDecrypt(vaultDecrypted, serverKey);
            } else {
                return doDecrypt(ciphertext, serverKey);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Decryption failed", e);
        }
    }

    public boolean isEncrypted(String value) {
        return value != null && (value.startsWith("v1:") || value.startsWith("v2:"));
    }

    public byte[] deriveKey(String passphrase, byte[] salt) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            PBEKeySpec spec = new PBEKeySpec(
                    passphrase.toCharArray(),
                    salt,
                    PBKDF2_ITERATIONS,
                    KEY_LENGTH_BITS
            );
            return factory.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Key derivation failed", e);
        }
    }

    public byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        secureRandom.nextBytes(salt);
        return salt;
    }

    private String doEncrypt(String plaintext, String keyMaterial, boolean deterministic) throws Exception {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];

        if (deterministic) {
            byte[] hash = java.security.MessageDigest.getInstance("SHA-256")
                    .digest((keyMaterial + plaintext).getBytes(StandardCharsets.UTF_8));
            System.arraycopy(hash, 0, salt, 0, SALT_LENGTH_BYTES);
            System.arraycopy(hash, SALT_LENGTH_BYTES, iv, 0, GCM_IV_LENGTH_BYTES);
        } else {
            secureRandom.nextBytes(salt);
            secureRandom.nextBytes(iv);
        }

        SecretKey key = deriveSecretKey(keyMaterial, salt);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);

        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        // Pack: salt ‖ iv ‖ ciphertext
        ByteBuffer buffer = ByteBuffer.allocate(SALT_LENGTH_BYTES + GCM_IV_LENGTH_BYTES + ciphertext.length);
        buffer.put(salt);
        buffer.put(iv);
        buffer.put(ciphertext);

        return KEY_VERSION_PREFIX + Base64.getEncoder().encodeToString(buffer.array());
    }

    private String doDecrypt(String ciphertext, String keyMaterial) throws Exception {
        if (!ciphertext.startsWith(KEY_VERSION_PREFIX)) {
            throw new IllegalArgumentException("Unknown ciphertext version");
        }

        byte[] decoded = Base64.getDecoder().decode(ciphertext.substring(KEY_VERSION_PREFIX.length()));
        ByteBuffer buffer = ByteBuffer.wrap(decoded);

        byte[] salt = new byte[SALT_LENGTH_BYTES];
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        byte[] encrypted = new byte[decoded.length - SALT_LENGTH_BYTES - GCM_IV_LENGTH_BYTES];

        buffer.get(salt);
        buffer.get(iv);
        buffer.get(encrypted);

        SecretKey key = deriveSecretKey(keyMaterial, salt);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);

        byte[] plainBytes = cipher.doFinal(encrypted);
        return new String(plainBytes, StandardCharsets.UTF_8);
    }

    private SecretKey deriveSecretKey(String keyMaterial, byte[] salt) throws Exception {
        byte[] keyBytes = deriveKey(keyMaterial, salt);
        return new SecretKeySpec(keyBytes, "AES");
    }
}
