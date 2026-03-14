package com.finance_tracker.utils.strategy;

import com.finance_tracker.model.InvestmentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PriceProviderService {
    
    private static final Logger logger = LoggerFactory.getLogger(PriceProviderService.class);
    
    private final List<PriceProviderStrategy> priceProviders;
    
    public PriceProviderService(List<PriceProviderStrategy> priceProviders) {
        this.priceProviders = priceProviders;
        logger.info("Initialized PriceProviderService with {} providers", priceProviders.size());
    }

    public BigDecimal fetchPrice(String symbol, InvestmentType type) {
        for (PriceProviderStrategy provider : priceProviders) {
            if (!provider.isAvailable()) continue;
            try {
                BigDecimal price = provider.fetchPrice(symbol, type);
                if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                    return price;
                }
            } catch (Exception e) {
                logger.warn("Price fetch failed for {} via {}: {}", symbol, provider.getProviderName(), e.getMessage());
            }
        }
        logger.error("No valid price for {} from any provider", symbol);
        return null;
    }
}

