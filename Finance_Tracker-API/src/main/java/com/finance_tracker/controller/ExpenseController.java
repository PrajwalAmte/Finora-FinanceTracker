package com.finance_tracker.controller;

import com.finance_tracker.model.Expense;
import com.finance_tracker.service.ExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {
    private final ExpenseService expenseService;

    @GetMapping
    public List<Expense> getAllExpenses() {
        return expenseService.getAllExpenses();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Expense> getExpenseById(@PathVariable Long id) {
        return expenseService.getExpenseById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Expense createExpense(@Valid @RequestBody Expense expense) {
        return expenseService.saveExpense(expense);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Expense> updateExpense(@PathVariable Long id, @Valid @RequestBody Expense expense) {
        return expenseService.getExpenseById(id)
                .map(existingExpense -> {
                    expense.setId(id);
                    return ResponseEntity.ok(expenseService.saveExpense(expense));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        return expenseService.getExpenseById(id)
                .map(expense -> {
                    expenseService.deleteExpense(id);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-date-range")
    public List<Expense> getExpensesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return expenseService.getExpensesBetweenDates(startDate, endDate);
    }

    @GetMapping("/by-category")
    public List<Expense> getExpensesByCategory(@RequestParam String category) {
        return expenseService.getExpensesByCategory(category);
    }

    @GetMapping("/summary")
    public ResponseEntity<Object> getExpenseSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1); // First day of current month
        }

        if (endDate == null) {
            endDate = LocalDate.now(); // Today
        }

        BigDecimal totalExpenses = expenseService.getTotalExpenses(startDate, endDate);
        Map<String, BigDecimal> expensesByCategory = expenseService.getExpensesByCategory(startDate, endDate);

        return ResponseEntity.ok(Map.of(
                "totalExpenses", totalExpenses,
                "expensesByCategory", expensesByCategory
        ));
    }

    @GetMapping("/average-monthly")
    public ResponseEntity<BigDecimal> getAverageMonthlyExpense(
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(expenseService.getAverageMonthlyExpense(category));
    }
}

