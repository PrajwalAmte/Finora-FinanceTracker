package com.finance_tracker.dto.statement;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class StatementPreviewDTO {
    private List<ParsedHolding> holdings;
    private List<ParsedMFHolding> mfHoldings;
    private List<String> warnings;
    private LocalDate statementDate;
}
