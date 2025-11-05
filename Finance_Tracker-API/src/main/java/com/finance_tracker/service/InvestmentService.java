package com.finance_tracker.service;

import com.finance_tracker.model.Investment;
import com.finance_tracker.repository.InvestmentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class InvestmentService {
    private static final Logger logger = LoggerFactory.getLogger(InvestmentService.class);

    private final InvestmentRepository investmentRepository;

    @Value("${twelvedata.api.key:}")
    private String twelveDataApiKey;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Cache to prevent API rate limiting issues
    private long lastYahooApiCallTime = 0;
    private long lastTwelveDataApiCallTime = 0;
    private static final long YAHOO_API_CALL_DELAY_MS = 10000; // Respect Yahoo Finance's rate limits
    private static final long TWELVEDATA_API_CALL_DELAY_MS = 8000; // Respect Twelve Data's rate limits

    public List<Investment> getAllInvestments() {
        return investmentRepository.findAll();
    }

    public Optional<Investment> getInvestmentById(Long id) {
        return investmentRepository.findById(id);
    }

    public Investment saveInvestment(Investment investment) {
        if (investment.getLastUpdated() == null) {
            investment.setLastUpdated(LocalDate.now());
        }
        return investmentRepository.save(investment);
    }

    public void deleteInvestment(Long id) {
        investmentRepository.deleteById(id);
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

    // Method to update investment prices (called by the scheduler)
    @Transactional
    public void updateCurrentPrices() {
        logger.info("Starting price update for all investments");
        List<Investment> investments = getAllInvestments();
        int updatedCount = 0;
        int failedCount = 0;

        for (Investment investment : investments) {
            try {
                BigDecimal currentPrice = null;
                String symbol = investment.getSymbol();
                com.finance_tracker.model.InvestmentType type = investment.getType();

                // First try Yahoo Finance
                try {
                    // Check if we need to throttle Yahoo API calls
                    throttleYahooApiCall();

                    currentPrice = fetchYahooFinancePrice(symbol, type);
                    if (currentPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0) {
                        logger.info("Successfully fetched price for {} using Yahoo Finance: {}", symbol, currentPrice);
                    } else {
                        logger.warn("Failed to get valid price for {} from Yahoo Finance, will try Twelve Data", symbol);
                    }
                } catch (Exception e) {
                    logger.warn("Error fetching price from Yahoo Finance for {}, will try Twelve Data. Error: {}",
                            symbol, e.getMessage());
                }

                // If Yahoo Finance failed, try Twelve Data as fallback
                if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                    try {
                        // Check if we need to throttle Twelve Data API calls
                        throttleTwelveDataApiCall();

                        currentPrice = fetchTwelveDataPrice(symbol, type);
                        if (currentPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0) {
                            logger.info("Successfully fetched price for {} using Twelve Data: {}", symbol, currentPrice);
                        } else {
                            logger.warn("Failed to get valid price for {} from Twelve Data", symbol);
                        }
                    } catch (Exception e) {
                        logger.error("Error fetching price from Twelve Data for {}: {}", symbol, e.getMessage());
                    }
                }

                if (currentPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0) {
                    investment.setCurrentPrice(currentPrice);
                    investment.setLastUpdated(LocalDate.now());
                    investmentRepository.save(investment);
                    updatedCount++;
                    logger.debug("Updated price for {}: {}", symbol, currentPrice);
                } else {
                    failedCount++;
                    logger.error("Failed to get valid price for {} from all APIs", symbol);
                }
            } catch (Exception e) {
                failedCount++;
                logger.error("Error in price update process for {}: {}", investment.getSymbol(), e.getMessage());
            }
        }

        logger.info("Price update completed. Updated: {}, Failed: {}", updatedCount, failedCount);
    }

    // Fetch current price using Yahoo Finance
    private static final int MAX_RETRIES = 3;

    private BigDecimal fetchYahooFinancePrice(String symbol, com.finance_tracker.model.InvestmentType type) {
        int attempt = 0;
        long backoff = 3000;

        while (attempt < MAX_RETRIES) {
            try {
                String yahooSymbol = symbol.endsWith(".NS") || symbol.endsWith(".BO") ? symbol : symbol + ".NS";
                String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + yahooSymbol + "?interval=1d";

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(15))
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 429) {
                    logger.warn("429 Too Many Requests for {}. Retrying in {} ms", yahooSymbol, backoff);
                    Thread.sleep(backoff);
                    backoff *= 2; // exponential backoff
                    attempt++;
                    continue;
                }

                if (response.statusCode() != 200) {
                    logger.error("Yahoo Finance API returned error: {} - {}", response.statusCode(), response.body());
                    return null;
                }

                JsonNode rootNode = objectMapper.readTree(response.body());
                JsonNode resultNode = rootNode.at("/chart/result/0");
                JsonNode closePrices = resultNode.at("/indicators/quote/0/close");

                if (closePrices.isArray() && closePrices.size() > 0) {
                    JsonNode lastCloseNode = closePrices.get(closePrices.size() - 1);
                    if (!lastCloseNode.isNull()) {
                        double lastClose = lastCloseNode.asDouble();
                        return BigDecimal.valueOf(lastClose);
                    }
                }

                logger.warn("No valid close price found for {} in Yahoo Finance", yahooSymbol);
                return null;

            } catch (Exception e) {
                logger.error("Error fetching price from Yahoo Finance for {}: {}", symbol, e.getMessage());
                attempt++;
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(backoff);
                        backoff *= 2;
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
            }
        }

        logger.error("Exceeded max retries for Yahoo Finance API for {}", symbol);
        return null;
    }

    // Fetch current price using Twelve Data API
    private BigDecimal fetchTwelveDataPrice(String symbol, com.finance_tracker.model.InvestmentType type) {
        if (twelveDataApiKey == null || twelveDataApiKey.isEmpty()) {
            logger.error("Twelve Data API key is not configured");
            return null;
        }

        try {
            // Format the symbol appropriately for Twelve Data
            // This may need adjustment based on how symbols are formatted in your system
            String tdSymbol = symbol;
            if (symbol.endsWith(".NS")) {
                tdSymbol = symbol.replace(".NS", "");
            } else if (symbol.endsWith(".BO")) {
                tdSymbol = symbol.replace(".BO", "");
            }

            String exchange = symbol.endsWith(".NS") ? "NSE" :
                    symbol.endsWith(".BO") ? "BSE" : "NSE";

            String url = "https://api.twelvedata.com/price" +
                    "?symbol=" + tdSymbol +
                    "&exchange=" + exchange +
                    "&apikey=" + twelveDataApiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                logger.error("Twelve Data API returned error: {} - {}", response.statusCode(), response.body());
                return null;
            }

            JsonNode rootNode = objectMapper.readTree(response.body());

            // Check for error response
            if (rootNode.has("code") || rootNode.has("status") && "error".equals(rootNode.get("status").asText())) {
                logger.error("Twelve Data API error: {}", rootNode.toString());
                return null;
            }

            // Extract price
            if (rootNode.has("price")) {
                String priceStr = rootNode.get("price").asText();
                return new BigDecimal(priceStr);
            } else {
                logger.warn("No price found in Twelve Data response for {}: {}", symbol, rootNode.toString());
                return null;
            }

        } catch (Exception e) {
            logger.error("Error fetching price from Twelve Data for {}: {}", symbol, e.getMessage(), e);
            return null;
        }
    }

    // Throttle Yahoo Finance API calls to avoid rate limiting
    private void throttleYahooApiCall() throws InterruptedException {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastCall = currentTime - lastYahooApiCallTime;

        if (timeSinceLastCall < YAHOO_API_CALL_DELAY_MS) {
            long waitTime = YAHOO_API_CALL_DELAY_MS - timeSinceLastCall;
            logger.debug("Throttling Yahoo API call. Sleeping for {} ms", waitTime);
            TimeUnit.MILLISECONDS.sleep(waitTime);
        }

        // Update the timestamp after the delay
        lastYahooApiCallTime = System.currentTimeMillis();
    }

    // Throttle Twelve Data API calls to avoid rate limiting
    private void throttleTwelveDataApiCall() throws InterruptedException {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastCall = currentTime - lastTwelveDataApiCallTime;

        if (timeSinceLastCall < TWELVEDATA_API_CALL_DELAY_MS) {
            long waitTime = TWELVEDATA_API_CALL_DELAY_MS - timeSinceLastCall;
            logger.debug("Throttling Twelve Data API call. Sleeping for {} ms", waitTime);
            TimeUnit.MILLISECONDS.sleep(waitTime);
        }

        // Update the timestamp after the delay
        lastTwelveDataApiCallTime = System.currentTimeMillis();
    }
}