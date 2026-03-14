package com.finance_tracker.dto.statement;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/** Response for POST /api/statements/confirm — import counts and per-ISIN skip reasons. */
@Data
@Builder
public class StatementImportResultDTO {
    private int imported;
    private int updated;
    private int skipped;
    // key = ISIN, value = human-readable reason (only populated for skipped rows)
    private Map<String, String> skippedReasons;
}
