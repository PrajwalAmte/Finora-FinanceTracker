package com.finance_tracker.dto.expense;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ExpenseImportRequest {
    private List<ExpenseEntry> expenses;

    @Data
    public static class ExpenseEntry {
        private String date;
        private String description;
        private BigDecimal amount;
        private String category;
        private String paymentMethod;
    }
}
