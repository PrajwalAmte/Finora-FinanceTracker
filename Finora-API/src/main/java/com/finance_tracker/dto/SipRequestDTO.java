package com.finance_tracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SipRequestDTO {
    @NotBlank(message = "Name is required")
    private String name;

    // Optional: needed for AMFI NAV auto-refresh; can be null for import-derived SIPs
    private String schemeCode;

    @NotNull(message = "Monthly amount is required")
    @Digits(integer = 17, fraction = 2, message = "Invalid monthly amount format")
    @DecimalMin(value = "0.01", message = "Monthly amount must be greater than 0")
    private BigDecimal monthlyAmount;

    private LocalDate startDate;

    // Optional — defaults to 120 months (10 years) when null
    private Integer durationMonths;

    // Current NAV and total units — sent by frontend when creating from statement import
    private BigDecimal currentNav;
    private BigDecimal totalUnits;

    // For SIPs created from MF import
    private String isin;
    private String importSource;
}

