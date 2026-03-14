package com.finance_tracker.dto.statement;

import com.finance_tracker.model.InvestmentType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** Equity / ETF / bond holding parsed from a statement; maps to investments table on confirm. */
@Data
@Builder
public class ParsedHolding {
    private String isin;
    private String name;
    private String symbol;
    private BigDecimal quantity;
    private BigDecimal avgCost;
    private String importSource;  // written to import_source column
    private InvestmentType detectedType;
    private ImportStatus status;  // null until StatementImportService.preview() enriches it
}
