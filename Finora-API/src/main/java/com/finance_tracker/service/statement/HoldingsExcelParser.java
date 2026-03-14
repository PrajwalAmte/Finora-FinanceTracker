package com.finance_tracker.service.statement;

import com.finance_tracker.dto.statement.ParsedHolding;
import com.finance_tracker.dto.statement.ParsedMFHolding;
import com.finance_tracker.dto.statement.StatementPreviewDTO;
import com.finance_tracker.exception.StatementParseException;
import com.finance_tracker.model.InvestmentType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Generic broker holdings parser — supports .xlsx/.xls (Excel) and .csv.
 * Detects column positions by header name (case-insensitive substring match), so it survives
 * column reorders across broker export format updates.
 * Supported brokers: Zerodha, Groww, Upstox, HDFC Securities, ICICI Direct, Angel One, 5Paisa.
 */
@Service
public class HoldingsExcelParser implements StatementParser {

    private static final Logger log = LoggerFactory.getLogger(HoldingsExcelParser.class);

    // Standard 12-char Indian ISIN
    private static final Pattern ISIN_PATTERN = Pattern.compile("^IN[A-Z0-9]{10}$");

    // Max rows to scan looking for the header (Zerodha XLSX header is at row 23)
    private static final int MAX_HEADER_SCAN_ROWS = 30;

    // Column-name registries — any header containing one of these strings
    // (after lowercase + trim) will be mapped to the column role.
    // More specific strings must come BEFORE more general ones to avoid
    // false positives (e.g. "avg. cost" before bare "avg").

    private static final List<String> ISIN_HEADERS = List.of("isin");

    private static final List<String> QTY_HEADERS = List.of(
            "net qty", "net quantity", "quantity", "shares", "units", "balance", "qty");

    private static final List<String> AVG_COST_HEADERS = List.of(
            "avg. cost", "avg cost", "average cost", "average price",
            "cost/share", "cost per share",
            "avg buy price", "buy price", "purchase price", "avg price", "cost price");

    private static final List<String> NAME_HEADERS = List.of(
            "instrument", "scrip name", "security name", "stock name", "scheme name",
            "security", "scrip", "stock", "scheme", "name");

    private static final List<String> SYMBOL_HEADERS = List.of(
            "nse symbol", "bse symbol", "trading symbol", "symbol", "ticker");

    private static final List<String> LTP_HEADERS = List.of(
            "last traded price", "last price", "market price", "current price",
            "close price", "ltp", "cmp", "nav");

    private static final List<String> T1_HEADERS = List.of(
            "t1 holdings", "unsettled qty", "pending delivery", "t1 qty", "t+1", "t1");

    private static final List<String> TYPE_HEADERS = List.of(
            "instrument type", "asset type", "asset class", "type", "series");

    // Public API

    @Override
    public StatementPreviewDTO parse(byte[] fileBytes, String password, String statementType)
            throws StatementParseException {
        // Route CSV files separately — no POI needed
        if (statementType.endsWith("_CSV") || statementType.equals("CSV")) {
            return parseFromCsv(fileBytes, statementType);
        }
        Workbook workbook = openWorkbook(fileBytes);

        List<ParsedHolding>   holdings   = new ArrayList<>();
        List<ParsedMFHolding> mfHoldings = new ArrayList<>();
        List<String>          warnings   = new ArrayList<>();
        int t1Count = 0;

        // Parse every sheet that has the expected column structure.
        // Zerodha Console exports Equity and Mutual Funds as separate sheets in one workbook.
        for (int si = 0; si < workbook.getNumberOfSheets(); si++) {
            Sheet sheet = workbook.getSheetAt(si);
            ColumnMap cols;
            try {
                cols = detectColumns(sheet);
            } catch (StatementParseException e) {
                log.debug("Skipping sheet '{}': {}", sheet.getSheetName(), e.getMessage());
                continue;
            }
            log.debug("Parsing sheet '{}': headerRow={}, isin={}, qty={}, avgCost={}",
                    sheet.getSheetName(), cols.headerRow, cols.isinCol, cols.qtyCol, cols.avgCostCol);

            for (int r = cols.headerRow + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (isBlankRow(row)) continue;

                String isin = cols.isinCol >= 0 ? cellString(row, cols.isinCol) : null;
                if (isin != null) {
                    isin = isin.trim().toUpperCase();
                    if (!ISIN_PATTERN.matcher(isin).matches()) isin = null;
                }

                String symbol = cols.symbolCol >= 0 ? cellString(row, cols.symbolCol) : null;
                String name   = cols.nameCol   >= 0 ? cellString(row, cols.nameCol)   : null;
                if (name == null || name.isBlank()) name = symbol;
                if (name == null || name.isBlank()) name = isin;
                if (name == null || name.isBlank()) continue;

                BigDecimal qty = cellDecimal(row, cols.qtyCol);
                if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
                    warnings.add("Skipped " + name + " — zero or missing quantity.");
                    continue;
                }

                BigDecimal avgCost = cols.avgCostCol >= 0 ? cellDecimal(row, cols.avgCostCol) : null;
                BigDecimal ltp     = cols.ltpCol >= 0 ? cellDecimal(row, cols.ltpCol) : null;

                if (cols.t1Col >= 0) {
                    BigDecimal t1 = cellDecimal(row, cols.t1Col);
                    if (t1 != null && t1.compareTo(BigDecimal.ZERO) > 0) t1Count++;
                }

                InvestmentType type = resolveType(cols, row, isin, name);

                if (type == InvestmentType.MUTUAL_FUND) {
                    mfHoldings.add(ParsedMFHolding.builder()
                            .isin(isin).schemeName(name).schemeCode(null)
                            .units(qty).avgCost(avgCost).nav(ltp).build());
                } else {
                    holdings.add(ParsedHolding.builder()
                            .isin(isin).name(name).symbol(symbol)
                            .quantity(qty).avgCost(avgCost)
                            .importSource(statementType).detectedType(type).build());
                }
            }
        }

        aggregateDuplicates(holdings, warnings);
        aggregateMfDuplicates(mfHoldings, warnings);

        if (t1Count > 0)
            warnings.add(t1Count + " holding(s) may include unsettled T+1 delivery quantity.");

        if (holdings.isEmpty() && mfHoldings.isEmpty())
            warnings.add("No valid holdings found. Ensure the file is a broker Holdings export.");

        try { workbook.close(); } catch (IOException ignored) {}

        return StatementPreviewDTO.builder()
                .holdings(holdings).mfHoldings(mfHoldings)
                .warnings(warnings).statementDate(null).build();
    }

    private Workbook openWorkbook(byte[] fileBytes) throws StatementParseException {
        try {
            return WorkbookFactory.create(new ByteArrayInputStream(fileBytes));
        } catch (Exception e) {
            throw new StatementParseException(
                    "Could not open the Excel file. Please ensure it is a valid .xlsx or .xls "
                            + "broker Holdings export. "
                            + "Supported formats: Zerodha, Groww, Upstox, HDFC Securities, "
                            + "ICICI Direct, Angel One, 5Paisa.", e);
        }
    }

    // CSV parsing (Zerodha and other brokers that export holdings as CSV)

    private StatementPreviewDTO parseFromCsv(byte[] fileBytes, String statementType)
            throws StatementParseException {

        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(fileBytes), StandardCharsets.UTF_8))) {
            String l;
            while ((l = br.readLine()) != null) { if (!l.isBlank()) lines.add(l); }
        } catch (IOException e) {
            throw new StatementParseException("Failed to read CSV file: " + e.getMessage(), e);
        }
        if (lines.isEmpty()) throw new StatementParseException("The CSV file appears to be empty.");

        int isinIdx = -1, qtyIdx = -1, avgCostIdx = -1,
            nameIdx = -1, symbolIdx = -1, ltpIdx = -1, typeIdx = -1, headerLine = -1;

        for (int li = 0; li < Math.min(lines.size(), MAX_HEADER_SCAN_ROWS + 1); li++) {
            String[] cols = parseCsvLine(lines.get(li));
            int tIsin = -1, tQty = -1, tAvgCost = -1, tName = -1, tSymbol = -1, tLtp = -1, tType = -1;
            for (int i = 0; i < cols.length; i++) {
                String norm = cols[i].toLowerCase(java.util.Locale.ROOT).trim();
                if (norm.isEmpty()) continue;
                if      (tIsin    < 0 && matchAny(norm, ISIN_HEADERS))     tIsin    = i;
                else if (tName    < 0 && matchAny(norm, NAME_HEADERS))     tName    = i;
                else if (tSymbol  < 0 && matchAny(norm, SYMBOL_HEADERS))   tSymbol  = i;
                else if (tQty     < 0 && matchAny(norm, QTY_HEADERS))      tQty     = i;
                else if (tAvgCost < 0 && matchAny(norm, AVG_COST_HEADERS)) tAvgCost = i;
                else if (tLtp     < 0 && matchAny(norm, LTP_HEADERS))      tLtp     = i;
                else if (tType    < 0 && matchAny(norm, TYPE_HEADERS))     tType    = i;
            }
            if ((tIsin >= 0 || tName >= 0 || tSymbol >= 0) && tQty >= 0) {
                headerLine = li;
                isinIdx = tIsin; qtyIdx = tQty; avgCostIdx = tAvgCost;
                nameIdx = tName; symbolIdx = tSymbol; ltpIdx = tLtp; typeIdx = tType;
                break;
            }
        }

        if (headerLine < 0)
            throw new StatementParseException(
                    "Unrecognised CSV format — could not find Name/Instrument and Quantity columns.");

        List<ParsedHolding>   holdings   = new ArrayList<>();
        List<ParsedMFHolding> mfHoldings = new ArrayList<>();
        List<String>          warnings   = new ArrayList<>();
        boolean hasIsinCol = isinIdx >= 0;

        for (int li = headerLine + 1; li < lines.size(); li++) {
            String[] cols = parseCsvLine(lines.get(li));

            String isin = safeGet(cols, isinIdx);
            if (isin != null) {
                isin = isin.trim().toUpperCase();
                if (!ISIN_PATTERN.matcher(isin).matches()) isin = null;
            }

            String name = safeGet(cols, nameIdx);
            if (name == null || name.isBlank()) name = safeGet(cols, symbolIdx);
            if (name == null || name.isBlank()) continue;

            BigDecimal qty = parseCsvDecimal(safeGet(cols, qtyIdx));
            if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal avgCost = parseCsvDecimal(safeGet(cols, avgCostIdx));
            BigDecimal ltp     = parseCsvDecimal(safeGet(cols, ltpIdx));
            String symbol      = safeGet(cols, symbolIdx);
            String typeStr     = safeGet(cols, typeIdx);

            // When no ISIN/type column, equity symbols are all-uppercase with no spaces.
            InvestmentType type = (isin != null || typeStr != null)
                    ? resolveTypeFromStrings(typeStr, isin, name)
                    : (!name.contains(" ") && name.equals(name.toUpperCase(java.util.Locale.ROOT))
                            ? resolveTypeFromStrings(null, null, name)
                            : InvestmentType.MUTUAL_FUND);

            if (type == InvestmentType.MUTUAL_FUND) {
                mfHoldings.add(ParsedMFHolding.builder()
                        .isin(isin).schemeName(name).schemeCode(null)
                        .units(qty).avgCost(avgCost).nav(ltp).build());
            } else {
                holdings.add(ParsedHolding.builder()
                        .isin(isin).name(name).symbol(symbol != null ? symbol : name)
                        .quantity(qty).avgCost(avgCost)
                        .importSource(statementType).detectedType(type).build());
            }
        }

        aggregateDuplicates(holdings, warnings);
        aggregateMfDuplicates(mfHoldings, warnings);

        if (!hasIsinCol) {
            warnings.add("No ISIN column in this CSV — holdings imported by symbol/name. "
                    + "Use the Zerodha Console XLSX export for ISIN-based import.");
            if (!mfHoldings.isEmpty())
                warnings.add(mfHoldings.size() + " mutual fund holding(s) imported without ISIN. "
                        + "Use a CAS/CAMS statement for full MF import detail.");
        }

        if (holdings.isEmpty() && mfHoldings.isEmpty())
            warnings.add("No valid holdings found.");

        return StatementPreviewDTO.builder()
                .holdings(holdings).mfHoldings(mfHoldings)
                .warnings(warnings).statementDate(null).build();
    }

    /** Splits a CSV line, respecting double-quoted fields. */
    private static String[] parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuote && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"'); i++;    // escaped quote ""
                } else {
                    inQuote = !inQuote;
                }
            } else if (c == ',' && !inQuote) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim());
        return tokens.toArray(new String[0]);
    }

    private static String safeGet(String[] arr, int idx) {
        return (idx >= 0 && idx < arr.length) ? arr[idx] : null;
    }

    private static BigDecimal parseCsvDecimal(String s) {
        if (s == null || s.isBlank() || s.equals("-")) return null;
        try {
            return new BigDecimal(s.replace(",", "").replace("₹", "").replace("$", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private InvestmentType resolveTypeFromStrings(String typeStr, String isin, String name) {
        if (typeStr != null && !typeStr.isBlank()) {
            String tl = typeStr.toLowerCase();
            if (tl.contains("etf"))                            return InvestmentType.ETF;
            if (tl.contains("mutual") || tl.contains("mf"))   return InvestmentType.MUTUAL_FUND;
            if (tl.contains("bond")   || tl.contains("ncd")
                    || tl.contains("debt"))                    return InvestmentType.BOND;
            if (tl.contains("equity") || tl.contains("stock")) return InvestmentType.STOCK;
        }
        // ISIN prefix heuristics (guard against null ISIN in symbol-only imports)
        if (isin != null) {
            if (isin.startsWith("INF")) return InvestmentType.MUTUAL_FUND;
            if (isin.startsWith("IN0") || isin.startsWith("ING")) return InvestmentType.BOND;
        }
        if (name != null) {
            String nl = name.toLowerCase();
            if (nl.contains(" etf") || nl.contains("exchange traded fund") || nl.endsWith("etf"))
                return InvestmentType.ETF;
            if (nl.contains("bond") || nl.contains("debenture") || nl.contains(" ncd")
                    || nl.contains("gilt") || nl.contains("sgb")) return InvestmentType.BOND;
        }
        return InvestmentType.STOCK;
    }

    private static class ColumnMap {
        int headerRow  = 0;
        int isinCol    = -1;
        int qtyCol     = -1;
        int avgCostCol = -1;
        int nameCol    = -1;
        int symbolCol  = -1;
        int ltpCol     = -1;
        int t1Col      = -1;
        int typeCol    = -1;
    }

    private ColumnMap detectColumns(Sheet sheet) throws StatementParseException {
        ColumnMap cols = new ColumnMap();

        for (int r = 0; r <= Math.min(MAX_HEADER_SCAN_ROWS, sheet.getLastRowNum()); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            // Evaluate each row independently as a candidate header row
            int tIsin=-1, tQty=-1, tAvgCost=-1, tName=-1, tSymbol=-1, tLtp=-1, tT1=-1, tType=-1;
            for (Cell cell : row) {
                String hdr = cellString(cell);
                if (hdr == null || hdr.isBlank()) continue;
                String norm = hdr.toLowerCase(java.util.Locale.ROOT).trim();
                int col = cell.getColumnIndex();

                // Use first-match wins; lists are ordered most-specific to least-specific
                if      (tIsin    < 0 && matchAny(norm, ISIN_HEADERS))     tIsin    = col;
                else if (tName    < 0 && matchAny(norm, NAME_HEADERS))     tName    = col;
                else if (tSymbol  < 0 && matchAny(norm, SYMBOL_HEADERS))   tSymbol  = col;
                else if (tQty     < 0 && matchAny(norm, QTY_HEADERS))      tQty     = col;
                else if (tAvgCost < 0 && matchAny(norm, AVG_COST_HEADERS)) tAvgCost = col;
                else if (tLtp     < 0 && matchAny(norm, LTP_HEADERS))      tLtp     = col;
                else if (tT1      < 0 && matchAny(norm, T1_HEADERS))       tT1      = col;
                else if (tType    < 0 && matchAny(norm, TYPE_HEADERS))     tType    = col;
            }

            // Header row must have Quantity + at least one identifier (ISIN, Name, or Symbol)
            if (tQty >= 0 && (tIsin >= 0 || tName >= 0 || tSymbol >= 0)) {
                cols.headerRow  = r;
                cols.isinCol    = tIsin;
                cols.qtyCol     = tQty;
                cols.avgCostCol = tAvgCost;
                cols.nameCol    = tName;
                cols.symbolCol  = tSymbol;
                cols.ltpCol     = tLtp;
                cols.t1Col      = tT1;
                cols.typeCol    = tType;
                break;
            }
        }

        // Validate required columns
        List<String> missing = new ArrayList<>();
        if (cols.qtyCol < 0) missing.add("Quantity (Qty / Shares / Units)");
        if (cols.avgCostCol < 0) missing.add("Avg Cost (Avg. cost / Buy price / Purchase price)");
        if (cols.isinCol < 0 && cols.nameCol < 0 && cols.symbolCol < 0)
            missing.add("Instrument identifier (ISIN / Name / Symbol)");

        if (!missing.isEmpty()) {
            throw new StatementParseException(
                    "Unrecognised Holdings export format — could not find required columns: "
                            + String.join(", ", missing) + ". "
                            + "Supported brokers: Zerodha, Groww, Upstox, HDFC Securities, "
                            + "ICICI Direct, Angel One. "
                            + "Ensure you are exporting the Holdings report, not a P&L or "
                            + "transaction report.");
        }

        return cols;
    }

    private InvestmentType resolveType(ColumnMap cols, Row row, String isin, String name) {
        // Explicit type column (most reliable)
        if (cols.typeCol >= 0) {
            String t = cellString(row, cols.typeCol);
            if (t != null) {
                String tl = t.toLowerCase();
                if (tl.contains("etf"))                           return InvestmentType.ETF;
                if (tl.contains("mutual") || tl.contains("mf"))  return InvestmentType.MUTUAL_FUND;
                if (tl.contains("bond") || tl.contains("ncd")
                        || tl.contains("debt"))                   return InvestmentType.BOND;
                if (tl.contains("equity") || tl.contains("stock")) return InvestmentType.STOCK;
            }
        }
        // ISIN prefix heuristic (guard against null ISIN in symbol-only imports)
        if (isin != null && isin.startsWith("INF")) return InvestmentType.MUTUAL_FUND;
        if (isin != null && (isin.startsWith("IN0") || isin.startsWith("ING"))) return InvestmentType.BOND;

        // Name keyword heuristic
        String nl = name.toLowerCase();
        if (nl.contains(" etf") || nl.contains("exchange traded fund")) return InvestmentType.ETF;
        if (nl.contains("bond") || nl.contains("debenture") || nl.contains(" ncd")
                || nl.contains("gilt") || nl.contains("sgb"))            return InvestmentType.BOND;

        return InvestmentType.STOCK;
    }

    private void aggregateDuplicates(List<ParsedHolding> holdings, List<String> warnings) {
        Map<String, ParsedHolding> seen = new LinkedHashMap<>();
        List<ParsedHolding> merged = new ArrayList<>();

        for (ParsedHolding h : holdings) {
            String key = h.getIsin() != null ? h.getIsin() : h.getSymbol();
            if (key == null) { merged.add(h); continue; }

            if (!seen.containsKey(key)) {
                seen.put(key, h);
                merged.add(h);
            } else {
                ParsedHolding existing = seen.get(key);
                BigDecimal totalQty  = existing.getQuantity().add(h.getQuantity());
                BigDecimal totalCost = safeCostBasis(existing).add(safeCostBasis(h));
                BigDecimal newAvg    = totalQty.compareTo(BigDecimal.ZERO) > 0
                        ? totalCost.divide(totalQty, 6, java.math.RoundingMode.HALF_UP)
                        : existing.getAvgCost();

                merged.remove(existing);
                ParsedHolding mergedH = ParsedHolding.builder()
                        .isin(existing.getIsin())
                        .name(existing.getName())
                        .symbol(existing.getSymbol())
                        .quantity(totalQty)
                        .avgCost(newAvg)
                        .importSource(existing.getImportSource())
                        .detectedType(existing.getDetectedType())
                        .build();
                seen.put(key, mergedH);
                merged.add(mergedH);
                warnings.add(existing.getName() + " appeared in multiple rows — quantities merged.");
            }
        }
        holdings.clear();
        holdings.addAll(merged);
    }

    private void aggregateMfDuplicates(List<ParsedMFHolding> mfHoldings, List<String> warnings) {
        Map<String, ParsedMFHolding> seen = new LinkedHashMap<>();
        List<ParsedMFHolding> merged = new ArrayList<>();

        for (ParsedMFHolding h : mfHoldings) {
            String key = h.getIsin() != null ? h.getIsin() : h.getSchemeName();
            if (key == null) { merged.add(h); continue; }

            if (!seen.containsKey(key)) {
                seen.put(key, h);
                merged.add(h);
            } else {
                ParsedMFHolding existing = seen.get(key);
                BigDecimal totalUnits = existing.getUnits().add(h.getUnits());
                BigDecimal totalCost  = safeMfCostBasis(existing).add(safeMfCostBasis(h));
                BigDecimal newAvg     = totalUnits.compareTo(BigDecimal.ZERO) > 0
                        ? totalCost.divide(totalUnits, 6, java.math.RoundingMode.HALF_UP)
                        : existing.getAvgCost();

                merged.remove(existing);
                ParsedMFHolding mergedH = ParsedMFHolding.builder()
                        .isin(existing.getIsin())
                        .schemeName(existing.getSchemeName())
                        .schemeCode(existing.getSchemeCode())
                        .units(totalUnits)
                        .avgCost(newAvg)
                        .nav(existing.getNav() != null ? existing.getNav() : h.getNav())
                        .build();
                seen.put(key, mergedH);
                merged.add(mergedH);
                warnings.add(existing.getSchemeName() + " appeared in multiple rows — units merged.");
            }
        }
        mfHoldings.clear();
        mfHoldings.addAll(merged);
    }

    private BigDecimal safeCostBasis(ParsedHolding h) {
        if (h.getAvgCost() == null || h.getQuantity() == null) return BigDecimal.ZERO;
        return h.getAvgCost().multiply(h.getQuantity());
    }

    private BigDecimal safeMfCostBasis(ParsedMFHolding h) {
        if (h.getAvgCost() == null || h.getUnits() == null) return BigDecimal.ZERO;
        return h.getAvgCost().multiply(h.getUnits());
    }

    private boolean isBlankRow(Row row) {
        if (row == null) return true;
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String v = cellString(cell);
                if (v != null && !v.isBlank()) return false;
            }
        }
        return true;
    }

    private String cellString(Row row, int col) {
        if (col < 0 || row == null) return null;
        return cellString(row.getCell(col));
    }

    private String cellString(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double d = cell.getNumericCellValue();
                yield (d == Math.floor(d) && !Double.isInfinite(d))
                        ? String.valueOf((long) d)
                        : String.valueOf(d);
            }
            case FORMULA -> {
                try { yield String.valueOf(cell.getNumericCellValue()); }
                catch (Exception e) {
                    try { yield cell.getStringCellValue(); }
                    catch (Exception e2) { yield null; }
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default      -> null;
        };
    }

    private BigDecimal cellDecimal(Row row, int col) {
        if (col < 0 || row == null) return null;
        return cellDecimal(row.getCell(col));
    }

    private BigDecimal cellDecimal(Cell cell) {
        if (cell == null) return null;
        try {
            return switch (cell.getCellType()) {
                case NUMERIC  -> BigDecimal.valueOf(cell.getNumericCellValue());
                case FORMULA  -> BigDecimal.valueOf(cell.getNumericCellValue());
                case STRING   -> {
                    String s = cell.getStringCellValue().replace(",", "").replace("₹", "").trim();
                    yield (s.isEmpty() || s.equals("-")) ? null : new BigDecimal(s);
                }
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }

    private boolean matchAny(String header, List<String> variants) {
        for (String v : variants) {
            if (header.contains(v)) return true;
        }
        return false;
    }
}
