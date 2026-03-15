package com.finance_tracker.dto.statement;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ParsedMFHolding {
    private String isin;
    private String schemeName;
    private String schemeCode;
    private BigDecimal units;
    private BigDecimal avgCost;
    private BigDecimal nav;
    private ImportStatus status;
}
