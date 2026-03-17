package com.finance_tracker.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SipModelTest {

    private Sip buildSip(BigDecimal totalUnits, BigDecimal currentNav,
                          BigDecimal totalInvested, BigDecimal monthlyAmount) {
        Sip s = new Sip();
        s.setTotalUnits(totalUnits);
        s.setCurrentNav(currentNav);
        s.setTotalInvested(totalInvested);
        s.setMonthlyAmount(monthlyAmount);
        return s;
    }

    // ── getCurrentValue ───────────────────────────────────────────────────────

    @Test
    void getCurrentValue_returnsUnitsTimesNav() {
        Sip s = buildSip(new BigDecimal("100"), new BigDecimal("50.00"), null, null);
        assertThat(s.getCurrentValue()).isEqualByComparingTo("5000.00");
    }

    @Test
    void getCurrentValue_nullUnits_returnsZero() {
        Sip s = buildSip(null, new BigDecimal("50.00"), null, null);
        assertThat(s.getCurrentValue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getCurrentValue_nullNav_returnsZero() {
        Sip s = buildSip(new BigDecimal("100"), null, null, null);
        assertThat(s.getCurrentValue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── getTotalInvested ──────────────────────────────────────────────────────

    @Test
    void getTotalInvested_nonNull_returnsValue() {
        Sip s = buildSip(null, null, new BigDecimal("12000.00"), null);
        assertThat(s.getTotalInvested()).isEqualByComparingTo("12000.00");
    }

    @Test
    void getTotalInvested_null_returnsZero() {
        Sip s = buildSip(null, null, null, null);
        assertThat(s.getTotalInvested()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── getCompletedInstallments ──────────────────────────────────────────────

    @Test
    void getCompletedInstallments_dividesTotalByMonthly() {
        Sip s = buildSip(null, null, new BigDecimal("15000.00"), new BigDecimal("5000.00"));
        assertThat(s.getCompletedInstallments()).isEqualTo(3);
    }

    @Test
    void getCompletedInstallments_zeroMonthlyAmount_returnsZero() {
        Sip s = buildSip(null, null, new BigDecimal("15000.00"), BigDecimal.ZERO);
        assertThat(s.getCompletedInstallments()).isEqualTo(0);
    }

    @Test
    void getCompletedInstallments_nullMonthlyAmount_returnsZero() {
        Sip s = buildSip(null, null, new BigDecimal("15000.00"), null);
        assertThat(s.getCompletedInstallments()).isEqualTo(0);
    }

    @Test
    void getCompletedInstallments_nullTotalInvested_returnsZero() {
        Sip s = buildSip(null, null, null, new BigDecimal("5000.00"));
        assertThat(s.getCompletedInstallments()).isEqualTo(0);
    }

    @Test
    void getCompletedInstallments_roundsDown() {
        Sip s = buildSip(null, null, new BigDecimal("7000.00"), new BigDecimal("5000.00"));
        assertThat(s.getCompletedInstallments()).isEqualTo(1);
    }

    // ── getProfitLoss ─────────────────────────────────────────────────────────

    @Test
    void getProfitLoss_positiveGain() {
        Sip s = buildSip(new BigDecimal("100"), new BigDecimal("120.00"),
                new BigDecimal("10000.00"), new BigDecimal("1000.00"));
        assertThat(s.getProfitLoss()).isEqualByComparingTo("2000.00");
    }

    @Test
    void getProfitLoss_loss() {
        Sip s = buildSip(new BigDecimal("100"), new BigDecimal("80.00"),
                new BigDecimal("10000.00"), new BigDecimal("1000.00"));
        assertThat(s.getProfitLoss()).isEqualByComparingTo("-2000.00");
    }

    @Test
    void getProfitLoss_zeroGain() {
        Sip s = buildSip(new BigDecimal("100"), new BigDecimal("100.00"),
                new BigDecimal("10000.00"), new BigDecimal("1000.00"));
        assertThat(s.getProfitLoss()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
