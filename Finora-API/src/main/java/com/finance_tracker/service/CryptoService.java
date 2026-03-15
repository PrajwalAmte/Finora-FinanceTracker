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

@Service
public class CryptoService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final String KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int PBKDF2_ITERATIONS = 310_000;

    private final SecureRandom secureRandom = new SecureRandom();

    public byte[] encrypt(byte[] plaintext, String password) {
        try {
            byte[] salt = new byte[SALT_LENGTH_BYTES];
            secureRandom.nextBytes(salt);

            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            SecretKey key = deriveKey(password, salt);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);
            byte[] ciphertext = cipher.doFinal(plaintext);

            ByteBuffer output = ByteBuffer.allocate(SALT_LENGTH_BYTES + IV_LENGTH_BYTES + ciphertext.length);
            output.put(salt);
            output.put(iv);
            output.put(ciphertext);

            return output.array();
        } catch (Exception e) {
            throw new BackupException("Encryption failed", e);
        }
    }

    public byte[] decrypt(byte[] encryptedData, String password) {
        try {
            if (encryptedData.length < SALT_LENGTH_BYTES + IV_LENGTH_BYTES + GCM_TAG_LENGTH_BITS / 8) {
                throw new BackupException("Encrypted data is too short — file may be corrupted");
            }

            ByteBuffer buffer = ByteBuffer.wrap(encryptedData);

            byte[] salt = new byte[SALT_LENGTH_BYTES];
            buffer.get(salt);

            byte[] iv = new byte[IV_LENGTH_BYTES];
            buffer.get(iv);

            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            SecretKey key = deriveKey(password, salt);

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

    private SecretKey deriveKey(String password, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }
}
