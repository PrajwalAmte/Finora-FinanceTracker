package com.finance_tracker.utils.strategy;

import com.finance_tracker.model.InvestmentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PriceProviderServiceTest {

    private PriceProviderStrategy provider(boolean available, BigDecimal price) {
        PriceProviderStrategy p = mock(PriceProviderStrategy.class);
        when(p.isAvailable()).thenReturn(available);
        if (available) {
            when(p.fetchPrice("RELIANCE.NS", InvestmentType.STOCK)).thenReturn(price);
            when(p.getProviderName()).thenReturn("MockProvider");
        }
        return p;
    }

    @Test
    void fetchPrice_returnsFirstPositiveResult() {
        PriceProviderStrategy p = provider(true, new BigDecimal("2450.00"));
        PriceProviderService service = new PriceProviderService(java.util.List.of(p));

        BigDecimal price = service.fetchPrice("RELIANCE.NS", InvestmentType.STOCK);
        assertThat(price).isEqualByComparingTo("2450.00");
    }

    @Test
    void fetchPrice_skipsUnavailableProvider() {
        PriceProviderStrategy unavailable = mock(PriceProviderStrategy.class);
        when(unavailable.isAvailable()).thenReturn(false);

        PriceProviderStrategy available = provider(true, new BigDecimal("100.00"));

        PriceProviderService service = new PriceProviderService(java.util.List.of(unavailable, available));
        assertThat(service.fetchPrice("RELIANCE.NS", InvestmentType.STOCK)).isEqualByComparingTo("100.00");
    }

    @Test
    void fetchPrice_triesNextWhenFirstReturnsNull() {
        PriceProviderStrategy first = provider(true, null);
        PriceProviderStrategy second = provider(true, new BigDecimal("200.00"));

        PriceProviderService service = new PriceProviderService(java.util.List.of(first, second));
        assertThat(service.fetchPrice("RELIANCE.NS", InvestmentType.STOCK)).isEqualByComparingTo("200.00");
    }

    @Test
    void fetchPrice_returnsNullWhenAllFail() {
        PriceProviderStrategy p = provider(true, null);
        PriceProviderService service = new PriceProviderService(java.util.List.of(p));

        assertThat(service.fetchPrice("RELIANCE.NS", InvestmentType.STOCK)).isNull();
    }

    @Test
    void fetchPrice_swallowsExceptionAndTriesNext() {
        PriceProviderStrategy throwing = mock(PriceProviderStrategy.class);
        when(throwing.isAvailable()).thenReturn(true);
        when(throwing.getProviderName()).thenReturn("Boom");
        when(throwing.fetchPrice("RELIANCE.NS", InvestmentType.STOCK))
                .thenThrow(new RuntimeException("timeout"));

        PriceProviderStrategy fallback = provider(true, new BigDecimal("999.00"));

        PriceProviderService service = new PriceProviderService(java.util.List.of(throwing, fallback));
        assertThat(service.fetchPrice("RELIANCE.NS", InvestmentType.STOCK)).isEqualByComparingTo("999.00");
    }
}
