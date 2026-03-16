package com.finance_tracker.dto.expense;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpensePreviewDTO {
    private List<ParsedTransaction> transactions;
    private List<String> warnings;
    private String bankName;
    private int totalDebits;
    private int totalCredits;
}
