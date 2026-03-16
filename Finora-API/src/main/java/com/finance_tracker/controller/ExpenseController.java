package com.finance_tracker.controller;

import com.finance_tracker.dto.ExpenseRequestDTO;
import com.finance_tracker.dto.ExpenseResponseDTO;
import com.finance_tracker.dto.ExpenseSummaryDTO;
import com.finance_tracker.mapper.ExpenseMapper;
import com.finance_tracker.model.Expense;
import com.finance_tracker.service.ExpenseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private final ExpenseMapper expenseMapper;

    @GetMapping
    public List<ExpenseResponseDTO> getAllExpenses(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "1000") int size) {
        if (size > 1000) size = 1000;
        Pageable pageable = PageRequest.of(page, size);
        Page<Expense> expensePage = expenseService.getAllExpenses(pageable);
        return expenseMapper.toDTOList(expensePage.getContent());
    }

    @GetMapping("/{id}")
    public ExpenseResponseDTO getExpenseById(@PathVariable Long id) {
        Expense expense = expenseService.getExpenseById(id);
        return expenseMapper.toDTO(expense);
    }

    @PostMapping
    public ExpenseResponseDTO createExpense(@Valid @RequestBody ExpenseRequestDTO expenseDTO) {
        Expense expense = expenseMapper.toEntity(expenseDTO);
        Expense savedExpense = expenseService.saveExpense(expense);
        return expenseMapper.toDTO(savedExpense);
    }

    @PutMapping("/{id}")
    public ExpenseResponseDTO updateExpense(@PathVariable Long id, @Valid @RequestBody ExpenseRequestDTO expenseDTO) {
        expenseService.getExpenseById(id);
        
        Expense expense = expenseMapper.toEntity(expenseDTO);
        expense.setId(id);
        Expense updatedExpense = expenseService.saveExpense(expense);
        return expenseMapper.toDTO(updatedExpense);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        expenseService.deleteExpense(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<Map<String, Integer>> bulkDeleteExpenses(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.getOrDefault("ids", List.of());
        int deleted = expenseService.bulkDelete(ids);
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }

    @PutMapping("/bulk")
    public ResponseEntity<Map<String, Integer>> bulkUpdateExpenses(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Number> rawIds = (List<Number>) body.getOrDefault("ids", List.of());
        List<Long> ids = rawIds.stream().map(Number::longValue).toList();
        String category = (String) body.get("category");
        String paymentMethod = (String) body.get("paymentMethod");
        int updated = expenseService.bulkUpdate(ids, category, paymentMethod);
        return ResponseEntity.ok(Map.of("updated", updated));
    }

    @GetMapping("/by-date-range")
    public List<ExpenseResponseDTO> getExpensesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<Expense> expenses = expenseService.getExpensesBetweenDates(startDate, endDate);
        return expenseMapper.toDTOList(expenses);
    }

    @GetMapping("/by-category")
    public List<ExpenseResponseDTO> getExpensesByCategory(@RequestParam String category) {
        List<Expense> expenses = expenseService.getExpensesByCategory(category);
        return expenseMapper.toDTOList(expenses);
    }

    @GetMapping("/summary")
    public ExpenseSummaryDTO getExpenseSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }

        if (endDate == null) {
            endDate = LocalDate.now();
        }

        BigDecimal totalExpenses = expenseService.getTotalExpenses(startDate, endDate);
        var expensesByCategory = expenseService.getExpensesByCategory(startDate, endDate);

        return ExpenseSummaryDTO.builder()
                .totalExpenses(totalExpenses)
                .expensesByCategory(expensesByCategory)
                .build();
    }

    @GetMapping("/average-monthly")
    public BigDecimal getAverageMonthlyExpense(
            @RequestParam(required = false) String category) {
        return expenseService.getAverageMonthlyExpense(category);
    }
}

