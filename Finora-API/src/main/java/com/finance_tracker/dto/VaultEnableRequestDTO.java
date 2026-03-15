package com.finance_tracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VaultEnableRequestDTO {

    @NotBlank(message = "Passphrase is required")
    @Size(min = 8, max = 128, message = "Passphrase must be 8-128 characters")
    private String passphrase;

    @NotBlank(message = "Confirmation is required")
    private String confirmation;
}
