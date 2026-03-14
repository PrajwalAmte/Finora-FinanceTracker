package com.finance_tracker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Fetches AMFI NAVAll.txt once per day and caches two maps:
 *   navsBySchemeCode: scheme code → NAV
 *   schemeCodeByIsin: ISIN → scheme code (indexes both growth and div-reinvestment ISINs)
 * Used by SipService (NAV updates) and StatementImportService (ISIN → scheme code lookup).
 */
@Service
public class AmfiNavService {

    private static final Logger logger = LoggerFactory.getLogger(AmfiNavService.class);
    private static final String AMFI_URL = "https://portal.amfiindia.com/spages/NAVAll.txt";
    private static final String ISIN_REGEX = "IN[A-Z]{2}[A-Z0-9]{9}\\d";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    // Volatile so reads outside the synchronized block see the latest reference.
    private volatile Map<String, BigDecimal> navsBySchemeCode = new HashMap<>();
    private volatile Map<String, String> schemeCodeByIsin = new HashMap<>();
    private volatile Map<String, String> schemeNameByCode = new HashMap<>();
    private volatile LocalDate cacheDate = null;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Returns all NAVs by scheme code; refreshes cache if stale. */
    public Map<String, BigDecimal> getAllNavs() {
        refreshIfStale();
        return Collections.unmodifiableMap(navsBySchemeCode);
    }

    /** Returns the NAV for a single scheme code; empty if unknown. */
    public Optional<BigDecimal> getNavBySchemeCode(String schemeCode) {
        if (schemeCode == null || schemeCode.isBlank()) return Optional.empty();
        refreshIfStale();
        return Optional.ofNullable(navsBySchemeCode.get(schemeCode.trim()));
    }

    /** Looks up AMFI scheme code by ISIN (indexes both growth and div-reinvestment ISINs). */
    public Optional<String> lookupSchemeCodeByIsin(String isin) {
        if (isin == null || isin.isBlank()) return Optional.empty();
        refreshIfStale();
        return Optional.ofNullable(schemeCodeByIsin.get(isin.trim().toUpperCase()));
    }

    /**
     * Searches AMFI scheme names by keyword (case-insensitive substring match).
     * Returns up to 15 results sorted by name, each containing schemeCode, name, nav.
     */
    public List<Map<String, Object>> searchByName(String query) {
        if (query == null || query.trim().length() < 2) return List.of();
        refreshIfStale();
        String q = query.trim().toLowerCase();
        List<Map<String, Object>> results = new ArrayList<>();
        for (Map.Entry<String, String> entry : schemeNameByCode.entrySet()) {
            if (entry.getValue().toLowerCase().contains(q)) {
                Map<String, Object> item = new HashMap<>();
                item.put("schemeCode", entry.getKey());
                item.put("name", entry.getValue());
                item.put("nav", navsBySchemeCode.getOrDefault(entry.getKey(), java.math.BigDecimal.ZERO));
                results.add(item);
                if (results.size() == 50) break; // cap before sort
            }
        }
        results.sort(Comparator.comparing(m -> (String) m.get("name")));
        return results.size() > 15 ? results.subList(0, 15) : results;
    }

    /** Force-refreshes the cache; called by the scheduler before a NAV-update run. */
    public synchronized void forceRefresh() {
        fetchAndCache();
    }

    // -------------------------------------------------------------------------
    // Cache management
    // -------------------------------------------------------------------------

    private void refreshIfStale() {
        // Fast path: cache is fresh — no locking needed.
        if (cacheDate != null && cacheDate.equals(LocalDate.now()) && !navsBySchemeCode.isEmpty()) {
            return;
        }
        synchronized (this) {
            // Re-check inside the lock (another thread may have refreshed already).
            if (cacheDate != null && cacheDate.equals(LocalDate.now()) && !navsBySchemeCode.isEmpty()) {
                return;
            }
            fetchAndCache();
        }
    }

    private void fetchAndCache() {
        try {
            logger.info("Fetching AMFI NAV data from {}", AMFI_URL);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(AMFI_URL))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                logger.error("AMFI returned HTTP {} — cache not refreshed", response.statusCode());
                return;
            }

            Map<String, BigDecimal> navs = new HashMap<>();
            Map<String, String> isinMap = new HashMap<>();
            Map<String, String> nameMap = new HashMap<>();

            BufferedReader reader = new BufferedReader(new StringReader(response.body()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.contains(";")) continue;

                // Split with -1 limit to preserve trailing empty fields.
                String[] parts = line.split(";", -1);
                if (parts.length < 6) continue;

                String schemeCode = parts[0].trim();
                String isin1      = parts[1].trim().toUpperCase(); // growth / div-payout
                String isin2      = parts[2].trim().toUpperCase(); // div-reinvestment
                String schemeName = parts[3].trim();
                String navStr     = parts[4].trim();

                if (schemeCode.isEmpty() || navStr.isEmpty() || !isNumeric(navStr)) continue;

                navs.put(schemeCode, new BigDecimal(navStr));
                if (!schemeName.isEmpty()) nameMap.put(schemeCode, schemeName);

                if (isin1.matches(ISIN_REGEX)) {
                    isinMap.put(isin1, schemeCode);
                }
                if (isin2.matches(ISIN_REGEX) && !isin2.equals(isin1)) {
                    isinMap.put(isin2, schemeCode);
                }
            }

            // Atomic swap — all maps updated together.
            navsBySchemeCode = navs;
            schemeCodeByIsin = isinMap;
            schemeNameByCode = nameMap;
            cacheDate = LocalDate.now();

            logger.info("AMFI cache refreshed: {} NAVs, {} ISINs indexed",
                    navs.size(), isinMap.size());

        } catch (Exception e) {
            logger.error("Error fetching AMFI NAV data: {}", e.getMessage(), e);
        }
    }

    private boolean isNumeric(String str) {
        try {
            new BigDecimal(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
