package com.finance_tracker.dto;

import com.finance_tracker.model.CompoundingFrequency;
import com.finance_tracker.model.LoanInterestType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class LoanRequestDTO {
    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Principal amount is required")
    @Digits(integer = 17, fraction = 2, message = "Invalid principal amount format")
    @DecimalMin(value = "0.01", message = "Principal amount must be greater than 0")
    private BigDecimal principalAmount;

    @NotNull(message = "Interest rate is required")
    @Digits(integer = 3, fraction = 6, message = "Invalid interest rate format")
    @DecimalMin(value = "0.000001", message = "Interest rate must be greater than 0")
    private BigDecimal interestRate;

    @NotNull(message = "Interest type is required")
    private LoanInterestType interestType;

    @NotNull(message = "Compounding frequency is required")
    private CompoundingFrequency compoundingFrequency;

    private LocalDate startDate;

    @NotNull(message = "Tenure in months is required")
    private Integer tenureMonths;
}

