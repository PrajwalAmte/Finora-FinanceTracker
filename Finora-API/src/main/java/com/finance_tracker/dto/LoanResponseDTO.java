package com.finance_tracker.dto;

import com.finance_tracker.model.CompoundingFrequency;
import com.finance_tracker.model.LoanInterestType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanResponseDTO {
    private Long id;
    private String name;
    private BigDecimal principalAmount;
    private BigDecimal interestRate;
    private LoanInterestType interestType;
    private CompoundingFrequency compoundingFrequency;
    private LocalDate startDate;
    private Integer tenureMonths;
    private BigDecimal emiAmount;
    private BigDecimal currentBalance;
    private LocalDate lastUpdated;
    private LocalDate endDate;
    private Integer remainingMonths;
    private BigDecimal totalRepayment;
    private BigDecimal totalInterest;
}

