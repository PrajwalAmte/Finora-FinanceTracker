package com.finance_tracker.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class InvestmentModelTest {

    private Investment buildInvestment(BigDecimal qty, BigDecimal purchasePrice, BigDecimal currentPrice) {
        Investment inv = new Investment();
        inv.setQuantity(qty);
        inv.setPurchasePrice(purchasePrice);
        inv.setCurrentPrice(currentPrice);
        return inv;
    }

    // ── getCurrentValue ───────────────────────────────────────────────────────

    @Test
    void getCurrentValue_returnsQtyTimesCurrentPrice() {
        Investment inv = buildInvestment(
                new BigDecimal("10"), new BigDecimal("100"), new BigDecimal("150"));

        assertThat(inv.getCurrentValue()).isEqualByComparingTo("1500");
    }

    @Test
    void getCurrentValue_nullQuantity_returnsZero() {
        Investment inv = buildInvestment(null, new BigDecimal("100"), new BigDecimal("150"));
        assertThat(inv.getCurrentValue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getCurrentValue_nullCurrentPrice_returnsZero() {
        Investment inv = buildInvestment(new BigDecimal("10"), new BigDecimal("100"), null);
        assertThat(inv.getCurrentValue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── getProfitLoss ─────────────────────────────────────────────────────────

    @Test
    void getProfitLoss_positiveProfit() {
        Investment inv = buildInvestment(
                new BigDecimal("10"), new BigDecimal("100"), new BigDecimal("150"));

        assertThat(inv.getProfitLoss()).isEqualByComparingTo("500");
    }

    @Test
    void getProfitLoss_negativeLoss() {
        Investment inv = buildInvestment(
                new BigDecimal("10"), new BigDecimal("100"), new BigDecimal("80"));

        assertThat(inv.getProfitLoss()).isEqualByComparingTo("-200");
    }

    @Test
    void getProfitLoss_nullQuantity_returnsZero() {
        Investment inv = buildInvestment(null, new BigDecimal("100"), new BigDecimal("150"));
        assertThat(inv.getProfitLoss()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getProfitLoss_nullPurchasePrice_returnsZero() {
        Investment inv = buildInvestment(new BigDecimal("10"), null, new BigDecimal("150"));
        assertThat(inv.getProfitLoss()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── getReturnPercentage ───────────────────────────────────────────────────

    @Test
    void getReturnPercentage_positiveReturn() {
        Investment inv = buildInvestment(
                new BigDecimal("10"), new BigDecimal("100"), new BigDecimal("150"));

        assertThat(inv.getReturnPercentage()).isEqualByComparingTo("50.00");
    }

    @Test
    void getReturnPercentage_negativeReturn() {
        Investment inv = buildInvestment(
                new BigDecimal("10"), new BigDecimal("100"), new BigDecimal("80"));

        assertThat(inv.getReturnPercentage()).isEqualByComparingTo("-20.00");
    }

    @Test
    void getReturnPercentage_zeroCostBasis_returnsZero() {
        Investment inv = buildInvestment(
                new BigDecimal("10"), new BigDecimal("0"), new BigDecimal("150"));

        assertThat(inv.getReturnPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getReturnPercentage_nullFields_returnsZero() {
        Investment inv = buildInvestment(null, null, null);
        assertThat(inv.getReturnPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
