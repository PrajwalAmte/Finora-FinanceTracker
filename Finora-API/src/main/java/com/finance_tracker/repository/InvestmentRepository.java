package com.finance_tracker.repository;

import com.finance_tracker.model.Investment;
import com.finance_tracker.model.InvestmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvestmentRepository extends JpaRepository<Investment, Long> {
    List<Investment> findByUserId(Long userId);
    List<Investment> findByType(InvestmentType type);
    List<Investment> findBySymbol(String symbol);

    /**
     * Dedup lookup for statement import: finds the single existing row
     * (if any) for this user+ISIN combination.
     * The DB partial unique index guarantees at most one result.
     */
    Optional<Investment> findByUserIdAndIsin(Long userId, String isin);

    /** Symbol-based dedup for CSV imports that don't include ISIN. */
    Optional<Investment> findFirstByUserIdAndSymbol(Long userId, String symbol);
}
