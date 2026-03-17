package com.finance_tracker.utils.converter;

import com.finance_tracker.service.FieldEncryptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeterministicEncryptedStringConverterTest {

    @Mock
    private FieldEncryptionService encryptionService;

    @InjectMocks
    private DeterministicEncryptedStringConverter converter;

    @Test
    void convertToDatabaseColumn_nullReturnsNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void convertToDatabaseColumn_usesEncryptDeterministic() {
        when(encryptionService.encryptDeterministic("email@example.com")).thenReturn("det-encrypted");

        String result = converter.convertToDatabaseColumn("email@example.com");

        assertThat(result).isEqualTo("det-encrypted");
        verify(encryptionService).encryptDeterministic("email@example.com");
    }

    @Test
    void convertToEntityAttribute_nullReturnsNull() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void convertToEntityAttribute_decryptsWithNullKey() {
        when(encryptionService.decrypt("det-encrypted", null)).thenReturn("email@example.com");

        String result = converter.convertToEntityAttribute("det-encrypted");

        assertThat(result).isEqualTo("email@example.com");
        verify(encryptionService).decrypt("det-encrypted", null);
    }
}
