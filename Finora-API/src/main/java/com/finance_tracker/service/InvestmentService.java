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
        // AMFI scheme codes are purely numeric (e.g. "119598") — no exchange suffix needed.
        if (normalized.matches("\\d+")) {
            return normalized;
        }
        // ISIN format: 12-char string starting with "IN" — no exchange suffix.
        if (normalized.matches("IN[A-Z]{2}[A-Z0-9]{9}\\d")) {
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

    /**
     * Total current value excluding investments whose IDs are in {@code excludeIds}.
     * Used to exclude SIP-linked MF rows so they are not double-counted alongside SIP totals.
     */
    public BigDecimal getTotalInvestmentValueExcluding(List<Long> excludeIds) {
        return getAllInvestments().stream()
                .filter(inv -> inv.getId() == null || !excludeIds.contains(inv.getId()))
                .map(Investment::getCurrentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Same exclusion logic for P&L. */
    public BigDecimal getTotalProfitLossExcluding(List<Long> excludeIds) {
        return getAllInvestments().stream()
                .filter(inv -> inv.getId() == null || !excludeIds.contains(inv.getId()))
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
                    // Try symbol as AMFI scheme code first; if that fails and symbol looks like
                    // an ISIN (starts with IN), resolve the scheme code via ISIN lookup.
                    currentPrice = amfiNavService.getNavBySchemeCode(investment.getSymbol()).orElse(null);
                    if (currentPrice == null) {
                        // Strip exchange suffix added by normalizeSymbol() for pre-fix rows
                        String sym = investment.getSymbol().replaceAll("\\.NS$|\\.BO$", "");
                        // Step 1: retry stripped string as a scheme code (handles "119598.NS" → "119598")
                        currentPrice = amfiNavService.getNavBySchemeCode(sym).orElse(null);
                        if (currentPrice != null) {
                            investment.setSymbol(sym);  // fix the stored symbol for future refreshes
                        } else {
                            // Step 2: try ISIN lookup from the stripped symbol
                            String resolved = amfiNavService.lookupSchemeCodeByIsin(sym).orElse(null);
                            // Step 3: also try the investment's own isin field as a last resort
                            if (resolved == null && investment.getIsin() != null) {
                                resolved = amfiNavService.lookupSchemeCodeByIsin(
                                        investment.getIsin().replaceAll("\\.NS$|\\.BO$", "")).orElse(null);
                            }
                            if (resolved != null) {
                                currentPrice = amfiNavService.getNavBySchemeCode(resolved).orElse(null);
                                if (currentPrice != null) {
                                    // Correct the symbol so future refreshes skip the ISIN resolution step
                                    investment.setSymbol(resolved);
                                }
                            }
                        }
                    }
                    if (currentPrice == null) {
                        logger.warn("No AMFI NAV found for MF '{}' (symbol: {})",
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

    /**
     * Adds units to an existing investment and recalculates the weighted-average buy price.
     *
     * new_avg = (old_qty × old_avg + add_qty × add_price) / (old_qty + add_qty)
     */
    @Transactional
    public Investment addUnits(Long id, BigDecimal addQty, BigDecimal addPrice) {
        Long userId = resolveUserId();
        Investment inv = investmentRepository.findById(id)
                .orElseThrow(() -> new com.finance_tracker.exception.ResourceNotFoundException("Investment", id));
        validateOwnership(inv.getUserId(), userId);

        // Snapshot before state for the ledger
        Investment before = snapshotOf(inv);

        BigDecimal oldValue = inv.getQuantity().multiply(inv.getPurchasePrice());
        BigDecimal addValue  = addQty.multiply(addPrice);
        BigDecimal newQty    = inv.getQuantity().add(addQty);
        BigDecimal newAvg    = oldValue.add(addValue).divide(newQty, 6, RoundingMode.HALF_UP);

        inv.setQuantity(newQty.setScale(6, RoundingMode.HALF_UP));
        inv.setPurchasePrice(newAvg);
        inv.setLastUpdated(LocalDate.now());

        Investment saved = investmentRepository.save(inv);
        ledgerService.recordEvent("INVESTMENT", String.valueOf(id), "ADD_UNITS", before, saved, String.valueOf(userId));
        return saved;
    }

    /**
     * Sells units from an existing investment.
     *
     * - If sell_qty ≥ current_qty: the investment is deleted entirely; returns empty Optional.
     * - Otherwise: quantity is reduced and the weighted-average buy price stays the same.
     *
     * @throws com.finance_tracker.exception.BusinessLogicException if sellQty is invalid.
     */
    @Transactional
    public java.util.Optional<Investment> sellUnits(Long id, BigDecimal sellQty, BigDecimal sellPrice) {
        Long userId = resolveUserId();
        Investment inv = investmentRepository.findById(id)
                .orElseThrow(() -> new com.finance_tracker.exception.ResourceNotFoundException("Investment", id));
        validateOwnership(inv.getUserId(), userId);

        if (sellQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new com.finance_tracker.exception.BusinessLogicException("Sell quantity must be greater than 0");
        }

        Investment before = snapshotOf(inv);

        if (sellQty.compareTo(inv.getQuantity()) >= 0) {
            // Sold all (or more than held) — remove the row entirely
            investmentRepository.deleteById(id);
            ledgerService.recordEvent("INVESTMENT", String.valueOf(id), "SELL_ALL", before, null, String.valueOf(userId));
            return java.util.Optional.empty();
        }

        BigDecimal newQty = inv.getQuantity().subtract(sellQty).setScale(6, RoundingMode.HALF_UP);
        inv.setQuantity(newQty);
        inv.setLastUpdated(LocalDate.now());
        // Avg buy price is unchanged (cost-basis stays the same per remaining unit)

        Investment saved = investmentRepository.save(inv);
        ledgerService.recordEvent("INVESTMENT", String.valueOf(id), "SELL_UNITS", before, saved, String.valueOf(userId));
        return java.util.Optional.of(saved);
    }

    /** Creates a detached copy of an investment for ledger snapshots. */
    private Investment snapshotOf(Investment src) {
        Investment snap = new Investment();
        snap.setId(src.getId());
        snap.setName(src.getName());
        snap.setSymbol(src.getSymbol());
        snap.setType(src.getType());
        snap.setQuantity(src.getQuantity());
        snap.setPurchasePrice(src.getPurchasePrice());
        snap.setCurrentPrice(src.getCurrentPrice());
        snap.setPurchaseDate(src.getPurchaseDate());
        snap.setLastUpdated(src.getLastUpdated());
        snap.setUserId(src.getUserId());
        snap.setIsin(src.getIsin());
        snap.setImportSource(src.getImportSource());
        return snap;
    }
}