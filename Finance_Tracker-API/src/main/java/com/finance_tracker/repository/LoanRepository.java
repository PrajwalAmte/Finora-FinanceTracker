package com.finance_tracker.repository;

import com.finance_tracker.model.Loan;
import com.finance_tracker.model.LoanInterestType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByInterestType(LoanInterestType interestType);
}

