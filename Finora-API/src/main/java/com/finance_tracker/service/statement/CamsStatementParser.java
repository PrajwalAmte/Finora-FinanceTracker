package com.finance_tracker.service.statement;

import com.finance_tracker.dto.statement.ParsedMFHolding;
import com.finance_tracker.dto.statement.StatementPreviewDTO;
import com.finance_tracker.exception.StatementParseException;
import com.finance_tracker.service.AmfiNavService;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses CAMS Consolidated Statement of Accounts (MF only).
 * Password: the email address registered with CAMS.
 * Multi-folio schemes are aggregated (units summed, weighted avgCost computed).
 */
@Service
@RequiredArgsConstructor
public class CamsStatementParser implements StatementParser {

    private static final Logger log = LoggerFactory.getLogger(CamsStatementParser.class);

    private final AmfiNavService amfiNavService;

    // Standard 12-char ISIN, typically inside parentheses in CAMS: "(INF179KB1HH7)"
    private static final Pattern ISIN_IN_PARENS =
            Pattern.compile("\\((IN[A-Z0-9]{10})\\)");

    // Fallback: bare ISIN label "ISIN: INF179KB1HH7" (some CAMS formats)
    private static final Pattern ISIN_LABEL =
            Pattern.compile("(?i)\\bisin\\s*[:\\-]\\s*(IN[A-Z0-9]{10})\\b");

    private static final Pattern NUM_PATTERN =
            Pattern.compile("[\\d,]+(?:\\.\\d+)?");

    // NAV after "@" in "Closing Balance: 100 @ 52.30"
    private static final Pattern AT_NAV =
            Pattern.compile("@\\s*(?:Rs\\.?\\s*)?([\\d,]+(?:\\.\\d+)?)");

    // Suffix patterns that don't help with AMFI lookup — strip before resolving
    private static final String[] SCHEME_SUFFIXES = {
            "(?i)\\s*-\\s*Growth\\s*(?:Option|Plan)?\\s*$",
            "(?i)\\s*-\\s*IDCW\\s*(?:Payout|Reinvestment|Option|Plan)?\\s*$",
            "(?i)\\s*-\\s*Dividend\\s*(?:Payout|Reinvestment|Option|Plan)?\\s*$",
            "(?i)\\s*-\\s*Direct\\s*Plan\\s*-\\s*Growth\\s*$",
            "(?i)\\s*-\\s*Regular\\s*Plan\\s*-\\s*Growth\\s*$",
            "(?i)\\s*-\\s*Direct\\s*-\\s*Growth\\s*$",
            "(?i)\\s*-\\s*Regular\\s*-\\s*Growth\\s*$",
            "(?i)\\s*\\([^)]{0,40}\\)\\s*$",   // trailing parenthetical e.g. "(formerly XYZ)"
    };

    private static final List<DateTimeFormatter> DATE_FMTS = List.of(
            DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"));

    private static final Pattern DATE_CONTEXT =
            Pattern.compile("(?i)(?:as\\s+on|as\\s+of|statement\\s+date|closing\\s+date|"
                    + "date)\\s*[:\\-]?\\s*(\\d{1,2}[/\\-](?:\\d{1,2}|[A-Za-z]{3})[/\\-]\\d{2,4})");

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    @Override
    public StatementPreviewDTO parse(byte[] fileBytes, String password, String statementType)
            throws StatementParseException {

        String rawText = decryptAndExtract(fileBytes, password);
        String[] lines = rawText.split("\\r?\\n");

        List<ParsedMFHolding> mfHoldings = new ArrayList<>();
        List<String>          warnings   = new ArrayList<>();
        LocalDate statementDate = extractStatementDate(rawText);

        parseSchemes(lines, mfHoldings, warnings);

        if (mfHoldings.isEmpty()) {
            warnings.add("No MF holdings were extracted from the CAMS statement. "
                    + "Verify the file is a valid CAMS Consolidated Statement and the "
                    + "password is your CAMS-registered email address.");
        }

        return StatementPreviewDTO.builder()
                .holdings(List.of())        // CAMS contains only MF data
                .mfHoldings(mfHoldings)
                .warnings(warnings)
                .statementDate(statementDate)
                .build();
    }

    private String decryptAndExtract(byte[] fileBytes, String password)
            throws StatementParseException {
        try {
            PDDocument doc = (password != null && !password.isBlank())
                    ? Loader.loadPDF(fileBytes, password)
                    : Loader.loadPDF(fileBytes);
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(doc);
            doc.close();
            return text;
        } catch (IOException e) {
            String msg = e.getMessage() != null && e.getMessage().toLowerCase().contains("encrypt")
                    ? "Wrong password — CAMS statement password is the email address "
                      + "you registered with CAMS."
                    : "Could not read the CAMS PDF — it may be corrupted. " + e.getMessage();
            throw new StatementParseException(msg, e);
        }
    }

    // Line-by-line state machine: new ISIN line starts a scheme section; closing balance + cost lines accumulate values
    private void parseSchemes(String[] lines,
                               List<ParsedMFHolding> out,
                               List<String> warnings) {

        // Accumulator maps keyed by ISIN
        Map<String, String>     isinToName       = new LinkedHashMap<>();
        Map<String, String>     isinToSchemeCode = new LinkedHashMap<>();
        Map<String, BigDecimal> isinToUnits      = new LinkedHashMap<>();
        Map<String, BigDecimal> isinToTotalCost  = new LinkedHashMap<>();
        Map<String, BigDecimal> isinToNav        = new LinkedHashMap<>();

        String activeIsin = null;
        boolean skipActive = false;     // true when current scheme should be excluded (e.g. FMP)

        for (int i = 0; i < lines.length; i++) {
            String raw = lines[i].trim();
            String low = raw.toLowerCase();

            // ------------------------------------------------------------------
            // Detect new scheme section (ISIN in parentheses, or ISIN label)
            // ------------------------------------------------------------------
            String detectedIsin = detectIsin(raw);

            if (detectedIsin != null) {
                activeIsin  = detectedIsin;
                skipActive  = false;

                // Name = everything before the ISIN parenthetical (or whole line for label format)
                String schemeName = extractSchemeName(raw, detectedIsin);

                // --- FMP / matured fund detection ---
                if (low.contains("fmp") || low.contains("fixed maturity")) {
                    // Look ahead up to 15 lines for "matured" / "redeemed"
                    for (int j = i + 1; j < Math.min(i + 15, lines.length); j++) {
                        String fwdLow = lines[j].toLowerCase();
                        if (fwdLow.contains("matur") || fwdLow.contains("redeem")) {
                            skipActive = true;
                            warnings.add("Skipped " + schemeName
                                    + " — FMP appears to have matured/redeemed.");
                            break;
                        }
                    }
                    if (skipActive) { activeIsin = null; continue; }
                }

                // Register if first occurrence; subsequent folios reuse existing entry
                if (!isinToName.containsKey(activeIsin)) {
                    isinToName.put(activeIsin, schemeName);
                    isinToUnits.put(activeIsin, BigDecimal.ZERO);
                    isinToTotalCost.put(activeIsin, BigDecimal.ZERO);

                    // Resolve AMFI scheme code
                    String code = amfiNavService.lookupSchemeCodeByIsin(activeIsin).orElse(null);
                    isinToSchemeCode.put(activeIsin, code);
                    if (code == null) {
                        warnings.add("No AMFI scheme code for ISIN " + activeIsin
                                + " (" + schemeName + ") — NAV will not auto-update after import.");
                    }
                }
                continue;
            }

            if (activeIsin == null || skipActive) continue;

            // closing balance → units + optional NAV ("Closing Balance : 521.053 @ Rs. 52.30")
            if (low.contains("closing balance") || low.contains("closing unit balance")
                    || low.contains("close balance")) {
                BigDecimal units = extractFirstPositiveNumber(raw);
                if (units != null) {
                    isinToUnits.merge(activeIsin, units, BigDecimal::add);
                }
                // Optional NAV after "@"
                Matcher navM = AT_NAV.matcher(raw);
                if (navM.find()) {
                    try {
                        BigDecimal nav = new BigDecimal(navM.group(1).replace(",", ""));
                        isinToNav.putIfAbsent(activeIsin, nav);
                    } catch (NumberFormatException ignored) {}
                }
            }

            // invested/cost value line
            if (low.contains("invested value") || low.contains("cost value")
                    || low.contains("purchase cost") || low.contains("amount invested")
                    || (low.contains("cost") && !low.contains("at cost"))) {
                BigDecimal cost = extractLastNumber(raw);
                if (cost != null && cost.compareTo(BigDecimal.ZERO) > 0) {
                    isinToTotalCost.merge(activeIsin, cost, BigDecimal::add);
                }
            }
        }

        for (String isin : isinToName.keySet()) {
            BigDecimal units     = isinToUnits.getOrDefault(isin, BigDecimal.ZERO);
            BigDecimal totalCost = isinToTotalCost.getOrDefault(isin, BigDecimal.ZERO);
            String     name      = isinToName.get(isin);

            if (units.compareTo(BigDecimal.ZERO) == 0) {
                warnings.add("Skipped " + name + " — zero or missing closing balance.");
                continue;
            }

            BigDecimal avgCost = totalCost.compareTo(BigDecimal.ZERO) > 0
                    && units.compareTo(BigDecimal.ZERO) > 0
                    ? totalCost.divide(units, 6, RoundingMode.HALF_UP)
                    : null;

            out.add(ParsedMFHolding.builder()
                    .isin(isin)
                    .schemeName(name)
                    .schemeCode(isinToSchemeCode.get(isin))
                    .units(units)
                    .avgCost(avgCost)
                    .nav(isinToNav.get(isin))
                    .build());
        }
    }

    // Tries in-parentheses format first (most CAMS), then falls back to explicit ISIN label
    private String detectIsin(String line) {
        Matcher m1 = ISIN_IN_PARENS.matcher(line);
        if (m1.find()) return m1.group(1);
        Matcher m2 = ISIN_LABEL.matcher(line);
        if (m2.find()) return m2.group(1);
        return null;
    }

    // Strips parenthetical ISIN and common plan/option suffixes for cleaner AMFI lookup
    private String extractSchemeName(String line, String isin) {
        // Remove parenthesised ISIN
        String name = line.replaceAll("\\(" + isin + "\\)", "")
                          .replaceAll("(?i)ISIN\\s*[:\\-]\\s*" + isin, "")
                          .trim();
        // Normalise suffix variants so AMFI lookup resolves correctly
        for (String suffix : SCHEME_SUFFIXES) {
            name = name.replaceAll(suffix, "");
        }
        return name.replaceAll("\\s{2,}", " ").trim();
    }

    private LocalDate extractStatementDate(String text) {
        Matcher m = DATE_CONTEXT.matcher(text);
        while (m.find()) {
            String ds = m.group(1);
            for (DateTimeFormatter fmt : DATE_FMTS) {
                try { return LocalDate.parse(ds, fmt); } catch (Exception ignored) {}
            }
        }
        return null;
    }

    /** Returns the first positive number in the string (used for closing units). */
    private BigDecimal extractFirstPositiveNumber(String text) {
        Matcher m = NUM_PATTERN.matcher(text);
        while (m.find()) {
            try {
                BigDecimal v = new BigDecimal(m.group().replace(",", ""));
                if (v.compareTo(BigDecimal.ZERO) > 0) return v;
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    /** Returns the rightmost number in the string (the total on a value line). */
    private BigDecimal extractLastNumber(String text) {
        Matcher m = NUM_PATTERN.matcher(text);
        BigDecimal last = null;
        while (m.find()) {
            try { last = new BigDecimal(m.group().replace(",", "")); }
            catch (NumberFormatException ignored) {}
        }
        return last;
    }
}
