package com.finance_tracker.utils.strategy;

import com.finance_tracker.model.InvestmentType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class YahooFinancePriceProviderTest {

    private final YahooFinancePriceProvider provider = new YahooFinancePriceProvider();

    @Test
    void isAvailable_alwaysTrue() {
        assertThat(provider.isAvailable()).isTrue();
    }

    @Test
    void getProviderName_returnsYahooFinance() {
        assertThat(provider.getProviderName()).isEqualTo("Yahoo Finance");
    }
}
