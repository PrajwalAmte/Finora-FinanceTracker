package com.finance_tracker.utils.converter;

import com.finance_tracker.service.FieldEncryptionService;
import com.finance_tracker.utils.security.VaultKeyContext;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final FieldEncryptionService encryptionService;

    public EncryptedStringConverter(FieldEncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        String vaultKey = VaultKeyContext.get();
        return encryptionService.encrypt(attribute, vaultKey);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        String vaultKey = VaultKeyContext.get();
        return encryptionService.decrypt(dbData, vaultKey);
    }
}
