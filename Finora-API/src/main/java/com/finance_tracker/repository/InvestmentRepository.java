package com.finance_tracker.repository;

import com.finance_tracker.model.Investment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvestmentRepository extends JpaRepository<Investment, Long> {
    List<Investment> findByUserId(Long userId);

    Optional<Investment> findByUserIdAndIsin(Long userId, String isin);

    Optional<Investment> findFirstByUserIdAndSymbol(Long userId, String symbol);

    @Query("SELECT COALESCE(SUM(i.quantity * i.currentPrice), 0) FROM Investment i WHERE i.userId = :userId")
    BigDecimal sumCurrentValueByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(i.quantity * i.currentPrice), 0) FROM Investment i WHERE i.userId = :userId AND i.id NOT IN :excludeIds")
    BigDecimal sumCurrentValueByUserIdExcluding(@Param("userId") Long userId, @Param("excludeIds") List<Long> excludeIds);

    @Query("SELECT COALESCE(SUM(i.quantity * i.currentPrice - i.quantity * i.purchasePrice), 0) FROM Investment i WHERE i.userId = :userId")
    BigDecimal sumProfitLossByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(i.quantity * i.currentPrice - i.quantity * i.purchasePrice), 0) FROM Investment i WHERE i.userId = :userId AND i.id NOT IN :excludeIds")
    BigDecimal sumProfitLossByUserIdExcluding(@Param("userId") Long userId, @Param("excludeIds") List<Long> excludeIds);

    @Query("SELECT COALESCE(SUM(i.quantity * i.currentPrice), 0) FROM Investment i WHERE i.id IN :ids")
    BigDecimal sumCurrentValueByIds(@Param("ids") List<Long> ids);

    @Query("SELECT COALESCE(SUM(i.quantity * i.purchasePrice), 0) FROM Investment i WHERE i.id IN :ids")
    BigDecimal sumCostBasisByIds(@Param("ids") List<Long> ids);
}
