package com.finance_tracker.service;

import com.finance_tracker.exception.BackupException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CryptoServiceTest {

    private final CryptoService cryptoService = new CryptoService();

    @Test
    void encryptThenDecrypt_roundTrip_returnsOriginalBytes() {
        byte[] original = "Hello Finora backup data 🔒".getBytes();
        String password = "StrongPassword123!";

        byte[] encrypted = cryptoService.encrypt(original, password);
        byte[] decrypted = cryptoService.decrypt(encrypted, password);

        assertThat(decrypted).isEqualTo(original);
    }

    @Test
    void encrypt_producesDifferentCiphertextEachTime() {
        byte[] data = "same data".getBytes();
        byte[] enc1 = cryptoService.encrypt(data, "pass");
        byte[] enc2 = cryptoService.encrypt(data, "pass");

        assertThat(enc1).isNotEqualTo(enc2); // random salt + IV
    }

    @Test
    void decrypt_tooShortData_throwsBackupException() {
        byte[] tooShort = new byte[5];
        assertThatThrownBy(() -> cryptoService.decrypt(tooShort, "pass"))
                .isInstanceOf(BackupException.class)
                .hasMessageContaining("too short");
    }

    @Test
    void decrypt_wrongPassword_throwsBackupException() {
        byte[] encrypted = cryptoService.encrypt("secret".getBytes(), "correct-password");
        assertThatThrownBy(() -> cryptoService.decrypt(encrypted, "wrong-password"))
                .isInstanceOf(BackupException.class)
                .hasMessageContaining("incorrect password");
    }

    @Test
    void encrypt_emptyPlaintext_succeeds() {
        byte[] empty = new byte[0];
        byte[] encrypted = cryptoService.encrypt(empty, "password");
        byte[] decrypted = cryptoService.decrypt(encrypted, "password");
        assertThat(decrypted).isEmpty();
    }
}
