package com.finance_tracker.dto.statement;

import com.finance_tracker.model.InvestmentType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ParsedHolding {
    private String isin;
    private String name;
    private String symbol;
    private BigDecimal quantity;
    private BigDecimal avgCost;
    private BigDecimal ltp;
    private String importSource;
    private InvestmentType detectedType;
    private ImportStatus status;
}
