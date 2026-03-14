package com.finance_tracker.service;

import com.finance_tracker.model.Investment;
import com.finance_tracker.model.InvestmentType;
import com.finance_tracker.repository.InvestmentRepository;
import com.finance_tracker.utils.strategy.PriceProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvestmentService {
    private static final Logger logger = LoggerFactory.getLogger(InvestmentService.class);

    private final InvestmentRepository investmentRepository;
    private final PriceProviderService priceProviderService;
    private final AmfiNavService amfiNavService;
    private final LedgerService ledgerService;

    private Long resolveUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        try {
            return Long.parseLong(auth.getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void validateOwnership(Long resourceUserId, Long requestingUserId) {
        if (resourceUserId != null && requestingUserId != null
                && !resourceUserId.equals(requestingUserId)) {
            throw new com.finance_tracker.exception.ResourceNotFoundException("Investment not found");
        }
    }

    private String normalizeSymbol(String symbol) {
        if (symbol == null) {
            return null;
        }
        String normalized = symbol.trim().toUpperCase();
        if (normalized.endsWith(".NS") || normalized.endsWith(".BO")) {
            return normalized;
        }
        if (normalized.contains(".")) {
            return normalized;
        }
        return normalized + ".NS";
    }

    public List<Investment> getAllInvestments() {
        Long userId = resolveUserId();
        return investmentRepository.findByUserId(userId);
    }

    public Investment getInvestmentById(Long id) {
        Investment investment = investmentRepository.findById(id)
                .orElseThrow(() -> new com.finance_tracker.exception.ResourceNotFoundException("Investment", id));
        validateOwnership(investment.getUserId(), resolveUserId());
        return investment;
    }

    public Investment saveInvestment(Investment investment) {
        Long userId = resolveUserId();
        investment.setSymbol(normalizeSymbol(investment.getSymbol()));
        if (investment.getLastUpdated() == null) {
            investment.setLastUpdated(LocalDate.now());
        }
        if (investment.getId() != null) {
            Investment before = investmentRepository.findById(investment.getId()).orElse(null);
            if (before != null) validateOwnership(before.getUserId(), userId);
            investment.setUserId(userId);
            Investment saved = investmentRepository.save(investment);
            ledgerService.recordEvent("INVESTMENT", String.valueOf(saved.getId()), "UPDATE", before, saved, String.valueOf(userId));
            return saved;
        }
        investment.setUserId(userId);
        Investment saved = investmentRepository.save(investment);
        ledgerService.recordEvent("INVESTMENT", String.valueOf(saved.getId()), "CREATE", null, saved, String.valueOf(userId));
        return saved;
    }

    public void deleteInvestment(Long id) {
        Long userId = resolveUserId();
        Investment before = investmentRepository.findById(id)
                .orElseThrow(() -> new com.finance_tracker.exception.ResourceNotFoundException("Investment", id));
        validateOwnership(before.getUserId(), userId);
        investmentRepository.deleteById(id);
        ledgerService.recordEvent("INVESTMENT", String.valueOf(id), "DELETE", before, null, String.valueOf(userId));
    }

    public BigDecimal getTotalInvestmentValue() {
        return getAllInvestments().stream()
                .map(Investment::getCurrentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalProfitLoss() {
        return getAllInvestments().stream()
                .map(Investment::getProfitLoss)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public void updateCurrentPrices() {
        logger.info("Starting price update for all investments");
        List<Investment> investments = investmentRepository.findAll();
        int updatedCount = 0;
        int failedCount = 0;

        for (Investment investment : investments) {
            try {
                BigDecimal currentPrice;

                // Mutual funds: fetch live NAV from AMFI using the scheme code stored in symbol
                if (investment.getType() == InvestmentType.MUTUAL_FUND) {
                    if (investment.getSymbol() == null || investment.getSymbol().isBlank()) {
                        logger.debug("Skipping MF price update for '{}' — no scheme code", investment.getName());
                        failedCount++;
                        continue;
                    }
                    currentPrice = amfiNavService.getNavBySchemeCode(investment.getSymbol()).orElse(null);
                    if (currentPrice == null) {
                        logger.warn("No AMFI NAV found for MF '{}' (scheme code: {})",
                                investment.getName(), investment.getSymbol());
                        failedCount++;
                        continue;
                    }
                } else {
                    currentPrice = priceProviderService.fetchPrice(
                            investment.getSymbol(), investment.getType());
                }

                if (currentPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal scaledPrice = currentPrice.setScale(6, RoundingMode.HALF_UP);
                    // guard against values that exceed the column's 13-integer-digit precision
                    if (scaledPrice.toBigInteger().toString().length() > 13) {
                        logger.warn("Price for {} exceeds column precision: {}. Skipping.",
                                investment.getSymbol(), scaledPrice);
                        failedCount++;
                        continue;
                    }
                    investment.setCurrentPrice(scaledPrice);
                    investment.setLastUpdated(LocalDate.now());
                    investmentRepository.save(investment);
                    updatedCount++;
                } else {
                    failedCount++;
                    logger.error("No valid price for {} from any provider", investment.getSymbol());
                }
            } catch (Exception e) {
                failedCount++;
                logger.error("Error updating price for {}: {}", investment.getSymbol(), e.getMessage());
            }
        }

        logger.info("Price update completed. Updated: {}, Failed: {}", updatedCount, failedCount);
    }
}