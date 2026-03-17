package com.finance_tracker.utils.strategy;

import com.finance_tracker.model.InvestmentType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class AlphaVantagePriceProviderTest {

    private AlphaVantagePriceProvider providerWithKey(String key) {
        AlphaVantagePriceProvider p = new AlphaVantagePriceProvider();
        ReflectionTestUtils.setField(p, "apiKey", key);
        return p;
    }

    @Test
    void isAvailable_falseWhenKeyIsBlank() {
        assertThat(providerWithKey("").isAvailable()).isFalse();
    }

    @Test
    void isAvailable_falseWhenKeyIsNull() {
        assertThat(providerWithKey(null).isAvailable()).isFalse();
    }

    @Test
    void isAvailable_trueWhenKeyIsSet() {
        assertThat(providerWithKey("SOME_KEY_123").isAvailable()).isTrue();
    }

    @Test
    void getProviderName_returnsAlphaVantage() {
        assertThat(providerWithKey("key").getProviderName()).isEqualTo("Alpha Vantage");
    }

    @Test
    void fetchPrice_returnsNullWhenKeyIsBlank() {
        AlphaVantagePriceProvider p = providerWithKey("");
        assertThat(p.fetchPrice("RELIANCE.NS", InvestmentType.STOCK)).isNull();
    }
}
