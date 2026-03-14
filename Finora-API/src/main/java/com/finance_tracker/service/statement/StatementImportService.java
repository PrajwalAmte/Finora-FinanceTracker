package com.finance_tracker.service.statement;

import com.finance_tracker.dto.statement.ImportStatus;
import com.finance_tracker.dto.statement.ParsedHolding;
import com.finance_tracker.dto.statement.ParsedMFHolding;
import com.finance_tracker.dto.statement.StatementConfirmRequest;
import com.finance_tracker.dto.statement.StatementImportResultDTO;
import com.finance_tracker.dto.statement.StatementPreviewDTO;
import com.finance_tracker.exception.StatementParseException;
import com.finance_tracker.model.Investment;
import com.finance_tracker.model.InvestmentType;
import com.finance_tracker.repository.InvestmentRepository;
import com.finance_tracker.service.AmfiNavService;
import com.finance_tracker.service.InvestmentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Two-step statement import: preview (parse + DB status enrichment) → confirm (transactional save). */
@Service
@RequiredArgsConstructor
public class StatementImportService {

    private static final Logger log = LoggerFactory.getLogger(StatementImportService.class);

    private final CasStatementParser    casParser;
    private final CamsStatementParser   camsParser;
    private final HoldingsExcelParser   excelParser;
    private final InvestmentRepository  investmentRepository;
    private final InvestmentService     investmentService;
    private final AmfiNavService        amfiNavService;

    /** Parses the file and enriches each holding with its DB-resolved ImportStatus. Read-only. */
    public StatementPreviewDTO preview(MultipartFile file,
                                        String statementType,
                                        String password,
                                        Long userId)
            throws StatementParseException {

        byte[] bytes = readBytes(file);
        StatementPreviewDTO preview = route(bytes, statementType, password);

        // Enrich ImportStatus for equity/ETF holdings
        List<ParsedHolding> enrichedHoldings = new ArrayList<>();
        for (ParsedHolding h : preview.getHoldings()) {
            h.setStatus(resolveHoldingStatus(userId, h.getIsin()));
            enrichedHoldings.add(h);
        }

        // Enrich ImportStatus for MF holdings
        List<ParsedMFHolding> enrichedMf = new ArrayList<>();
        for (ParsedMFHolding h : preview.getMfHoldings()) {
            h.setStatus(resolveHoldingStatus(userId, h.getIsin()));
            // Resolve AMFI scheme code if not already set (CAS parser leaves it null)
            if (h.getSchemeCode() == null) {
                h.setSchemeCode(amfiNavService.lookupSchemeCodeByIsin(h.getIsin()).orElse(null));
            }
            enrichedMf.add(h);
        }

        return StatementPreviewDTO.builder()
                .holdings(enrichedHoldings)
                .mfHoldings(enrichedMf)
                .warnings(preview.getWarnings())
                .statementDate(preview.getStatementDate())
                .build();
    }

    /** Imports user-selected ISINs into the DB; re-resolves status inside the transaction (TOCTOU guard). */
    @Transactional
    public StatementImportResultDTO confirmImport(StatementConfirmRequest request, Long userId) {

        Set<String> selectedIsins = Set.copyOf(request.getSelectedIsins());
        String statementType      = request.getStatementType();

        int imported = 0;
        int updated  = 0;
        int skipped  = 0;
        Map<String, String> skippedReasons = new HashMap<>();

        for (ParsedHolding h : safeList(request.getHoldings())) {
            String key = h.getIsin() != null ? h.getIsin() : h.getSymbol();
            if (!selectedIsins.contains(key)) continue;
            ImportOutcome outcome = saveHolding(h, statementType, userId, skippedReasons);
            imported += outcome.imported;
            updated  += outcome.updated;
            skipped  += outcome.skipped;
        }

        for (ParsedMFHolding h : safeList(request.getMfHoldings())) {
            String key = h.getIsin() != null ? h.getIsin() : h.getSchemeName();
            if (!selectedIsins.contains(key)) continue;
            ImportOutcome outcome = saveMfHolding(h, statementType, userId, skippedReasons);
            imported += outcome.imported;
            updated  += outcome.updated;
            skipped  += outcome.skipped;
        }

        log.info("Statement import for user {}: imported={}, updated={}, skipped={}",
                userId, imported, updated, skipped);

        return StatementImportResultDTO.builder()
                .imported(imported)
                .updated(updated)
                .skipped(skipped)
                .skippedReasons(skippedReasons)
                .build();
    }

    private ImportOutcome saveHolding(ParsedHolding h, String statementType,
                                       Long userId, Map<String, String> skippedReasons) {
        // Re-resolve status inside transaction (TOCTOU guard)
        Optional<Investment> existing;
        if (h.getIsin() != null) {
            existing = investmentRepository.findByUserIdAndIsin(userId, h.getIsin());
        } else {
            // Symbol-only import (CSV without ISIN) — dedup by symbol
            existing = h.getSymbol() != null
                    ? investmentRepository.findFirstByUserIdAndSymbol(userId, h.getSymbol())
                    : Optional.empty();
        }

        if (existing.isPresent()) {
            Investment inv = existing.get();
            if (inv.getImportSource() == null) {
                // Manual record — sacred, never overwrite.
                skippedReasons.put(h.getIsin(),
                        "Manual entry — will not be overwritten by statement import. "
                                + "Edit the record directly if you want to update it.");
                return ImportOutcome.SKIP;
            }
            // Existing imported record — update in place.
            return updateInvestment(inv, h.getQuantity(), h.getAvgCost(), h.getLtp(), userId);
        }

        // New record.
        try {
            return insertHolding(h, statementType, userId);
        } catch (Exception e) {
            // Catch DB unique-index violation from a concurrent import.
            if (isUniqueConstraintViolation(e)) {
                skippedReasons.put(h.getIsin(),
                        "Concurrent import detected — another import already saved this holding. "
                                + "Refresh and re-import if needed.");
                return ImportOutcome.SKIP;
            }
            throw e;
        }
    }

    private ImportOutcome saveMfHolding(ParsedMFHolding h, String statementType,
                                         Long userId, Map<String, String> skippedReasons) {

        // Resolve scheme code if still null
        String schemeCode = h.getSchemeCode() != null
                ? h.getSchemeCode()
                : amfiNavService.lookupSchemeCodeByIsin(h.getIsin()).orElse(null);

        // Re-resolve status inside the transaction.
        Optional<Investment> existing = investmentRepository.findByUserIdAndIsin(userId, h.getIsin());

        if (existing.isPresent()) {
            Investment inv = existing.get();
            if (inv.getImportSource() == null) {
                skippedReasons.put(h.getIsin(),
                        "Manual entry — will not be overwritten by statement import.");
                return ImportOutcome.SKIP;
            }
            // Update existing imported MF row.
            return updateInvestment(inv, h.getUnits(), h.getAvgCost(), h.getNav(), userId);
        }

        // New MF holding → investment row (type = MUTUAL_FUND).
        try {
            return insertMfHolding(h, schemeCode, statementType, userId);
        } catch (Exception e) {
            if (isUniqueConstraintViolation(e)) {
                skippedReasons.put(h.getIsin(),
                        "Concurrent import detected — another import already saved this holding.");
                return ImportOutcome.SKIP;
            }
            throw e;
        }
    }

    private ImportOutcome updateInvestment(Investment inv, BigDecimal qty,
                                            BigDecimal avgCost, BigDecimal ltp, Long userId) {
        if (qty != null)     inv.setQuantity(qty);
        if (avgCost != null) {
            inv.setPurchasePrice(avgCost);
        }
        // Always update currentPrice from statement if we have ltp; otherwise fall back to avgCost
        BigDecimal newCurrentPrice = ltp != null ? ltp : avgCost;
        if (newCurrentPrice != null && inv.getImportSource() != null) {
            inv.setCurrentPrice(newCurrentPrice);
        }
        inv.setLastUpdated(LocalDate.now());
        investmentService.saveInvestment(inv);
        return ImportOutcome.UPDATED;
    }

    private ImportOutcome insertHolding(ParsedHolding h, String statementType, Long userId) {
        BigDecimal purchasePrice = h.getAvgCost() != null ? h.getAvgCost() : BigDecimal.ZERO;
        BigDecimal currentPrice  = h.getLtp()     != null ? h.getLtp()     : purchasePrice;

        Investment inv = new Investment();
        String nameVal   = h.getName()   != null ? h.getName()   : (h.getIsin() != null ? h.getIsin() : h.getSymbol());
        String symbolVal = h.getSymbol() != null ? h.getSymbol() : (h.getIsin() != null ? h.getIsin() : nameVal);
        inv.setName(nameVal != null ? nameVal : "UNKNOWN");
        inv.setSymbol(symbolVal != null ? symbolVal : "UNKNOWN");
        inv.setType(h.getDetectedType() != null ? h.getDetectedType() : InvestmentType.STOCK);
        inv.setQuantity(h.getQuantity());
        inv.setPurchasePrice(purchasePrice);
        inv.setCurrentPrice(currentPrice);
        inv.setPurchaseDate(LocalDate.now());
        inv.setLastUpdated(LocalDate.now());
        inv.setIsin(h.getIsin());
        inv.setImportSource(statementType);
        inv.setUserId(userId);

        investmentService.saveInvestment(inv);
        return ImportOutcome.IMPORTED;
    }

    private ImportOutcome insertMfHolding(ParsedMFHolding h, String schemeCode,
                                           String statementType, Long userId) {
        BigDecimal nav = h.getNav() != null ? h.getNav()
                : (h.getAvgCost() != null ? h.getAvgCost() : BigDecimal.ZERO);
        BigDecimal avg = h.getAvgCost() != null ? h.getAvgCost() : BigDecimal.ZERO;

        // Symbol for MF: prefer AMFI scheme code, then ISIN, then truncated scheme name
        String mfSymbol = schemeCode != null ? schemeCode
                : (h.getIsin() != null ? h.getIsin()
                : (h.getSchemeName() != null ? h.getSchemeName() : "UNKNOWN_MF"));

        Investment inv = new Investment();
        inv.setName(h.getSchemeName() != null ? h.getSchemeName() : mfSymbol);
        // symbol = AMFI scheme code (used by NAV scheduler); falls back to ISIN or scheme name
        inv.setSymbol(mfSymbol);
        inv.setType(InvestmentType.MUTUAL_FUND);
        inv.setQuantity(h.getUnits());
        inv.setPurchasePrice(avg);
        inv.setCurrentPrice(nav);
        inv.setPurchaseDate(LocalDate.now());
        inv.setLastUpdated(LocalDate.now());
        inv.setIsin(h.getIsin());
        inv.setImportSource(statementType);
        inv.setUserId(userId);

        investmentService.saveInvestment(inv);
        return ImportOutcome.IMPORTED;
    }

    // Routes to the correct parser; all Excel variants use HoldingsExcelParser (column-name detection)
    private StatementPreviewDTO route(byte[] bytes, String statementType, String password)
            throws StatementParseException {
        return switch (statementType.trim().toUpperCase()) {
            case "CAS"  -> casParser.parse(bytes, password, "CAS");
            case "CAMS" -> camsParser.parse(bytes, password, "CAMS");
            case "ZERODHA_EXCEL", "GROWW_EXCEL", "UPSTOX_EXCEL",
                 "HDFC_EXCEL", "ICICI_EXCEL", "ANGEL_EXCEL",
                 "5PAISA_EXCEL", "KOTAK_EXCEL", "SHAREKHAN_EXCEL",
                 "EXCEL" -> excelParser.parse(bytes, password, statementType.trim().toUpperCase());
            case "CSV", "HOLDINGS_CSV", "ZERODHA_CSV", "GROWW_CSV",
                 "UPSTOX_CSV", "HOLDINGS_FILE_CSV"
                    -> excelParser.parse(bytes, password, statementType.trim().toUpperCase());
            default -> throw new StatementParseException(
                    "Unknown statement type '" + statementType + "'. "
                            + "Supported: CAS, CAMS, ZERODHA_EXCEL, GROWW_EXCEL, "
                            + "UPSTOX_EXCEL, HDFC_EXCEL, ICICI_EXCEL, ANGEL_EXCEL, EXCEL, "
                            + "CSV, HOLDINGS_CSV, ZERODHA_CSV, GROWW_CSV.");
        };
    }

    private ImportStatus resolveHoldingStatus(Long userId, String isin) {
        if (isin == null) return ImportStatus.NEW;
        Optional<Investment> existing = investmentRepository.findByUserIdAndIsin(userId, isin);
        if (existing.isEmpty())                         return ImportStatus.NEW;
        if (existing.get().getImportSource() == null)   return ImportStatus.SKIP_MANUAL;
        return ImportStatus.UPDATE;
    }

    private byte[] readBytes(MultipartFile file) throws StatementParseException {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new StatementParseException(
                    "Failed to read the uploaded file: " + e.getMessage(), e);
        }
    }

    private boolean isUniqueConstraintViolation(Exception e) {
        Throwable cause = e;
        while (cause != null) {
            String msg = cause.getMessage();
            if (msg != null && (msg.contains("unique") || msg.contains("duplicate key")
                    || msg.contains("idx_investments_user_isin"))) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private <T> List<T> safeList(List<T> list) {
        return list != null ? list : List.of();
    }

    private static class ImportOutcome {
        final int imported;
        final int updated;
        final int skipped;

        private ImportOutcome(int i, int u, int s) { imported = i; updated = u; skipped = s; }

        static final ImportOutcome IMPORTED = new ImportOutcome(1, 0, 0);
        static final ImportOutcome UPDATED  = new ImportOutcome(0, 1, 0);
        static final ImportOutcome SKIP     = new ImportOutcome(0, 0, 1);
    }
}
