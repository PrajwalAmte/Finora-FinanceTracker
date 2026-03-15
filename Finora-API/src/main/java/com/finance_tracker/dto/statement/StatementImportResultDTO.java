package com.finance_tracker.dto.statement;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class StatementImportResultDTO {
    private int imported;
    private int updated;
    private int skipped;
    private Map<String, String> skippedReasons;
    private java.util.List<String> warnings;
}
