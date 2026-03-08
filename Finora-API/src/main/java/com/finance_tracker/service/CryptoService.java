package com.finance_tracker.service;

import com.finance_tracker.exception.BackupException;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

/**
 * Handles AES-256-GCM encryption and decryption with PBKDF2 key derivation.
 *
 * <p>Binary format of the encrypted output:
 * <pre>
 * [salt (16 bytes)] [IV (12 bytes)] [ciphertext + GCM auth tag]
 * </pre>
 *
 * <p>Security properties:
 * <ul>
 *   <li>AES-256-GCM provides authenticated encryption (confidentiality + integrity)</li>
 *   <li>PBKDF2-HMAC-SHA256 with 310,000 iterations for key derivation (OWASP 2023 recommendation)</li>
 *   <li>Random 16-byte salt per encryption prevents rainbow-table attacks</li>
 *   <li>Random 12-byte IV per encryption ensures unique ciphertexts for identical plaintexts</li>
 *   <li>GCM 128-bit authentication tag detects any tampering</li>
 * </ul>
 */
@Service
public class CryptoService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final String KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int PBKDF2_ITERATIONS = 310_000; // OWASP 2023 recommendation

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Encrypts plaintext bytes using AES-256-GCM with a password-derived key.
     *
     * @param plaintext the data to encrypt
     * @param password  the user-supplied password for key derivation
     * @return encrypted bytes in the format: salt + IV + ciphertext
     * @throws BackupException if encryption fails
     */
    public byte[] encrypt(byte[] plaintext, String password) {
        try {
            // Generate random salt and IV
            byte[] salt = new byte[SALT_LENGTH_BYTES];
            secureRandom.nextBytes(salt);

            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            // Derive AES key from password
            SecretKey key = deriveKey(password, salt);

            // Encrypt with AES-GCM
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);
            byte[] ciphertext = cipher.doFinal(plaintext);

            // Pack: salt + IV + ciphertext (ciphertext includes GCM auth tag)
            ByteBuffer output = ByteBuffer.allocate(SALT_LENGTH_BYTES + IV_LENGTH_BYTES + ciphertext.length);
            output.put(salt);
            output.put(iv);
            output.put(ciphertext);

            return output.array();
        } catch (Exception e) {
            throw new BackupException("Encryption failed", e);
        }
    }

    /**
     * Decrypts data that was encrypted by {@link #encrypt(byte[], String)}.
     *
     * @param encryptedData the encrypted bytes (salt + IV + ciphertext)
     * @param password      the same password used during encryption
     * @return the original plaintext bytes
     * @throws BackupException if decryption fails (wrong password or corrupted data)
     */
    public byte[] decrypt(byte[] encryptedData, String password) {
        try {
            if (encryptedData.length < SALT_LENGTH_BYTES + IV_LENGTH_BYTES + GCM_TAG_LENGTH_BITS / 8) {
                throw new BackupException("Encrypted data is too short — file may be corrupted");
            }

            ByteBuffer buffer = ByteBuffer.wrap(encryptedData);

            // Extract salt
            byte[] salt = new byte[SALT_LENGTH_BYTES];
            buffer.get(salt);

            // Extract IV
            byte[] iv = new byte[IV_LENGTH_BYTES];
            buffer.get(iv);

            // Extract ciphertext (remainder)
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            // Derive same key
            SecretKey key = deriveKey(password, salt);

            // Decrypt with AES-GCM
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);

            return cipher.doFinal(ciphertext);
        } catch (BackupException e) {
            throw e;
        } catch (javax.crypto.AEADBadTagException e) {
            throw new BackupException("Decryption failed — incorrect password or corrupted backup file", e);
        } catch (Exception e) {
            throw new BackupException("Decryption failed", e);
        }
    }

    /**
     * Derives a 256-bit AES key from the given password and salt using PBKDF2-HMAC-SHA256.
     */
    private SecretKey deriveKey(String password, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }
}
