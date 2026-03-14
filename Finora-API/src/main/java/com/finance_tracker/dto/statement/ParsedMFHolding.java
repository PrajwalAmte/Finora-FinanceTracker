package com.finance_tracker.dto.statement;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** MF holding parsed from CAS/CAMS/Excel; maps to investments (MUTUAL_FUND) on confirm. */
@Data
@Builder
public class ParsedMFHolding {
    private String isin;
    private String schemeName;
    // null if fund is delisted or not in AMFI; NAV scheduler skips gracefully
    private String schemeCode;
    private BigDecimal units;
    private BigDecimal avgCost;
    private BigDecimal nav;
    // null until StatementImportService.preview() enriches it
    private ImportStatus status;
}
