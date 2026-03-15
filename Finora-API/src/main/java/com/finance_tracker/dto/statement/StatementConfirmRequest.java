package com.finance_tracker.dto.statement;

import lombok.Data;

import java.util.List;

@Data
public class StatementConfirmRequest {
    private List<String> selectedIsins;
    private String statementType;
    private List<ParsedHolding> holdings;
    private List<ParsedMFHolding> mfHoldings;
}
