package com.finance_tracker.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BackupMetadataDTO {

    private String version;
    private OffsetDateTime exportTimestamp;
    private Long userId;
    private String username;
    private String ledgerRootHash;
    private long ledgerEventCount;
    private long expenseCount;
    private long investmentCount;
    private long loanCount;
    private long sipCount;
}
