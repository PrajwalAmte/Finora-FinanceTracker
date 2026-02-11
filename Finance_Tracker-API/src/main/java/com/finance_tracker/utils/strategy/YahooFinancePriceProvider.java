package com.finance_tracker.utils.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance_tracker.model.InvestmentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class YahooFinancePriceProvider implements PriceProviderStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(YahooFinancePriceProvider.class);
    private static final int MAX_RETRIES = 3;
    
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public BigDecimal fetchPrice(String symbol, InvestmentType type) {
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

                if (closePrices.isArray() && !closePrices.isEmpty()) {
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

    @Override
    public String getProviderName() {
        return "Yahoo Finance";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}

