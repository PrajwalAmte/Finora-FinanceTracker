package com.finance_tracker.service;

import com.finance_tracker.exception.ResourceNotFoundException;
import com.finance_tracker.model.Expense;
import com.finance_tracker.repository.ExpenseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    public Page<Expense> getAllExpenses(Pageable pageable) {
        return expenseRepository.findAll(pageable);
    }

    public List<Expense> getAllExpenses() {
        return expenseRepository.findAll();
    }

    public Expense getExpenseById(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", id));
    }

    public Expense saveExpense(Expense expense) {
        if (expense.getDate() == null) {
            expense.setDate(LocalDate.now());
        }
        return expenseRepository.save(expense);
    }

    public void deleteExpense(Long id) {
        if (!expenseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Expense", id);
        }
        expenseRepository.deleteById(id);
    }

    public List<Expense> getExpensesBetweenDates(LocalDate startDate, LocalDate endDate) {
        return expenseRepository.findByDateBetween(startDate, endDate);
    }

    public List<Expense> getExpensesByCategory(String category) {
        return expenseRepository.findByCategory(category);
    }

    // Fixed N+1 query problem - using database aggregation
    public BigDecimal getTotalExpenses(LocalDate startDate, LocalDate endDate) {
        BigDecimal total = expenseRepository.sumExpensesBetweenDates(startDate, endDate);
        return total != null ? total : BigDecimal.ZERO;
    }

    // Fixed N+1 query problem - using database aggregation
    public Map<String, BigDecimal> getExpensesByCategory(LocalDate startDate, LocalDate endDate) {
        List<Object[]> results = expenseRepository.sumExpensesByCategoryBetweenDates(startDate, endDate);
        Map<String, BigDecimal> categoryMap = new HashMap<>();
        for (Object[] result : results) {
            String category = (String) result[0];
            BigDecimal total = (BigDecimal) result[1];
            categoryMap.put(category, total);
        }
        return categoryMap;
    }

    public BigDecimal getAverageMonthlyExpense(String category) {
        LocalDate today = LocalDate.now();
        LocalDate sixMonthsAgo = today.minusMonths(6);

        BigDecimal total;
        if (category != null && !category.isEmpty()) {
            List<Expense> expenses = expenseRepository.findByCategoryAndDateBetween(category, sixMonthsAgo, today);
            total = expenses.stream()
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } else {
            total = expenseRepository.sumExpensesBetweenDates(sixMonthsAgo, today);
            if (total == null) {
                total = BigDecimal.ZERO;
            }
        }

        return total.divide(new BigDecimal("6"), 2, java.math.RoundingMode.HALF_UP);
    }
}
