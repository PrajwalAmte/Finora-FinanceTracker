package com.finance_tracker.dto.statement;

import lombok.Data;

import java.util.List;

/**
 * Request body for POST /api/statements/confirm.
 * The client echoes parsed holdings from the preview step back here to avoid re-parsing the file.
 */
@Data
public class StatementConfirmRequest {
    private List<String> selectedIsins;
    private String statementType;
    private List<ParsedHolding> holdings;
    private List<ParsedMFHolding> mfHoldings;
}
