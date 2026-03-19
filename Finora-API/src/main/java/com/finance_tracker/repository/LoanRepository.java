package com.finance_tracker.repository;

import com.finance_tracker.model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByUserId(Long userId);

    @Query("SELECT COALESCE(SUM(l.currentBalance), 0) FROM Loan l WHERE l.userId = :userId")
    BigDecimal sumCurrentBalanceByUserId(@Param("userId") Long userId);
}

