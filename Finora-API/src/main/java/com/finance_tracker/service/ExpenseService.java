package com.finance_tracker.service;

import com.finance_tracker.exception.ResourceNotFoundException;
import com.finance_tracker.model.Expense;
import com.finance_tracker.repository.ExpenseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseService {
    private final ExpenseRepository expenseRepository;
    private final LedgerService ledgerService;

    private Long resolveUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        try {
            return Long.parseLong(auth.getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void validateOwnership(Long resourceUserId, Long requestingUserId) {
        if (resourceUserId != null && requestingUserId != null
                && !resourceUserId.equals(requestingUserId)) {
            throw new ResourceNotFoundException("Expense not found");
        }
    }

    public Page<Expense> getAllExpenses(Pageable pageable) {
        Long userId = resolveUserId();
        return expenseRepository.findByUserId(userId, pageable);
    }

    public List<Expense> getAllExpenses() {
        Long userId = resolveUserId();
        return expenseRepository.findByUserId(userId);
    }

    public Expense getExpenseById(Long id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", id));
        validateOwnership(expense.getUserId(), resolveUserId());
        return expense;
    }

    public Expense saveExpense(Expense expense) {
        Long userId = resolveUserId();
        if (expense.getDate() == null) {
            expense.setDate(LocalDate.now());
        }
        if (expense.getId() != null) {
            Expense before = expenseRepository.findById(expense.getId()).orElse(null);
            if (before != null) validateOwnership(before.getUserId(), userId);
            expense.setUserId(userId);
            Expense saved = expenseRepository.save(expense);
            ledgerService.recordEvent("EXPENSE", String.valueOf(saved.getId()), "UPDATE", before, saved, String.valueOf(userId));
            return saved;
        }
        expense.setUserId(userId);
        Expense saved = expenseRepository.save(expense);
        ledgerService.recordEvent("EXPENSE", String.valueOf(saved.getId()), "CREATE", null, saved, String.valueOf(userId));
        return saved;
    }

    public void deleteExpense(Long id) {
        Long userId = resolveUserId();
        Expense before = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", id));
        validateOwnership(before.getUserId(), userId);
        expenseRepository.deleteById(id);
        ledgerService.recordEvent("EXPENSE", String.valueOf(id), "DELETE", before, null, String.valueOf(userId));
    }

    public List<Expense> getExpensesBetweenDates(LocalDate startDate, LocalDate endDate) {
        Long userId = resolveUserId();
        return expenseRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
    }

    public List<Expense> getExpensesByCategory(String category) {
        Long userId = resolveUserId();
        return expenseRepository.findByUserIdAndCategory(userId, category);
    }

    // Fixed N+1 query problem - using database aggregation
    public BigDecimal getTotalExpenses(LocalDate startDate, LocalDate endDate) {
        Long userId = resolveUserId();
        BigDecimal total = expenseRepository.sumExpensesByUserIdBetweenDates(userId, startDate, endDate);
        return total != null ? total : BigDecimal.ZERO;
    }

    // Fixed N+1 query problem - using database aggregation
    public Map<String, BigDecimal> getExpensesByCategory(LocalDate startDate, LocalDate endDate) {
        Long userId = resolveUserId();
        List<Object[]> results = expenseRepository.sumExpensesByUserIdAndCategoryBetweenDates(userId, startDate, endDate);
        Map<String, BigDecimal> categoryMap = new HashMap<>();
        for (Object[] result : results) {
            String category = (String) result[0];
            BigDecimal total = (BigDecimal) result[1];
            categoryMap.put(category, total);
        }
        return categoryMap;
    }

    public BigDecimal getAverageMonthlyExpense(String category) {
        Long userId = resolveUserId();
        LocalDate today = LocalDate.now();
        LocalDate sixMonthsAgo = today.minusMonths(6);

        BigDecimal total;
        if (category != null && !category.isEmpty()) {
            List<Expense> expenses = expenseRepository.findByUserIdAndCategoryAndDateBetween(userId, category, sixMonthsAgo, today);
            total = expenses.stream()
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } else {
            total = expenseRepository.sumExpensesByUserIdBetweenDates(userId, sixMonthsAgo, today);
            if (total == null) {
                total = BigDecimal.ZERO;
            }
        }

        return total.divide(new BigDecimal("6"), 2, java.math.RoundingMode.HALF_UP);
    }

    @Transactional
    public int bulkDelete(List<Long> ids) {
        Long userId = resolveUserId();
        int count = 0;
        for (Long id : ids) {
            Expense expense = expenseRepository.findById(id).orElse(null);
            if (expense == null) continue;
            validateOwnership(expense.getUserId(), userId);
            expenseRepository.deleteById(id);
            ledgerService.recordEvent("EXPENSE", String.valueOf(id), "DELETE", expense, null, String.valueOf(userId));
            count++;
        }
        return count;
    }

    @Transactional
    public int bulkUpdate(List<Long> ids, String category, String paymentMethod) {
        Long userId = resolveUserId();
        int count = 0;
        for (Long id : ids) {
            Expense expense = expenseRepository.findById(id).orElse(null);
            if (expense == null) continue;
            validateOwnership(expense.getUserId(), userId);
            Expense before = new Expense();
            before.setId(expense.getId());
            before.setDescription(expense.getDescription());
            before.setAmount(expense.getAmount());
            before.setDate(expense.getDate());
            before.setCategory(expense.getCategory());
            before.setPaymentMethod(expense.getPaymentMethod());
            before.setUserId(expense.getUserId());
            if (category != null && !category.isBlank()) expense.setCategory(category);
            if (paymentMethod != null && !paymentMethod.isBlank()) expense.setPaymentMethod(paymentMethod);
            expenseRepository.save(expense);
            ledgerService.recordEvent("EXPENSE", String.valueOf(id), "UPDATE", before, expense, String.valueOf(userId));
            count++;
        }
        return count;
    }
}
