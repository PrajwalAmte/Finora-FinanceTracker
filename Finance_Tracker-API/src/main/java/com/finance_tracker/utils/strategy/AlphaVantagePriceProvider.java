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
public class AlphaVantagePriceProvider implements PriceProviderStrategy {

    private static final Logger logger = LoggerFactory.getLogger(AlphaVantagePriceProvider.class);

    @Value("${alphavantage.api.key:}")
    private String apiKey;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public BigDecimal fetchPrice(String symbol, InvestmentType type) {
        if (!isAvailable()) {
            logger.error("Alpha Vantage API key is not configured");
            return null;
        }

        try {
            String avSymbol = buildAvSymbol(symbol);

            String url = "https://www.alphavantage.co/query" +
                    "?function=GLOBAL_QUOTE" +
                    "&symbol=" + avSymbol +
                    "&apikey=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                logger.error("Alpha Vantage API returned HTTP {}: {}", response.statusCode(), response.body());
                return null;
            }

            JsonNode root = objectMapper.readTree(response.body());

            if (root.has("Information")) {
                logger.warn("Alpha Vantage rate limit hit: {}", root.get("Information").asText());
                return null;
            }

            if (root.has("Error Message")) {
                logger.error("Alpha Vantage error for {}: {}", avSymbol, root.get("Error Message").asText());
                return null;
            }

            JsonNode quote = root.path("Global Quote");
            JsonNode priceNode = quote.path("05. price");

            if (priceNode.isMissingNode() || priceNode.asText().isBlank()) {
                logger.warn("Alpha Vantage returned no price for {} (queried as {}): {}", symbol, avSymbol, root.toString());
                return null;
            }

            return new BigDecimal(priceNode.asText());

        } catch (Exception e) {
            logger.error("Error fetching price from Alpha Vantage for {}: {}", symbol, e.getMessage(), e);
            return null;
        }
    }

    private String buildAvSymbol(String symbol) {
        if (symbol.endsWith(".NS")) {
            return symbol.replace(".NS", "") + ".BSE";
        }
        if (symbol.endsWith(".BO")) {
            return symbol.replace(".BO", "") + ".BSE";
        }
        if (!symbol.contains(".")) {
            return symbol + ".BSE";
        }
        return symbol;
    }

    @Override
    public String getProviderName() {
        return "Alpha Vantage";
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }
}
