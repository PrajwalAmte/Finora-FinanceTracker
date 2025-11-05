package com.finance_tracker.service;

import com.finance_tracker.model.Sip;
import com.finance_tracker.repository.SipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.io.BufferedReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class SipService {
    private static final Logger logger = LoggerFactory.getLogger(SipService.class);

    private final SipRepository sipRepository;

    // Cache NAV data to reduce API calls
    private Map<String, BigDecimal> navCache = new ConcurrentHashMap<>();
    private LocalDate navCacheDate = null;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public List<Sip> getAllSips() {
        return sipRepository.findAll();
    }

    public Optional<Sip> getSipById(Long id) {
        return sipRepository.findById(id);
    }

    public Sip saveSip(Sip sip) {
        if (sip.getLastUpdated() == null) {
            sip.setLastUpdated(LocalDate.now());
        }

        // Initialize total units if null
        if (sip.getTotalUnits() == null) {
            sip.setTotalUnits(BigDecimal.ZERO);
        }

        return sipRepository.save(sip);
    }

    public void deleteSip(Long id) {
        sipRepository.deleteById(id);
    }

    public BigDecimal getTotalSipValue() {
        return getAllSips().stream()
                .map(Sip::getCurrentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalSipInvestment() {
        return getAllSips().stream()
                .map(sip -> {
                    BigDecimal totalInvested = sip.getTotalInvested();
                    return totalInvested != null ? totalInvested : BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Method to update SIP NAVs (called by the scheduler)
    @Transactional
    public void updateCurrentNavs() {
        logger.info("Starting NAV update for all SIPs");

        try {
            // Fetch all NAVs in one API call and cache them
            Map<String, BigDecimal> navData = fetchAllNavData();
            if (navData == null || navData.isEmpty()) {
                logger.error("Failed to fetch NAV data from AMFI");
                return;
            }

            // Cache the NAV data
            navCache = navData;
            navCacheDate = LocalDate.now();

            // Update all SIPs with the latest NAVs
            List<Sip> sips = getAllSips();
            int updatedCount = 0;
            int failedCount = 0;

            for (Sip sip : sips) {
                try {
                    String schemeCode = sip.getSchemeCode();
                    BigDecimal nav = navCache.get(schemeCode);

                    if (nav != null && nav.compareTo(BigDecimal.ZERO) > 0) {
                        sip.setCurrentNav(nav);
                        sip.setLastUpdated(LocalDate.now());
                        sipRepository.save(sip);
                        updatedCount++;
                        logger.debug("Updated NAV for scheme {}: {}", schemeCode, nav);
                    } else {
                        failedCount++;
                        logger.warn("No NAV found for scheme code: {}", schemeCode);
                    }
                } catch (Exception e) {
                    failedCount++;
                    logger.error("Error updating NAV for scheme {}: {}",
                            sip.getSchemeCode(), e.getMessage());
                }
            }

            logger.info("NAV update completed. Updated: {}, Failed: {}", updatedCount, failedCount);
        } catch (Exception e) {
            logger.error("Error in updateCurrentNavs: {}", e.getMessage(), e);
        }
    }

    // Method to simulate monthly SIP investment (called by the scheduler)
    @Transactional
    public void processMonthlyInvestments() {
        logger.info("Processing monthly SIP investments");
        LocalDate today = LocalDate.now();
        List<Sip> sips = getAllSips();
        int processedCount = 0;
        int skippedCount = 0;

        for (Sip sip : sips) {
            try {
                // Check if it's time for monthly investment
                if (shouldProcessMonthlyInvestment(sip, today)) {
                    // Fetch current NAV
                    BigDecimal currentNav = getOrFetchNav(sip.getSchemeCode());

                    if (currentNav != null && currentNav.compareTo(BigDecimal.ZERO) > 0) {
                        // Calculate units purchased in this installment
                        BigDecimal newUnits = sip.getMonthlyAmount()
                                .divide(currentNav, 4, RoundingMode.HALF_UP);

                        // Update total units
                        BigDecimal totalUnits = sip.getTotalUnits();
                        if (totalUnits == null) {
                            totalUnits = BigDecimal.ZERO;
                        }
                        sip.setTotalUnits(totalUnits.add(newUnits));

                        // Update NAV and dates
                        sip.setCurrentNav(currentNav);
                        sip.setLastInvestmentDate(today);
                        sip.setLastUpdated(today);

                        // Save changes
                        sipRepository.save(sip);
                        processedCount++;
                        logger.info("Processed SIP installment for {}: Amount: {}, Units: {}, NAV: {}",
                                sip.getName(), sip.getMonthlyAmount(), newUnits, currentNav);
                    } else {
                        skippedCount++;
                        logger.warn("Invalid NAV for scheme {}, skipping investment", sip.getSchemeCode());
                    }
                } else {
                    skippedCount++;
                    logger.debug("Skipping SIP {} - Not due for investment today", sip.getName());
                }
            } catch (Exception e) {
                skippedCount++;
                logger.error("Error processing SIP investment for {}: {}",
                        sip.getName(), e.getMessage(), e);
            }
        }

        logger.info("Monthly SIP processing completed. Processed: {}, Skipped: {}",
                processedCount, skippedCount);
    }

    // Helper method to determine if a SIP installment should be processed
    private boolean shouldProcessMonthlyInvestment(Sip sip, LocalDate today) {
        // If SIP has never been invested before, process it
        if (sip.getLastInvestmentDate() == null) {
            // Only if start date is in the past or today
            return sip.getStartDate() == null || !today.isBefore(sip.getStartDate());
        }

        // Check if it's a new month since last investment
        LocalDate lastInvestment = sip.getLastInvestmentDate();
        return lastInvestment.getMonth() != today.getMonth() ||
                lastInvestment.getYear() != today.getYear();
    }

    // Get NAV from cache or fetch from API if needed
    private BigDecimal getOrFetchNav(String schemeCode) {
        // Use cached value if available and fresh
        if (navCache.containsKey(schemeCode) &&
                navCacheDate != null &&
                navCacheDate.equals(LocalDate.now())) {
            return navCache.get(schemeCode);
        }

        // Fetch individual NAV if cache is outdated
        try {
            if (navCache.isEmpty() || navCacheDate == null || !navCacheDate.equals(LocalDate.now())) {
                // Refresh the entire cache
                Map<String, BigDecimal> freshData = fetchAllNavData();
                if (freshData != null && !freshData.isEmpty()) {
                    navCache = freshData;
                    navCacheDate = LocalDate.now();
                }
            }

            return navCache.getOrDefault(schemeCode, null);
        } catch (Exception e) {
            logger.error("Error fetching NAV for scheme {}: {}", schemeCode, e.getMessage());
            return null;
        }
    }

    // Fetch all NAV data from AMFI API
    private Map<String, BigDecimal> fetchAllNavData() {
        try {
            logger.info("Fetching NAV data from AMFI");

            // AMFI provides NAV data in a plain text CSV format
            String amfiUrl = "https://www.amfiindia.com/spages/NAVAll.txt";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(amfiUrl))
                    .timeout(Duration.ofSeconds(30)) // Longer timeout for AMFI which can be slow
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Check if the request was successful
            if (response.statusCode() != 200) {
                logger.error("AMFI API returned error: {} - {}",
                        response.statusCode(), response.body());
                return null;
            }

            // Process the CSV-like response
            Map<String, BigDecimal> navData = new HashMap<>();
            BufferedReader reader = new BufferedReader(new StringReader(response.body()));
            String line;

            while ((line = reader.readLine()) != null) {
                try {
                    // AMFI format: Scheme Code;ISIN Div Payout/ISIN Growth;ISIN Div Reinvestment;Scheme Name;Net Asset Value;Date
                    if (line.contains(";")) {
                        String[] parts = line.split(";");
                        if (parts.length >= 5) {
                            String schemeCode = parts[0].trim();
                            String navStr = parts[4].trim();

                            if (!schemeCode.isEmpty() && !navStr.isEmpty() && isNumeric(navStr)) {
                                navData.put(schemeCode, new BigDecimal(navStr));
                            }
                        }
                    }
                } catch (Exception e) {
                    // Skip malformed lines but continue processing
                    logger.debug("Skipping malformed NAV data line: {}", line);
                }
            }

            logger.info("Fetched NAV data for {} schemes", navData.size());
            return navData;
        } catch (Exception e) {
            logger.error("Error fetching NAV data from AMFI: {}", e.getMessage(), e);
            return null;
        }
    }

    // Helper method to check if a string is numeric
    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            new BigDecimal(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}