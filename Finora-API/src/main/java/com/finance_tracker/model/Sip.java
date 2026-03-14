package com.finance_tracker.model;

import jakarta.persistence.*;
import lombok.Data;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "sips")
public class Sip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    private String schemeCode;   // nullable for import-derived SIPs


    @Digits(integer = 17, fraction = 2)
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(precision = 19, scale = 2)
    private BigDecimal monthlyAmount;
    private LocalDate startDate;
    private Integer durationMonths;

    @Digits(integer = 13, fraction = 6)
    @Column(precision = 19, scale = 6)
    private BigDecimal currentNav;

    @Digits(integer = 16, fraction = 8)
    @Column(precision = 24, scale = 8)
    private BigDecimal totalUnits;
    private LocalDate lastUpdated;
    private LocalDate lastInvestmentDate;

    // Stores the actual total amount invested so far.
    // Incremented by monthlyAmount each time a payment is recorded.
    // Seeded from statement import (units × avgCost) when available.
    @Column(name = "total_invested", precision = 19, scale = 2)
    private BigDecimal totalInvested;

    @Column(name = "user_id")
    private Long userId;

    // null for manually created SIP records
    private String isin;

    // null = manual (sacred); CAS / CAMS for statement-imported rows
    @Column(name = "import_source")
    private String importSource;

    // When set, this SIP is backed by the linked Investment (source of truth for units/NAV/value).
    // NULL = standalone SIP with its own unit/NAV tracking.
    @Column(name = "investment_id")
    private Long investmentId;

    // Helper methods
    public BigDecimal getCurrentValue() {
        if (totalUnits == null || currentNav == null) return BigDecimal.ZERO;
        return totalUnits.multiply(currentNav);
    }

    public BigDecimal getTotalInvested() {
        return totalInvested != null ? totalInvested : BigDecimal.ZERO;
    }

    /** Approximate count derived from stored totalInvested. */
    public Integer getCompletedInstallments() {
        if (monthlyAmount == null || monthlyAmount.compareTo(BigDecimal.ZERO) == 0) return 0;
        return getTotalInvested().divide(monthlyAmount, 0, java.math.RoundingMode.HALF_UP).intValue();
    }

    public BigDecimal getProfitLoss() {
        return getCurrentValue().subtract(getTotalInvested());
    }
}
