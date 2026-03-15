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

    private String schemeCode;

    @NotNull(message = "Monthly amount is required")
    @Digits(integer = 17, fraction = 2, message = "Invalid monthly amount format")
    @DecimalMin(value = "0.01", message = "Monthly amount must be greater than 0")
    private BigDecimal monthlyAmount;

    private LocalDate startDate;

    private Integer durationMonths;

    private BigDecimal currentNav;
    private BigDecimal totalUnits;

    private BigDecimal totalInvested;

    private String isin;
    private String importSource;

    private Long investmentId;
}

