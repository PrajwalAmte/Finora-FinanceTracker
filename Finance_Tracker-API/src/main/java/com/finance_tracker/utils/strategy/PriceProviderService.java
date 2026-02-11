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
            if (!provider.isAvailable()) {
                logger.debug("Skipping {} provider - not available", provider.getProviderName());
                continue;
            }
            
            try {
                logger.debug("Attempting to fetch price for {} using {}", symbol, provider.getProviderName());
                BigDecimal price = provider.fetchPrice(symbol, type);
                
                if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                    logger.info("Successfully fetched price for {} using {}: {}", symbol, provider.getProviderName(), price);
                    return price;
                } else {
                    logger.warn("Failed to get valid price for {} from {}", symbol, provider.getProviderName());
                }
            } catch (Exception e) {
                logger.warn("Error fetching price from {} for {}: {}", provider.getProviderName(), symbol, e.getMessage());
            }
        }
        
        logger.error("Failed to get valid price for {} from all providers", symbol);
        return null;
    }
}

