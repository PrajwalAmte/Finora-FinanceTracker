package com.finance_tracker.service;

import com.finance_tracker.model.Sip;
import com.finance_tracker.repository.SipRepository;
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
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SipService {
    private static final Logger logger = LoggerFactory.getLogger(SipService.class);

    private final SipRepository sipRepository;
    private final LedgerService ledgerService;
    private final AmfiNavService amfiNavService;

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
            throw new com.finance_tracker.exception.ResourceNotFoundException("SIP not found");
        }
    }

    public List<Sip> getAllSips() {
        Long userId = resolveUserId();
        return sipRepository.findByUserId(userId);
    }

    public Sip getSipById(Long id) {
        Sip sip = sipRepository.findById(id)
                .orElseThrow(() -> new com.finance_tracker.exception.ResourceNotFoundException("Sip", id));
        validateOwnership(sip.getUserId(), resolveUserId());
        return sip;
    }

    public Sip saveSip(Sip sip) {
        Long userId = resolveUserId();
        if (sip.getLastUpdated() == null) {
            sip.setLastUpdated(LocalDate.now());
        }

        if (sip.getTotalUnits() == null) {
            sip.setTotalUnits(BigDecimal.ZERO);
        }

        if (sip.getCurrentNav() == null) {
            sip.setCurrentNav(BigDecimal.ZERO);
        }

        if (sip.getId() != null) {
            Sip before = sipRepository.findById(sip.getId()).orElse(null);
            if (before != null) validateOwnership(before.getUserId(), userId);
            sip.setUserId(userId);
            Sip saved = sipRepository.save(sip);
            ledgerService.recordEvent("SIP", String.valueOf(saved.getId()), "UPDATE", before, saved, String.valueOf(userId));
            return saved;
        }
        sip.setUserId(userId);
        Sip saved = sipRepository.save(sip);
        ledgerService.recordEvent("SIP", String.valueOf(saved.getId()), "CREATE", null, saved, String.valueOf(userId));
        return saved;
    }

    public void deleteSip(Long id) {
        Long userId = resolveUserId();
        Sip before = sipRepository.findById(id)
                .orElseThrow(() -> new com.finance_tracker.exception.ResourceNotFoundException("Sip", id));
        validateOwnership(before.getUserId(), userId);
        sipRepository.deleteById(id);
        ledgerService.recordEvent("SIP", String.valueOf(id), "DELETE", before, null, String.valueOf(userId));
    }

    /**
     * Current value of standalone SIPs only (investmentId == null).
     * Linked SIPs are already counted via their backing Investment record.
     */
    public BigDecimal getTotalSipValue() {
        return getAllSips().stream()
                .filter(sip -> sip.getInvestmentId() == null)
                .map(Sip::getCurrentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalSipInvestment() {
        return getAllSips().stream()
                .filter(sip -> sip.getInvestmentId() == null)
                .map(sip -> sip.getTotalInvested() != null ? sip.getTotalInvested() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public void updateCurrentNavs() {
        logger.info("Starting NAV update for all SIPs");
        try {
            Map<String, BigDecimal> navData = amfiNavService.getAllNavs();
            if (navData == null || navData.isEmpty()) {
                logger.error("Failed to fetch NAV data from AMFI");
                return;
            }

            List<Sip> sips = sipRepository.findAll();
            int updatedCount = 0;
            int failedCount = 0;

            for (Sip sip : sips) {
                try {
                    String schemeCode = sip.getSchemeCode();

                    // If scheme code is missing, try to resolve it from the SIP's stored ISIN.
                    if (schemeCode == null || schemeCode.isBlank()) {
                        if (sip.getIsin() != null && !sip.getIsin().isBlank()) {
                            schemeCode = amfiNavService.lookupSchemeCodeByIsin(sip.getIsin()).orElse(null);
                            if (schemeCode != null) {
                                // Persist the resolved scheme code so future runs skip this step.
                                sip.setSchemeCode(schemeCode);
                                logger.info("Resolved scheme code {} for SIP '{}' via ISIN {}",
                                        schemeCode, sip.getName(), sip.getIsin());
                            }
                        }
                        if (schemeCode == null) {
                            logger.debug("Skipping NAV update for SIP '{}' — no scheme code or ISIN", sip.getName());
                            continue;
                        }
                    }

                    BigDecimal nav = navData.get(schemeCode);
                    if (nav != null && nav.compareTo(BigDecimal.ZERO) > 0) {
                        sip.setCurrentNav(nav);
                        sip.setLastUpdated(LocalDate.now());
                        sipRepository.save(sip);
                        updatedCount++;
                    } else {
                        failedCount++;
                        logger.warn("No NAV found for scheme code: {}", sip.getSchemeCode());
                    }
                } catch (Exception e) {
                    failedCount++;
                    logger.error("Error updating NAV for scheme {}: {}", sip.getSchemeCode(), e.getMessage());
                }
            }

            logger.info("NAV update completed. Updated: {}, Failed: {}", updatedCount, failedCount);
        } catch (Exception e) {
            logger.error("Error in updateCurrentNavs: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void processMonthlyInvestments() {
        logger.info("Processing monthly SIP investments");
        LocalDate today = LocalDate.now();
        List<Sip> sips = sipRepository.findAll();
        int processedCount = 0;
        int skippedCount = 0;

        for (Sip sip : sips) {
            try {
                if (shouldProcessMonthlyInvestment(sip, today)) {
                    BigDecimal currentNav = amfiNavService.getNavBySchemeCode(sip.getSchemeCode()).orElse(null);

                    if (currentNav != null && currentNav.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal newUnits = sip.getMonthlyAmount()
                                .divide(currentNav, 4, RoundingMode.HALF_UP);

                        BigDecimal totalUnits = sip.getTotalUnits() != null ? sip.getTotalUnits() : BigDecimal.ZERO;
                        sip.setTotalUnits(totalUnits.add(newUnits));
                        sip.setCurrentNav(currentNav);
                        sip.setLastInvestmentDate(today);
                        sip.setLastUpdated(today);

                        sipRepository.save(sip);
                        processedCount++;
                        logger.info("SIP installment for {}: amount={}, units={}, nav={}",
                                sip.getName(), sip.getMonthlyAmount(), newUnits, currentNav);
                    } else {
                        skippedCount++;
                        logger.warn("Invalid NAV for scheme {}, skipping", sip.getSchemeCode());
                    }
                } else {
                    skippedCount++;
                }
            } catch (Exception e) {
                skippedCount++;
                logger.error("Error processing SIP for {}: {}", sip.getName(), e.getMessage(), e);
            }
        }

        logger.info("Monthly SIP processing done. Processed: {}, Skipped: {}", processedCount, skippedCount);
    }

    private boolean shouldProcessMonthlyInvestment(Sip sip, LocalDate today) {
        if (sip.getLastInvestmentDate() == null) {
            return sip.getStartDate() == null || !today.isBefore(sip.getStartDate());
        }
        LocalDate lastInvestment = sip.getLastInvestmentDate();
        return lastInvestment.getMonth() != today.getMonth()
                || lastInvestment.getYear() != today.getYear();
    }

    /**
     * Records a manual monthly SIP payment made by the user.
     * Adds units = monthlyAmount / currentNav and sets lastInvestmentDate = today.
     */
    @Transactional
    public Sip recordPayment(Long id) {
        Long userId = resolveUserId();
        Sip sip = sipRepository.findById(id)
                .orElseThrow(() -> new com.finance_tracker.exception.ResourceNotFoundException("Sip", id));
        validateOwnership(sip.getUserId(), userId);

        // Add units for this installment (if NAV is available).
        BigDecimal currentNav = sip.getCurrentNav();
        if (currentNav != null && currentNav.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal unitsAdded = sip.getMonthlyAmount()
                    .divide(currentNav, 8, RoundingMode.HALF_UP);
            BigDecimal existing = sip.getTotalUnits() != null ? sip.getTotalUnits() : BigDecimal.ZERO;
            sip.setTotalUnits(existing.add(unitsAdded));
        }

        // Accumulate total invested (actual money paid, not derived from elapsed months).
        BigDecimal invested = sip.getTotalInvested() != null ? sip.getTotalInvested() : BigDecimal.ZERO;
        sip.setTotalInvested(invested.add(sip.getMonthlyAmount()));

        LocalDate today = LocalDate.now();
        sip.setLastInvestmentDate(today);
        sip.setLastUpdated(today);

        // Advance the next installment date by one month.
        if (sip.getStartDate() != null) {
            sip.setStartDate(sip.getStartDate().plusMonths(1));
        }

        Sip saved = sipRepository.save(sip);
        ledgerService.recordEvent("SIP", String.valueOf(id), "PAY", null, saved, String.valueOf(userId));
        return saved;
    }

}