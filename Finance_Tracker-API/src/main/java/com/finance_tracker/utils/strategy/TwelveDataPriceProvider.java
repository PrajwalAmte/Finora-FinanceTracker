package com.finance_tracker.utils.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance_tracker.model.InvestmentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class TwelveDataPriceProvider implements PriceProviderStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(TwelveDataPriceProvider.class);
    
    @Value("${twelvedata.api.key:}")
    private String twelveDataApiKey;
    
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public BigDecimal fetchPrice(String symbol, InvestmentType type) {
        if (!isAvailable()) {
            logger.error("Twelve Data API key is not configured");
            return null;
        }

        try {
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

            if (rootNode.has("code") || (rootNode.has("status") && "error".equals(rootNode.get("status").asText()))) {
                logger.error("Twelve Data API error: {}", rootNode.toString());
                return null;
            }

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

    @Override
    public String getProviderName() {
        return "Twelve Data";
    }

    @Override
    public boolean isAvailable() {
        return twelveDataApiKey != null && !twelveDataApiKey.isEmpty();
    }
}

