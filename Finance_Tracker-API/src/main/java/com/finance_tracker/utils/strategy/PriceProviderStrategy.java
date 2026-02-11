package com.finance_tracker.utils.strategy;

import com.finance_tracker.model.InvestmentType;

import java.math.BigDecimal;

public interface PriceProviderStrategy {

    BigDecimal fetchPrice(String symbol, InvestmentType type);

    String getProviderName();

    boolean isAvailable();
}

