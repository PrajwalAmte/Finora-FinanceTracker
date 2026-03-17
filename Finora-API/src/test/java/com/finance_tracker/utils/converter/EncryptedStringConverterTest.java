package com.finance_tracker.utils.converter;

import com.finance_tracker.service.FieldEncryptionService;
import com.finance_tracker.utils.security.VaultKeyContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EncryptedStringConverterTest {

    @Mock
    private FieldEncryptionService encryptionService;

    @InjectMocks
    private EncryptedStringConverter converter;

    @AfterEach
    void clearVaultKey() {
        VaultKeyContext.clear();
    }

    @Test
    void convertToDatabaseColumn_nullReturnsNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void convertToDatabaseColumn_encryptsWithVaultKey() {
        VaultKeyContext.set("my-vault-key");
        when(encryptionService.encrypt("sensitive", "my-vault-key")).thenReturn("encrypted");

        String result = converter.convertToDatabaseColumn("sensitive");

        assertThat(result).isEqualTo("encrypted");
        verify(encryptionService).encrypt("sensitive", "my-vault-key");
    }

    @Test
    void convertToEntityAttribute_nullReturnsNull() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void convertToEntityAttribute_decryptsWithVaultKey() {
        VaultKeyContext.set("my-vault-key");
        when(encryptionService.decrypt("encrypted", "my-vault-key")).thenReturn("plain");

        String result = converter.convertToEntityAttribute("encrypted");

        assertThat(result).isEqualTo("plain");
        verify(encryptionService).decrypt("encrypted", "my-vault-key");
    }
}
