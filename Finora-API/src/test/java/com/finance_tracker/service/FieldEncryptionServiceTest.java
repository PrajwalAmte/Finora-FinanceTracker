package com.finance_tracker.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FieldEncryptionServiceTest {

    private static final String VALID_KEY = "a".repeat(32);

    private FieldEncryptionService service(String key) {
        return new FieldEncryptionService(key);
    }

    // ── isEnabled ─────────────────────────────────────────────────────────────

    @Test
    void isEnabled_withKey_returnsTrue() {
        assertThat(service(VALID_KEY).isEnabled()).isTrue();
    }

    @Test
    void isEnabled_blankKey_returnsFalse() {
        assertThat(service("").isEnabled()).isFalse();
    }

    @Test
    void isEnabled_nullKey_returnsFalse() {
        assertThat(service(null).isEnabled()).isFalse();
    }

    // ── isEncrypted ───────────────────────────────────────────────────────────

    @Test
    void isEncrypted_v1Prefix_returnsTrue() {
        assertThat(service(VALID_KEY).isEncrypted("v1:somedata")).isTrue();
    }

    @Test
    void isEncrypted_v2Prefix_returnsTrue() {
        assertThat(service(VALID_KEY).isEncrypted("v2:somedata")).isTrue();
    }

    @Test
    void isEncrypted_plaintext_returnsFalse() {
        assertThat(service(VALID_KEY).isEncrypted("plaintext")).isFalse();
    }

    @Test
    void isEncrypted_null_returnsFalse() {
        assertThat(service(VALID_KEY).isEncrypted(null)).isFalse();
    }

    // ── encrypt / decrypt ─────────────────────────────────────────────────────

    @Test
    void encrypt_disabled_returnsPlaintext() {
        assertThat(service("").encrypt("hello")).isEqualTo("hello");
    }

    @Test
    void encrypt_nullInput_returnsNull() {
        assertThat(service(VALID_KEY).encrypt(null)).isNull();
    }

    @Test
    void encrypt_emptyInput_returnsEmpty() {
        assertThat(service(VALID_KEY).encrypt("")).isEmpty();
    }

    @Test
    void encrypt_alreadyEncrypted_returnsAsIs() {
        String ciphertext = service(VALID_KEY).encrypt("hello");
        String reEncrypted = service(VALID_KEY).encrypt(ciphertext);
        assertThat(reEncrypted).isEqualTo(ciphertext);
    }

    @Test
    void encrypt_thenDecrypt_roundtrip() {
        FieldEncryptionService svc = service(VALID_KEY);
        String ciphertext = svc.encrypt("secret-value");

        assertThat(ciphertext).startsWith("v1:");
        assertThat(svc.decrypt(ciphertext)).isEqualTo("secret-value");
    }

    @Test
    void decrypt_disabled_returnsAsIs() {
        assertThat(service("").decrypt("v1:somedata")).isEqualTo("v1:somedata");
    }

    @Test
    void decrypt_nullInput_returnsNull() {
        assertThat(service(VALID_KEY).decrypt(null)).isNull();
    }

    @Test
    void decrypt_emptyInput_returnsEmpty() {
        assertThat(service(VALID_KEY).decrypt("")).isEmpty();
    }

    @Test
    void decrypt_notEncrypted_returnsAsIs() {
        assertThat(service(VALID_KEY).decrypt("plaintext")).isEqualTo("plaintext");
    }

    // ── encrypt with vault key ────────────────────────────────────────────────

    @Test
    void encrypt_withVaultKey_producesV2Ciphertext() {
        FieldEncryptionService svc = service(VALID_KEY);
        String result = svc.encrypt("vault-secret", "vault-key-32chars-padding-here!");

        assertThat(result).startsWith("v2:");
    }

    @Test
    void encrypt_withVaultKey_thenDecrypt_roundtrip() {
        FieldEncryptionService svc = service(VALID_KEY);
        String vaultKey = "vault-key-32chars-padding-here!";
        String ciphertext = svc.encrypt("vault-secret", vaultKey);

        assertThat(svc.decrypt(ciphertext, vaultKey)).isEqualTo("vault-secret");
    }

    @Test
    void decrypt_v2WithoutVaultKey_throws() {
        FieldEncryptionService svc = service(VALID_KEY);
        String ciphertext = svc.encrypt("test", "vault-key-32chars-padding-here!");

        assertThatThrownBy(() -> svc.decrypt(ciphertext, null))
                .isInstanceOf(IllegalStateException.class);
    }

    // ── encryptDeterministic ──────────────────────────────────────────────────

    @Test
    void encryptDeterministic_samePlaintext_producesSameCiphertext() {
        FieldEncryptionService svc = service(VALID_KEY);
        String c1 = svc.encryptDeterministic("hello");
        String c2 = svc.encryptDeterministic("hello");

        assertThat(c1).isEqualTo(c2);
    }

    @Test
    void encryptDeterministic_differentPlaintext_producesDifferentCiphertext() {
        FieldEncryptionService svc = service(VALID_KEY);
        String c1 = svc.encryptDeterministic("hello");
        String c2 = svc.encryptDeterministic("world");

        assertThat(c1).isNotEqualTo(c2);
    }

    @Test
    void encryptDeterministic_disabled_returnsPlaintext() {
        assertThat(service("").encryptDeterministic("hello")).isEqualTo("hello");
    }

    @Test
    void encryptDeterministic_null_returnsNull() {
        assertThat(service(VALID_KEY).encryptDeterministic(null)).isNull();
    }

    @Test
    void encryptDeterministic_alreadyEncrypted_returnsAsIs() {
        FieldEncryptionService svc = service(VALID_KEY);
        String c1 = svc.encryptDeterministic("hello");
        assertThat(svc.encryptDeterministic(c1)).isEqualTo(c1);
    }

    // ── generateSalt ─────────────────────────────────────────────────────────

    @Test
    void generateSalt_returns16Bytes() {
        byte[] salt = service(VALID_KEY).generateSalt();
        assertThat(salt).hasSize(16);
    }

    @Test
    void generateSalt_eachCallProducesDifferentSalt() {
        FieldEncryptionService svc = service(VALID_KEY);
        byte[] salt1 = svc.generateSalt();
        byte[] salt2 = svc.generateSalt();

        assertThat(salt1).isNotEqualTo(salt2);
    }

    // ── deriveKey ─────────────────────────────────────────────────────────────

    @Test
    void deriveKey_returns32Bytes() {
        byte[] salt = service(VALID_KEY).generateSalt();
        byte[] key = service(VALID_KEY).deriveKey("passphrase", salt);

        assertThat(key).hasSize(32);
    }

    @Test
    void deriveKey_sameSaltAndPassphrase_producesSameKey() {
        FieldEncryptionService svc = service(VALID_KEY);
        byte[] salt = svc.generateSalt();
        byte[] k1 = svc.deriveKey("my-pass", salt);
        byte[] k2 = svc.deriveKey("my-pass", salt);

        assertThat(k1).isEqualTo(k2);
    }

    @Test
    void deriveKey_differentPassphrase_producesDifferentKey() {
        FieldEncryptionService svc = service(VALID_KEY);
        byte[] salt = svc.generateSalt();
        byte[] k1 = svc.deriveKey("pass-A", salt);
        byte[] k2 = svc.deriveKey("pass-B", salt);

        assertThat(k1).isNotEqualTo(k2);
    }
}
