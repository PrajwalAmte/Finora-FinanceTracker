package com.finance_tracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BackupExportRequestDTO {

    @NotBlank(message = "Encryption password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    private String password;
}
