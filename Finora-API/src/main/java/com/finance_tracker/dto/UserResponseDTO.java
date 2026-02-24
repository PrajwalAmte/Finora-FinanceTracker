package com.finance_tracker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.finance_tracker.model.Role;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class UserResponseDTO {

    private Long id;
    private String username;
    private String email;
    private Role role;

    @JsonProperty("isActive")
    private boolean isActive;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime lastLoginAt;
}
