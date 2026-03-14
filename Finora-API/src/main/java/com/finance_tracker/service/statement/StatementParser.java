package com.finance_tracker.service.statement;

import com.finance_tracker.dto.statement.StatementPreviewDTO;
import com.finance_tracker.exception.StatementParseException;

/**
 * Common contract for all statement file parsers.
 *
 * Implementations:
 *  - CasStatementParser   — CAS PDF (CDSL / NSDL), password = PAN_lowercase + DDMMYYYY
 *  - CamsStatementParser  — CAMS PDF, password = email registered with CAMS
 *  - HoldingsExcelParser  — Generic broker Holdings Excel (.xlsx / .xls)
 *
 * All parsing is in-memory only; no bytes are written to disk.
 * The password parameter is nullable/blank for unprotected files.
 * The statementType parameter ("CAS" | "CAMS" | "ZERODHA_EXCEL" | "EXCEL" | etc.)
 * is stamped into every ParsedHolding.importSource field so the import service
 * can write it straight to the DB import_source column.
 */
public interface StatementParser {

    /**
     * @param fileBytes     raw bytes of the uploaded file (PDF or Excel)
     * @param password      decryption password; null or blank = no password
     * @param statementType source tag written to import_source on each saved row
     * @return              preview DTO enriched with holdings and non-fatal warnings
     * @throws StatementParseException for unrecoverable parse failures (wrong password,
     *                                 unrecognised format, missing required columns)
     */
    StatementPreviewDTO parse(byte[] fileBytes, String password, String statementType)
            throws StatementParseException;
}
