package com.finance_tracker.service.expense;

import com.finance_tracker.dto.expense.ExpensePreviewDTO;
import com.finance_tracker.dto.expense.ParsedTransaction;
import com.finance_tracker.exception.StatementParseException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
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

@Service
public class BankStatementParser {

    private static final Logger log = LoggerFactory.getLogger(BankStatementParser.class);
    private static final int MAX_HEADER_SCAN = 30;

    private static final List<String> DATE_HEADERS = List.of(
            "date", "txn date", "transaction date", "value date", "posting date", "trans date");
    private static final List<String> NARRATION_HEADERS = List.of(
            "narration", "description", "particulars", "details",
            "transaction details", "remark", "remarks", "transaction remarks");
    private static final List<String> DEBIT_HEADERS = List.of(
            "debit", "withdrawal", "withdrawals", "debit amount", "dr", "dr amount");
    private static final List<String> CREDIT_HEADERS = List.of(
            "credit", "deposit", "deposits", "credit amount", "cr", "cr amount");
    private static final List<String> AMOUNT_HEADERS = List.of(
            "amount", "txn amount", "transaction amount");
    private static final List<String> BALANCE_HEADERS = List.of(
            "balance", "closing balance", "running balance");
    private static final List<String> TYPE_HEADERS = List.of(
            "type", "dr/cr", "cr/dr", "transaction type");

    private static final Pattern DATE_PATTERN = Pattern.compile(
            "\\d{1,4}[/\\-.]\\d{1,2}[/\\-.]\\d{1,4}");

    public ExpensePreviewDTO parse(byte[] data, String fileName) {
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        if ("csv".equals(ext)) {
            return parseCsv(data);
        }
        return parseExcel(data);
    }

    private ExpensePreviewDTO parseCsv(byte[] data) {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(data), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                rows.add(splitCsvLine(line));
            }
        } catch (IOException e) {
            throw new StatementParseException("Failed to read CSV: " + e.getMessage(), e);
        }
        return processRows(rows);
    }

    private ExpensePreviewDTO parseExcel(byte[] data) {
        List<String[]> rows = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(data))) {
            Sheet sheet = wb.getSheetAt(0);
            for (Row row : sheet) {
                String[] cells = new String[row.getLastCellNum() < 0 ? 0 : row.getLastCellNum()];
                for (int i = 0; i < cells.length; i++) {
                    Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    cells[i] = cellToString(cell);
                }
                rows.add(cells);
            }
        } catch (Exception e) {
            throw new StatementParseException("Failed to read Excel: " + e.getMessage(), e);
        }
        return processRows(rows);
    }

    private ExpensePreviewDTO processRows(List<String[]> rows) {
        int headerIdx = -1;
        Map<String, Integer> colMap = new LinkedHashMap<>();

        for (int i = 0; i < Math.min(rows.size(), MAX_HEADER_SCAN); i++) {
            colMap = detectColumns(rows.get(i));
            if (colMap.containsKey("date") && colMap.containsKey("narration")
                    && (colMap.containsKey("debit") || colMap.containsKey("amount"))) {
                headerIdx = i;
                break;
            }
        }

        if (headerIdx < 0) {
            throw new StatementParseException(
                    "Could not detect column headers. Expected columns: Date, Narration/Description, Debit/Amount");
        }

        List<ParsedTransaction> transactions = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int debits = 0, credits = 0;
        String bankName = guessBankName(rows, headerIdx);

        for (int i = headerIdx + 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            try {
                String dateStr = safeGet(row, colMap.get("date"));
                if (dateStr == null || dateStr.isBlank()) continue;
                if (!DATE_PATTERN.matcher(dateStr).find()) continue;

                String narration = safeGet(row, colMap.get("narration"));
                if (narration == null) narration = "";

                BigDecimal debit = parseBd(safeGet(row, colMap.get("debit")));
                BigDecimal credit = parseBd(safeGet(row, colMap.get("credit")));
                BigDecimal amount = parseBd(safeGet(row, colMap.get("amount")));
                BigDecimal balance = parseBd(safeGet(row, colMap.get("balance")));
                String type = safeGet(row, colMap.get("type"));

                BigDecimal txnAmount;
                String txnType;

                if (debit != null && debit.compareTo(BigDecimal.ZERO) > 0) {
                    txnAmount = debit;
                    txnType = "DEBIT";
                    debits++;
                } else if (credit != null && credit.compareTo(BigDecimal.ZERO) > 0) {
                    txnAmount = credit;
                    txnType = "CREDIT";
                    credits++;
                } else if (amount != null && amount.compareTo(BigDecimal.ZERO) != 0) {
                    if (type != null && type.toUpperCase().contains("CR")) {
                        txnType = "CREDIT";
                        txnAmount = amount.abs();
                        credits++;
                    } else if (type != null && type.toUpperCase().contains("DR")) {
                        txnType = "DEBIT";
                        txnAmount = amount.abs();
                        debits++;
                    } else if (amount.compareTo(BigDecimal.ZERO) < 0) {
                        txnType = "DEBIT";
                        txnAmount = amount.abs();
                        debits++;
                    } else {
                        txnType = "CREDIT";
                        txnAmount = amount;
                        credits++;
                    }
                } else {
                    continue;
                }

                transactions.add(ParsedTransaction.builder()
                        .date(normalizeDate(dateStr))
                        .narration(narration.trim())
                        .amount(txnAmount)
                        .type(txnType)
                        .balance(balance)
                        .build());
            } catch (Exception e) {
                warnings.add("Row " + (i + 1) + ": " + e.getMessage());
            }
        }

        if (transactions.isEmpty()) {
            throw new StatementParseException("No transactions found in statement");
        }

        return ExpensePreviewDTO.builder()
                .transactions(transactions)
                .warnings(warnings)
                .bankName(bankName)
                .totalDebits(debits)
                .totalCredits(credits)
                .build();
    }

    private Map<String, Integer> detectColumns(String[] headers) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String h = headers[i] == null ? "" : headers[i].trim().toLowerCase()
                    .replaceAll("[\\s]+", " ");
            if (map.get("date") == null && matchesAny(h, DATE_HEADERS)) map.put("date", i);
            else if (map.get("narration") == null && matchesAny(h, NARRATION_HEADERS)) map.put("narration", i);
            else if (map.get("debit") == null && matchesAny(h, DEBIT_HEADERS)) map.put("debit", i);
            else if (map.get("credit") == null && matchesAny(h, CREDIT_HEADERS)) map.put("credit", i);
            else if (map.get("amount") == null && matchesAny(h, AMOUNT_HEADERS)) map.put("amount", i);
            else if (map.get("balance") == null && matchesAny(h, BALANCE_HEADERS)) map.put("balance", i);
            else if (map.get("type") == null && matchesAny(h, TYPE_HEADERS)) map.put("type", i);
        }
        return map;
    }

    private boolean matchesAny(String header, List<String> candidates) {
        for (String c : candidates) {
            if (header.contains(c)) return true;
        }
        return false;
    }

    private String guessBankName(List<String[]> rows, int headerIdx) {
        for (int i = 0; i < Math.min(headerIdx, 10); i++) {
            String joined = String.join(" ", rows.get(i)).toLowerCase();
            if (joined.contains("hdfc")) return "HDFC Bank";
            if (joined.contains("icici")) return "ICICI Bank";
            if (joined.contains("sbi") || joined.contains("state bank")) return "SBI";
            if (joined.contains("axis")) return "Axis Bank";
            if (joined.contains("kotak")) return "Kotak Mahindra Bank";
            if (joined.contains("indusind")) return "IndusInd Bank";
            if (joined.contains("yes bank")) return "YES Bank";
            if (joined.contains("idfc")) return "IDFC First Bank";
            if (joined.contains("bob") || joined.contains("bank of baroda")) return "Bank of Baroda";
            if (joined.contains("pnb") || joined.contains("punjab national")) return "PNB";
            if (joined.contains("canara")) return "Canara Bank";
            if (joined.contains("union bank")) return "Union Bank";
            if (joined.contains("federal")) return "Federal Bank";
            if (joined.contains("rbl")) return "RBL Bank";
        }
        return "Unknown";
    }

    private String normalizeDate(String raw) {
        raw = raw.trim();
        String[] parts;
        if (raw.contains("/")) parts = raw.split("/");
        else if (raw.contains("-")) parts = raw.split("-");
        else if (raw.contains(".")) parts = raw.split("\\.");
        else return raw;

        if (parts.length < 3) return raw;

        String p0 = parts[0].trim();
        String p1 = parts[1].trim();
        String p2 = parts[2].trim();
        if (p2.length() > 4) p2 = p2.substring(0, 4);

        if (p0.length() == 4) {
            return p0 + "-" + padTwo(p1) + "-" + padTwo(p2);
        }
        if (p2.length() == 4) {
            return p2 + "-" + padTwo(p1) + "-" + padTwo(p0);
        }
        if (p2.length() == 2) {
            String year = Integer.parseInt(p2) > 50 ? "19" + p2 : "20" + p2;
            return year + "-" + padTwo(p1) + "-" + padTwo(p0);
        }
        return raw;
    }

    private String padTwo(String v) {
        return v.length() == 1 ? "0" + v : v;
    }

    private BigDecimal parseBd(String s) {
        if (s == null || s.isBlank()) return null;
        s = s.replaceAll("[,₹$€£\\s]", "").trim();
        if (s.isEmpty() || s.equals("-") || s.equals("--")) return null;
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String safeGet(String[] arr, Integer idx) {
        if (idx == null || idx >= arr.length) return null;
        return arr[idx];
    }

    private String cellToString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    var d = cell.getLocalDateTimeCellValue();
                    yield d.toLocalDate().toString();
                }
                double v = cell.getNumericCellValue();
                if (v == Math.floor(v) && !Double.isInfinite(v)) {
                    yield String.valueOf((long) v);
                }
                yield String.valueOf(v);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    try {
                        yield cell.getStringCellValue();
                    } catch (Exception e2) {
                        yield "";
                    }
                }
            }
            default -> "";
        };
    }

    private String[] splitCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        result.add(sb.toString().trim());
        return result.toArray(new String[0]);
    }
}
