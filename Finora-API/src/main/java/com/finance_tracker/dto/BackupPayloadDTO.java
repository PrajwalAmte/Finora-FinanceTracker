package com.finance_tracker.dto;

import com.finance_tracker.model.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * The full backup payload that gets serialized to JSON and then encrypted.
 * Contains all user data tables plus the append-only ledger chain.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupPayloadDTO {

    private BackupMetadataDTO metadata;
    private List<Expense> expenses;
    private List<Investment> investments;
    private List<Loan> loans;
    private List<Sip> sips;
    private List<LedgerEvent> ledgerEvents;
}
