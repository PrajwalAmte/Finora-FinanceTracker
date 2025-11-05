package com.finance_tracker.service;

import com.finance_tracker.model.Expense;
import com.finance_tracker.repository.ExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseService {
    private final ExpenseRepository expenseRepository;

    public List<Expense> getAllExpenses() {
        return expenseRepository.findAll();
    }

    public Optional<Expense> getExpenseById(Long id) {
        return expenseRepository.findById(id);
    }

    public Expense saveExpense(Expense expense) {
        if (expense.getDate() == null) {
            expense.setDate(LocalDate.now());
        }
        return expenseRepository.save(expense);
    }

    public void deleteExpense(Long id) {
        expenseRepository.deleteById(id);
    }

    public List<Expense> getExpensesBetweenDates(LocalDate startDate, LocalDate endDate) {
        return expenseRepository.findByDateBetween(startDate, endDate);
    }

    public List<Expense> getExpensesByCategory(String category) {
        return expenseRepository.findByCategory(category);
    }

    public BigDecimal getTotalExpenses(LocalDate startDate, LocalDate endDate) {
        List<Expense> expenses = getExpensesBetweenDates(startDate, endDate);
        return expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Map<String, BigDecimal> getExpensesByCategory(LocalDate startDate, LocalDate endDate) {
        List<Expense> expenses = getExpensesBetweenDates(startDate, endDate);
        return expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Expense::getAmount,
                                BigDecimal::add
                        )
                ));
    }

    public BigDecimal getAverageMonthlyExpense(String category) {
        LocalDate today = LocalDate.now();
        LocalDate sixMonthsAgo = today.minusMonths(6);

        List<Expense> expenses;
        if (category != null && !category.isEmpty()) {
            expenses = expenseRepository.findByCategoryAndDateBetween(category, sixMonthsAgo, today);
        } else {
            expenses = expenseRepository.findByDateBetween(sixMonthsAgo, today);
        }

        BigDecimal total = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return total.divide(new BigDecimal("6"), 2, java.math.RoundingMode.HALF_UP);
    }
}
