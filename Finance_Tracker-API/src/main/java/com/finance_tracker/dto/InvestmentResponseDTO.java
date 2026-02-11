package com.finance_tracker.dto;

import com.finance_tracker.model.InvestmentType;
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
public class InvestmentResponseDTO {
    private Long id;
    private String name;
    private String symbol;
    private InvestmentType type;
    private BigDecimal quantity;
    private BigDecimal purchasePrice;
    private BigDecimal currentPrice;
    private LocalDate purchaseDate;
    private LocalDate lastUpdated;
    private BigDecimal currentValue;
    private BigDecimal profitLoss;
    private BigDecimal returnPercentage;
}

