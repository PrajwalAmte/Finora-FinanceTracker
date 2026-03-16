package com.finance_tracker.service.expense;

import com.finance_tracker.dto.expense.ExpenseImportRequest;
import com.finance_tracker.dto.expense.ExpenseImportResultDTO;
import com.finance_tracker.dto.expense.ExpensePreviewDTO;
import com.finance_tracker.exception.StatementParseException;
import com.finance_tracker.model.Expense;
import com.finance_tracker.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ExpenseImportService {

    private static final Logger log = LoggerFactory.getLogger(ExpenseImportService.class);

    private final BankStatementParser parser;
    private final ExpenseService expenseService;

    public ExpensePreviewDTO preview(MultipartFile file) {
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            throw new StatementParseException("Failed to read file: " + e.getMessage(), e);
        }
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "statement.csv";
        return parser.parse(bytes, fileName);
    }

    @Transactional
    public ExpenseImportResultDTO confirmImport(ExpenseImportRequest request) {
        int imported = 0;
        int skipped = 0;

        for (ExpenseImportRequest.ExpenseEntry entry : request.getExpenses()) {
            try {
                if (entry.getAmount() == null || entry.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    skipped++;
                    continue;
                }
                if (entry.getDescription() == null || entry.getDescription().isBlank()) {
                    skipped++;
                    continue;
                }
                if (entry.getCategory() == null || entry.getCategory().isBlank()) {
                    skipped++;
                    continue;
                }

                Expense expense = new Expense();
                expense.setDescription(entry.getDescription());
                expense.setAmount(entry.getAmount());
                expense.setCategory(entry.getCategory());
                expense.setPaymentMethod(
                        entry.getPaymentMethod() != null && !entry.getPaymentMethod().isBlank()
                                ? entry.getPaymentMethod() : "Net Banking");

                LocalDate date = null;
                if (entry.getDate() != null && !entry.getDate().isBlank()) {
                    try {
                        date = LocalDate.parse(entry.getDate());
                    } catch (Exception ignored) {}
                }
                expense.setDate(date != null ? date : LocalDate.now());

                expenseService.saveExpense(expense);
                imported++;
            } catch (Exception e) {
                log.warn("Failed to import expense: {}", e.getMessage());
                skipped++;
            }
        }

        log.info("Expense import: imported={}, skipped={}", imported, skipped);
        return ExpenseImportResultDTO.builder()
                .imported(imported)
                .skipped(skipped)
                .build();
    }
}
