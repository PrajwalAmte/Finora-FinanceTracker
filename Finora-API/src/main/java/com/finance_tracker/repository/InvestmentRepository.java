package com.finance_tracker.repository;

import com.finance_tracker.model.Investment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvestmentRepository extends JpaRepository<Investment, Long> {
    List<Investment> findByUserId(Long userId);

    Optional<Investment> findByUserIdAndIsin(Long userId, String isin);

    Optional<Investment> findFirstByUserIdAndSymbol(Long userId, String symbol);
}
