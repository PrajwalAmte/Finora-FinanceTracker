package com.finance_tracker.dto;

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
public class SipResponseDTO {
    private Long id;
    private String name;
    private String schemeCode;
    private BigDecimal monthlyAmount;
    private LocalDate startDate;
    private Integer durationMonths;
    private BigDecimal currentNav;
    private BigDecimal totalUnits;
    private LocalDate lastUpdated;
    private LocalDate lastInvestmentDate;
    private BigDecimal currentValue;
    private Integer completedInstallments;
    private BigDecimal totalInvested;
    private BigDecimal profitLoss;
    private String isin;
    private String importSource;
    private Long investmentId;
}

