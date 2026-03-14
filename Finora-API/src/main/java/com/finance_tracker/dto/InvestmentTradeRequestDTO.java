package com.finance_tracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class InvestmentTradeRequestDTO {

    @NotNull(message = "Quantity is required")
    @Digits(integer = 13, fraction = 6, message = "Invalid quantity format")
    @DecimalMin(value = "0.000001", message = "Quantity must be greater than 0")
    private BigDecimal quantity;

    @NotNull(message = "Price is required")
    @Digits(integer = 13, fraction = 6, message = "Invalid price format")
    @DecimalMin(value = "0.000001", message = "Price must be greater than 0")
    private BigDecimal price;

    /** Optional — defaults to today on the server side if omitted. */
    private LocalDate tradeDate;
}
