package com.finance_tracker.service.statement;

import com.finance_tracker.dto.statement.ParsedHolding;
import com.finance_tracker.dto.statement.ParsedMFHolding;
import com.finance_tracker.dto.statement.StatementPreviewDTO;
import com.finance_tracker.exception.StatementParseException;
import com.finance_tracker.model.InvestmentType;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses CAS (Consolidated Account Statements) from CDSL and NSDL.
 * Password format: {@code <PAN_lowercase><DDMMYYYY>} e.g. {@code abcde1234f01011990}
 * Handles CDSL tabular layout (inline) and NSDL multi-line layout via ±8-line window scan.
 */
@Service
public class CasStatementParser implements StatementParser {

    private static final Logger log = LoggerFactory.getLogger(CasStatementParser.class);

    // Standard 12-char Indian ISIN: country code (IN) + 10 alphanumeric chars
    private static final Pattern ISIN_PATTERN =
            Pattern.compile("\\b(IN[A-Z0-9]{10})\\b");

    // Number pattern — handles comma-separated values
    private static final Pattern NUM_PATTERN =
            Pattern.compile("[\\d,]+(?:\\.\\d+)?");

    // Date-context pattern: looks for labelled date expressions
    private static final Pattern DATE_CONTEXT =
            Pattern.compile(
                    "(?i)(?:as\\s+on|as\\s+of|statement\\s+date|closing\\s+date|period)\\s*[:\\-]?\\s*"
                            + "(\\d{1,2}[/\\-](?:\\d{1,2}|[A-Za-z]{3})[/\\-]\\d{2,4})");

    private static final List<DateTimeFormatter> DATE_FMTS = List.of(
            DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("d-MMM-yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd/MM/yy"));

    private static final int WINDOW = 8;

    @Override
    public StatementPreviewDTO parse(byte[] fileBytes, String password, String statementType)
            throws StatementParseException {

        String rawText = decryptAndExtract(fileBytes, password);
        String[] lines = rawText.split("\\r?\\n");

        List<ParsedHolding>   holdings   = new ArrayList<>();
        List<ParsedMFHolding> mfHoldings = new ArrayList<>();
        List<String>          warnings   = new ArrayList<>();
        Set<String>           seenIsins  = new HashSet<>();

        LocalDate statementDate = extractStatementDate(rawText);

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            Matcher m = ISIN_PATTERN.matcher(line);
            if (!m.find()) continue;

            String isin = m.group(1);

            if (seenIsins.contains(isin)) {
                log.debug("CAS: duplicate ISIN {} (multiple DP sections) — skipped", isin);
                continue;
            }
            seenIsins.add(isin);

            HoldingRecord rec = extractRecord(lines, i, isin, statementType);

            // Skip N/A value (delisted / suspended / pledged)
            if (rec.isNaValue) {
                warnings.add("Skipped " + isin + " (" + rec.name + ") — market value is N/A "
                        + "(may be delisted, suspended, or pledged).");
                continue;
            }

            // Skip zero balance
            if (rec.quantity != null && rec.quantity.compareTo(BigDecimal.ZERO) == 0) {
                warnings.add("Skipped " + isin + " (" + rec.name + ") — zero balance.");
                continue;
            }

            // Could not parse quantity at all
            if (rec.quantity == null) {
                warnings.add("Skipped " + isin + " (" + rec.name + ") — quantity could not be "
                        + "parsed; the CAS layout may be non-standard.");
                continue;
            }

            if (isin.startsWith("INF")) {
                buildMfHolding(isin, rec, mfHoldings, warnings);
            } else {
                buildEquityHolding(isin, rec, holdings, warnings);
            }
        }

        if (seenIsins.isEmpty()) {
            warnings.add("No ISINs were found in the statement. Please verify the file is a "
                    + "valid CAS PDF and the password is correct.");
        } else if (holdings.isEmpty() && mfHoldings.isEmpty()) {
            warnings.add("ISINs were detected but no holdings could be fully extracted. "
                    + "This may be an unsupported CAS layout variant.");
        }

        return StatementPreviewDTO.builder()
                .holdings(holdings)
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
            stripper.setSortByPosition(true);   // Preserve reading order across columns

            String text = stripper.getText(doc);
            doc.close();
            return text;
        } catch (IOException e) {
            String msg = e.getMessage() != null && e.getMessage().toLowerCase().contains("encrypt")
                    ? "Wrong password or encrypted CAS PDF could not be opened. "
                      + "CAS password format: PAN in lowercase + date of birth as DDMMYYYY "
                      + "(e.g. abcde1234f01011990)"
                    : "Could not read the CAS PDF — file may be corrupted. " + e.getMessage();
            throw new StatementParseException(msg, e);
        }
    }

    private static class HoldingRecord {
        String      name        = "";
        BigDecimal  quantity;
        BigDecimal  marketValue;
        BigDecimal  avgCostPerUnit;
        boolean     isNaValue;
        String      importSource;
    }

    // Combines same-line extraction (CDSL tabular) with window scan fallback (NSDL multi-line)
    private HoldingRecord extractRecord(String[] lines, int isinIdx, String isin,
                                         String statementType) {
        HoldingRecord rec = new HoldingRecord();
        rec.importSource = statementType;

        String isinLine    = lines[isinIdx].trim();
        String remainder   = isinLine.replace(isin, "").trim();
        List<BigDecimal> lineNums = extractAllNumbers(remainder);
        String textPrefix  = extractLeadingText(remainder);

        // Same-line extraction: CDSL tabular format has Name + ISIN + Qty + Value on one line
        if (!textPrefix.isBlank() && textPrefix.length() > 3) {
            rec.name = cleanName(textPrefix);
        }
        if (lineNums.size() >= 2) {
            rec.quantity    = lineNums.get(lineNums.size() - 2);
            rec.marketValue = lineNums.get(lineNums.size() - 1);
        } else if (lineNums.size() == 1) {
            rec.quantity = lineNums.get(0);
        }

        // Window scan for NSDL multi-line layout and any supplementary data
        int start = Math.max(0, isinIdx - WINDOW);
        int end   = Math.min(lines.length - 1, isinIdx + WINDOW);

        for (int j = start; j <= end; j++) {
            String raw  = lines[j].trim();
            String low  = raw.toLowerCase();

            if (raw.contains("N.A.") || raw.contains("N/A")) {
                rec.isNaValue = true;
            }

            if (rec.name.isBlank() && j != isinIdx
                    && !ISIN_PATTERN.matcher(raw).find()
                    && raw.length() > 4
                    && !raw.matches("^[\\d,. %|/:\\-]+$")) {
                rec.name = cleanName(raw);
            }

            // Quantity keywords: NSDL uses "Balance", CDSL uses "Free Balance"
            if (rec.quantity == null
                    && (low.contains("balance") || low.contains("free bal")
                        || low.contains("quantity") || low.contains("units held"))) {
                BigDecimal q = extractLastNumber(raw);
                if (q != null && q.compareTo(BigDecimal.ZERO) >= 0) {
                    rec.quantity = q;
                }
            }

            if (rec.marketValue == null
                    && (low.contains("market value") || low.contains("mkt val")
                        || low.contains("current value"))) {
                rec.marketValue = extractLastNumber(raw);
            }

            if (rec.avgCostPerUnit == null
                    && (low.contains("avg") || low.contains("purchase cost")
                        || low.contains("invested"))) {
                BigDecimal v = extractLastNumber(raw);
                if (v != null && v.compareTo(BigDecimal.ZERO) > 0) {
                    rec.avgCostPerUnit = v;
                }
            }
        }

        // Derive per-unit cost from market value when not explicitly present
        if (rec.avgCostPerUnit == null && rec.marketValue != null
                && rec.quantity != null && rec.quantity.compareTo(BigDecimal.ZERO) > 0) {
            rec.avgCostPerUnit = rec.marketValue.divide(rec.quantity, 6, RoundingMode.HALF_UP);
        }

        if (rec.name.isBlank()) {
            rec.name = isin;
        }

        return rec;
    }

    private void buildMfHolding(String isin, HoldingRecord rec,
                                  List<ParsedMFHolding> out, List<String> warnings) {
        // Compute current NAV from market value / units
        BigDecimal nav = rec.marketValue != null
                && rec.quantity != null
                && rec.quantity.compareTo(BigDecimal.ZERO) > 0
                ? rec.marketValue.divide(rec.quantity, 6, RoundingMode.HALF_UP)
                : null;

        if (rec.avgCostPerUnit != null
                && rec.avgCostPerUnit.compareTo(BigDecimal.ZERO) == 0) {
            warnings.add(isin + " (" + rec.name + ") — zero cost; may be bonus units. Included.");
        }

        out.add(ParsedMFHolding.builder()
                .isin(isin)
                .schemeName(rec.name)
                .schemeCode(null)  // resolved by StatementImportService
                .units(rec.quantity)
                .avgCost(rec.avgCostPerUnit)
                .nav(nav)
                .build());
    }

    private void buildEquityHolding(String isin, HoldingRecord rec,
                                      List<ParsedHolding> out, List<String> warnings) {
        if (rec.avgCostPerUnit != null
                && rec.avgCostPerUnit.compareTo(BigDecimal.ZERO) == 0) {
            warnings.add(isin + " (" + rec.name + ") — zero average cost; "
                    + "may be bonus shares or corporate action. Included.");
        }

        out.add(ParsedHolding.builder()
                .isin(isin)
                .name(rec.name)
                .symbol(null)  // CAS does not expose exchange ticker symbols
                .quantity(rec.quantity)
                .avgCost(rec.avgCostPerUnit)
                .importSource(rec.importSource)
                .detectedType(detectEquityType(isin, rec.name))
                .build());
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

    private List<BigDecimal> extractAllNumbers(String text) {
        List<BigDecimal> result = new ArrayList<>();
        Matcher m = NUM_PATTERN.matcher(text);
        while (m.find()) {
            try { result.add(new BigDecimal(m.group().replace(",", ""))); }
            catch (NumberFormatException ignored) {}
        }
        return result;
    }

    private BigDecimal extractLastNumber(String text) {
        Matcher m = NUM_PATTERN.matcher(text);
        BigDecimal last = null;
        while (m.find()) {
            try { last = new BigDecimal(m.group().replace(",", "")); }
            catch (NumberFormatException ignored) {}
        }
        return last;
    }

    // Returns the leading non-numeric text: "RELIANCE INDUSTRIES 10 24500.00" → "RELIANCE INDUSTRIES"
    private String extractLeadingText(String text) {
        return text.replaceAll("[\\d,. ]+$", "")
                   .replaceAll("\\s{2,}", " ")
                   .trim();
    }

    private String cleanName(String raw) {
        return raw.replaceAll("[|\\-]+\\s*$", "")
                  .replaceAll("\\s{2,}", " ")
                  .trim();
    }

    // Infers type from ISIN prefix and name keywords; CAS has no explicit type column
    private InvestmentType detectEquityType(String isin, String name) {
        String n = name.toLowerCase();
        if (n.contains(" etf") || n.contains("exchange traded fund")) {
            return InvestmentType.ETF;
        }
        if (n.contains("bond") || n.contains("debenture") || n.contains(" ncd")
                || n.contains("gilt") || n.contains("sgb")) {
            return InvestmentType.BOND;
        }
        // IN0 / ING prefixes are government / sovereign bonds
        if (isin.startsWith("IN0") || isin.startsWith("ING")) {
            return InvestmentType.BOND;
        }
        // INE is the standard equity ISIN prefix in India
        return InvestmentType.STOCK;
    }
}
