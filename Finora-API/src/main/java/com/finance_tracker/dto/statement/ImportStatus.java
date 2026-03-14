package com.finance_tracker.dto.statement;

/** What will happen to a holding when the user confirms import. */
public enum ImportStatus {
    NEW,          // no existing DB row for (user, isin) — will INSERT
    UPDATE,       // existing imported row — qty/avgCost will be overwritten
    SKIP_MANUAL   // existing row with null import_source — never overwritten
}
