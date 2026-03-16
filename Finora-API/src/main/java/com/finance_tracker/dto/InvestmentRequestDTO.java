package com.finance_tracker.dto;

import com.finance_tracker.model.InvestmentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class InvestmentRequestDTO {
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Symbol is required")
    private String symbol;

    @NotNull(message = "Type is required")
    private InvestmentType type;

    @NotNull(message = "Quantity is required")
    @Digits(integer = 13, fraction = 6, message = "Invalid quantity format")
    @DecimalMin(value = "0.000001", message = "Quantity must be greater than 0")
    private BigDecimal quantity;

    @NotNull(message = "Purchase price is required")
    @Digits(integer = 13, fraction = 6, message = "Invalid purchase price format")
    @DecimalMin(value = "0.000001", message = "Purchase price must be greater than 0")
    private BigDecimal purchasePrice;

    @Digits(integer = 13, fraction = 6, message = "Invalid current price format")
    @DecimalMin(value = "0.000001", message = "Current price must be greater than 0")
    private BigDecimal currentPrice;

    private LocalDate purchaseDate;
}

