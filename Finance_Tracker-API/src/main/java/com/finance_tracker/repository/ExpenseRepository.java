package com.finance_tracker.repository;

import com.finance_tracker.model.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByCategory(String category);
    List<Expense> findByDateBetween(LocalDate startDate, LocalDate endDate);
    List<Expense> findByCategoryAndDateBetween(String category, LocalDate startDate, LocalDate endDate);
    
    // Pagination support
    Page<Expense> findAll(Pageable pageable);
    Page<Expense> findByCategory(String category, Pageable pageable);
    Page<Expense> findByDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);
    
    // Aggregation queries to fix N+1 problems
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.date BETWEEN :startDate AND :endDate")
    BigDecimal sumExpensesBetweenDates(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT e.category, COALESCE(SUM(e.amount), 0) as total FROM Expense e WHERE e.date BETWEEN :startDate AND :endDate GROUP BY e.category")
    List<Object[]> sumExpensesByCategoryBetweenDates(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}