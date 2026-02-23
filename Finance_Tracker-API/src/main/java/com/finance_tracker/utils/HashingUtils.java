package com.finance_tracker.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class HashingUtils {

    private final ObjectMapper mapper;

    public HashingUtils() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        this.mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    public String toCanonicalJson(Object value) {
        if (value == null) {
            return "null";
        }
        try {
            Object sorted = mapper.treeToValue(mapper.valueToTree(value), Object.class);
            return mapper.writeValueAsString(sorted);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize value to canonical JSON", e);
        }
    }

    public String computeHash(
            String entityType,
            String entityId,
            String actionType,
            String beforeJson,
            String afterJson,
            String eventTimestamp,
            String prevHash,
            String userId,
            int eventVersion
    ) {
        String canonical = String.join("|",
                entityType,
                entityId,
                actionType,
                beforeJson,
                afterJson,
                eventTimestamp,
                prevHash == null ? "" : prevHash,
                userId,
                String.valueOf(eventVersion)
        );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
