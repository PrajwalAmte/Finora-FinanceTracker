package com.finance_tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerIntegrityResultDTO {
    private boolean valid;
    private long eventCount;
    private String brokenAtEventUuid;
}
