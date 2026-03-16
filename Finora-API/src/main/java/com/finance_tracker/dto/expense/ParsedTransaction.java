package com.finance_tracker.dto.expense;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedTransaction {
    private String date;
    private String narration;
    private BigDecimal amount;
    private String type;
    private BigDecimal balance;
}
