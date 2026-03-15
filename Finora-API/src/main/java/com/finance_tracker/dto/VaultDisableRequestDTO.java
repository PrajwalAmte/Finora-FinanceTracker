package com.finance_tracker.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request to disable vault encryption (requires current passphrase).
 */
@Data
public class VaultDisableRequestDTO {

    @NotBlank(message = "Current passphrase is required")
    private String passphrase;
}
