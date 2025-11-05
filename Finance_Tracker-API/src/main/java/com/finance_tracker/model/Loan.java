package com.finance_tracker.model;

import jakarta.persistence.*;
import lombok.Data;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Data
@Entity
@Table(name = "loans")
public class Loan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    @Digits(integer = 17, fraction = 2)
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(precision = 19, scale = 2)
    private BigDecimal principalAmount;

    @Digits(integer = 3, fraction = 6)
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(precision = 9, scale = 6)
    private BigDecimal interestRate; // Annual interest rate (in percentage)

    @Enumerated(EnumType.STRING)
    @NotNull
    private LoanInterestType interestType; // SIMPLE, COMPOUND

    @Enumerated(EnumType.STRING)
    @NotNull
    private CompoundingFrequency compoundingFrequency; // MONTHLY, QUARTERLY, YEARLY (for compound interest)
    private LocalDate startDate;
    private Integer tenureMonths;

    @Column(precision = 19, scale = 2)
    private BigDecimal emiAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal currentBalance;
    private LocalDate lastUpdated;

    // Helper methods
    public LocalDate getEndDate() {
        if (startDate == null || tenureMonths == null) {
            return null;
        }
        return startDate.plusMonths(tenureMonths);
    }

    public Integer getRemainingMonths() {
        if (getEndDate() == null) {
            return null;
        }
        LocalDate today = LocalDate.now();
        return (int) today.until(getEndDate(), ChronoUnit.MONTHS);
    }

    public BigDecimal getTotalRepayment() {
        if (emiAmount == null || tenureMonths == null) {
            return null;
        }
        return emiAmount.multiply(new BigDecimal(tenureMonths));
    }

    public BigDecimal getTotalInterest() {
        BigDecimal totalRepayment = getTotalRepayment();
        if (totalRepayment == null || principalAmount == null) {
            return null;
        }
        return totalRepayment.subtract(principalAmount);
    }
}
