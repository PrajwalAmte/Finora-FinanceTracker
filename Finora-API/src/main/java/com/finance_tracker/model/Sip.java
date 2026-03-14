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

    @NotBlank
    private String schemeCode;

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

    @Column(name = "user_id")
    private Long userId;

    // null for manually created SIP records
    private String isin;

    // null = manual (sacred); CAS / CAMS for statement-imported rows
    @Column(name = "import_source")
    private String importSource;

    // Helper methods
    public BigDecimal getCurrentValue() {
        return totalUnits.multiply(currentNav);
    }

    public Integer getCompletedInstallments() {
        if (startDate == null) {
            return 0;
        }

        LocalDate endDate = lastInvestmentDate != null ? lastInvestmentDate : LocalDate.now();

        if (startDate.isAfter(endDate)) {
            return 0;
        }

        return (int) (startDate.until(endDate).toTotalMonths() + 1);
    }

    public BigDecimal getTotalInvested() {
        return monthlyAmount.multiply(new BigDecimal(getCompletedInstallments()));
    }

    public BigDecimal getProfitLoss() {
        return getCurrentValue().subtract(getTotalInvested());
    }
}
