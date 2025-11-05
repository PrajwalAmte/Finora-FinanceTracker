package com.finance_tracker.model;

import jakarta.persistence.*;
import lombok.Data;
import jakarta.validation.constraints.*;
import java.math.RoundingMode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "investments")
public class Investment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    @NotBlank
    private String symbol;

    @Enumerated(EnumType.STRING)
    @NotNull
    private InvestmentType type; // STOCK, MUTUAL_FUND, etc.

    @Digits(integer = 13, fraction = 6)
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(precision = 19, scale = 6)
    private BigDecimal quantity;

    @Digits(integer = 13, fraction = 6)
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(precision = 19, scale = 6)
    private BigDecimal purchasePrice;

    @Digits(integer = 13, fraction = 6)
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(precision = 19, scale = 6)
    private BigDecimal currentPrice;

    private LocalDate purchaseDate;
    private LocalDate lastUpdated;

    // Helper methods
    public BigDecimal getCurrentValue() {
        return quantity.multiply(currentPrice);
    }

    public BigDecimal getProfitLoss() {
        BigDecimal costBasis = quantity.multiply(purchasePrice);
        return getCurrentValue().subtract(costBasis);
    }

    public BigDecimal getReturnPercentage() {
        BigDecimal costBasis = quantity.multiply(purchasePrice);
        if (costBasis.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return getProfitLoss().multiply(new BigDecimal("100")).divide(costBasis, 2, RoundingMode.HALF_UP);
    }
}
