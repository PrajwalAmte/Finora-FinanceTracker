package com.finance_tracker.repository;

import com.finance_tracker.model.Investment;
import com.finance_tracker.model.InvestmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvestmentRepository extends JpaRepository<Investment, Long> {
    List<Investment> findByType(InvestmentType type);
    List<Investment> findBySymbol(String symbol);
}
