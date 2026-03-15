package com.finance_tracker.utils.converter;

import com.finance_tracker.service.FieldEncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

@Converter
@Component
public class DeterministicEncryptedStringConverter implements AttributeConverter<String, String> {

    private final FieldEncryptionService encryptionService;

    public DeterministicEncryptedStringConverter(FieldEncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        return encryptionService.encryptDeterministic(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return encryptionService.decrypt(dbData, null);
    }
}
