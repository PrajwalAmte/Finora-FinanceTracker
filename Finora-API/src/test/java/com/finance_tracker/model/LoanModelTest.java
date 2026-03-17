package com.finance_tracker.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class LoanModelTest {

    private Loan buildLoan(LocalDate startDate, Integer tenureMonths,
                            BigDecimal emiAmount, BigDecimal principal) {
        Loan l = new Loan();
        l.setStartDate(startDate);
        l.setTenureMonths(tenureMonths);
        l.setEmiAmount(emiAmount);
        l.setPrincipalAmount(principal);
        return l;
    }

    // ── getEndDate ────────────────────────────────────────────────────────────

    @Test
    void getEndDate_returnsStartPlusTenure() {
        Loan l = buildLoan(LocalDate.of(2023, 1, 1), 12, null, null);
        assertThat(l.getEndDate()).isEqualTo(LocalDate.of(2024, 1, 1));
    }

    @Test
    void getEndDate_nullStartDate_returnsNull() {
        Loan l = buildLoan(null, 12, null, null);
        assertThat(l.getEndDate()).isNull();
    }

    @Test
    void getEndDate_nullTenure_returnsNull() {
        Loan l = buildLoan(LocalDate.of(2023, 1, 1), null, null, null);
        assertThat(l.getEndDate()).isNull();
    }

    // ── getRemainingMonths ────────────────────────────────────────────────────

    @Test
    void getRemainingMonths_futureEnd_returnsPositive() {
        LocalDate futureEnd = LocalDate.now().plusMonths(6);
        Loan l = buildLoan(futureEnd.minusMonths(12), 12, null, null);
        assertThat(l.getRemainingMonths()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void getRemainingMonths_pastEnd_returnsNegativeOrZero() {
        Loan l = buildLoan(LocalDate.of(2020, 1, 1), 12, null, null);
        assertThat(l.getRemainingMonths()).isLessThanOrEqualTo(0);
    }

    @Test
    void getRemainingMonths_nullEndDate_returnsNull() {
        Loan l = buildLoan(null, null, null, null);
        assertThat(l.getRemainingMonths()).isNull();
    }

    // ── getTotalRepayment ─────────────────────────────────────────────────────

    @Test
    void getTotalRepayment_emiTimesMonths() {
        Loan l = buildLoan(LocalDate.now(), 24, new BigDecimal("5000.00"), new BigDecimal("100000.00"));
        assertThat(l.getTotalRepayment()).isEqualByComparingTo("120000.00");
    }

    @Test
    void getTotalRepayment_nullEmi_returnsNull() {
        Loan l = buildLoan(LocalDate.now(), 24, null, new BigDecimal("100000.00"));
        assertThat(l.getTotalRepayment()).isNull();
    }

    @Test
    void getTotalRepayment_nullTenure_returnsNull() {
        Loan l = buildLoan(LocalDate.now(), null, new BigDecimal("5000.00"), new BigDecimal("100000.00"));
        assertThat(l.getTotalRepayment()).isNull();
    }

    // ── getTotalInterest ──────────────────────────────────────────────────────

    @Test
    void getTotalInterest_repaymentMinusPrincipal() {
        Loan l = buildLoan(LocalDate.now(), 24, new BigDecimal("5000.00"), new BigDecimal("100000.00"));
        assertThat(l.getTotalInterest()).isEqualByComparingTo("20000.00");
    }

    @Test
    void getTotalInterest_nullEmi_returnsNull() {
        Loan l = buildLoan(LocalDate.now(), 24, null, new BigDecimal("100000.00"));
        assertThat(l.getTotalInterest()).isNull();
    }

    @Test
    void getTotalInterest_nullPrincipal_returnsNull() {
        Loan l = buildLoan(LocalDate.now(), 24, new BigDecimal("5000.00"), null);
        assertThat(l.getTotalInterest()).isNull();
    }
}
