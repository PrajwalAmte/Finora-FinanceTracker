package com.finance_tracker.repository;

import com.finance_tracker.model.Sip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface SipRepository extends JpaRepository<Sip, Long> {
    List<Sip> findByUserId(Long userId);

    Optional<Sip> findByUserIdAndIsin(Long userId, String isin);

    @Query("SELECT COALESCE(SUM(s.totalUnits * s.currentNav), 0) FROM Sip s WHERE s.userId = :userId AND s.investmentId IS NULL")
    BigDecimal sumStandaloneCurrentValueByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(COALESCE(s.totalInvested, 0)), 0) FROM Sip s WHERE s.userId = :userId AND s.investmentId IS NULL")
    BigDecimal sumStandaloneTotalInvestedByUserId(@Param("userId") Long userId);

    @Query("SELECT s.investmentId FROM Sip s WHERE s.userId = :userId AND s.investmentId IS NOT NULL")
    List<Long> findLinkedInvestmentIdsByUserId(@Param("userId") Long userId);
}

