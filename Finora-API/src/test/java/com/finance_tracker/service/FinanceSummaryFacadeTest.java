package com.finance_tracker.service;

import com.finance_tracker.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinanceSummaryFacadeTest {

    @Mock
    private ExpenseService expenseService;

    @Mock
    private InvestmentService investmentService;

    @Mock
    private LoanService loanService;

    @Mock
    private SipService sipService;

    @InjectMocks
    private FinanceSummaryFacade facade;

    // ── getComprehensiveSummary ───────────────────────────────────────────────

    @Test
    void getComprehensiveSummary_withNullDates_usesCurrentMonthDefaults() {
        stubSummaryServices(
                new BigDecimal("3000.00"), Map.of(),
                new BigDecimal("10000.00"), new BigDecimal("1000.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("8000.00"), new BigDecimal("7000.00"),
                new BigDecimal("500.00"));

        FinanceSummaryFacade.ComprehensiveFinanceSummary result =
                facade.getComprehensiveSummary(null, null);

        assertThat(result.getNetWorth()).isEqualByComparingTo("13000.00"); // (10000+8000) - 5000
        assertThat(result.getTotalAssets()).isEqualByComparingTo("18000.00");
        assertThat(result.getTotalLiabilities()).isEqualByComparingTo("5000.00");
        assertThat(result.getAverageMonthlyExpense()).isEqualByComparingTo("500.00");
    }

    @Test
    void getComprehensiveSummary_withExplicitDates_passesDatesToExpenseService() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);

        stubSummaryServices(
                BigDecimal.ZERO, Map.of(),
                BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO);

        facade.getComprehensiveSummary(start, end);

        verify(expenseService).getTotalExpenses(start, end);
        verify(expenseService).getExpensesByCategory(start, end);
    }

    @Test
    void getComprehensiveSummary_netWorthCorrectWhenLiabilitiesExceedAssets() {
        stubSummaryServices(
                BigDecimal.ZERO, Map.of(),
                new BigDecimal("5000.00"), BigDecimal.ZERO,
                new BigDecimal("20000.00"),
                new BigDecimal("3000.00"), new BigDecimal("2000.00"),
                BigDecimal.ZERO);

        FinanceSummaryFacade.ComprehensiveFinanceSummary result =
                facade.getComprehensiveSummary(null, null);

        assertThat(result.getNetWorth()).isNegative();
    }

    // ── getExpenseSummary ─────────────────────────────────────────────────────

    @Test
    void getExpenseSummary_populatesDTO() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);
        when(expenseService.getTotalExpenses(start, end)).thenReturn(new BigDecimal("1500.00"));
        when(expenseService.getExpensesByCategory(start, end))
                .thenReturn(Map.of("Food", new BigDecimal("500.00"), "Travel", new BigDecimal("1000.00")));

        ExpenseSummaryDTO dto = facade.getExpenseSummary(start, end);

        assertThat(dto.getTotalExpenses()).isEqualByComparingTo("1500.00");
        assertThat(dto.getExpensesByCategory()).containsKey("Food");
    }

    // ── getInvestmentSummary ──────────────────────────────────────────────────

    @Test
    void getInvestmentSummary_excludesSipLinkedIds() {
        when(sipService.getLinkedInvestmentIds()).thenReturn(List.of(5L, 6L));
        when(investmentService.getTotalInvestmentValueExcluding(List.of(5L, 6L)))
                .thenReturn(new BigDecimal("25000.00"));
        when(investmentService.getTotalProfitLossExcluding(List.of(5L, 6L)))
                .thenReturn(new BigDecimal("3000.00"));

        InvestmentSummaryDTO dto = facade.getInvestmentSummary();

        assertThat(dto.getTotalValue()).isEqualByComparingTo("25000.00");
        assertThat(dto.getTotalProfitLoss()).isEqualByComparingTo("3000.00");
    }

    // ── getLoanSummary ────────────────────────────────────────────────────────

    @Test
    void getLoanSummary_populatesDTO() {
        when(loanService.getTotalLoanBalance()).thenReturn(new BigDecimal("45000.00"));

        LoanSummaryDTO dto = facade.getLoanSummary();

        assertThat(dto.getTotalBalance()).isEqualByComparingTo("45000.00");
    }

    // ── getSipSummary ─────────────────────────────────────────────────────────

    @Test
    void getSipSummary_calculatesCorrectProfitLoss() {
        when(sipService.getTotalSipValue()).thenReturn(new BigDecimal("12000.00"));
        when(sipService.getTotalSipInvestment()).thenReturn(new BigDecimal("10000.00"));

        SipSummaryDTO dto = facade.getSipSummary();

        assertThat(dto.getTotalCurrentValue()).isEqualByComparingTo("12000.00");
        assertThat(dto.getTotalInvestment()).isEqualByComparingTo("10000.00");
        assertThat(dto.getTotalProfitLoss()).isEqualByComparingTo("2000.00");
    }

    @Test
    void getSipSummary_negativeProfitLossWhenValueBelowInvested() {
        when(sipService.getTotalSipValue()).thenReturn(new BigDecimal("8000.00"));
        when(sipService.getTotalSipInvestment()).thenReturn(new BigDecimal("10000.00"));

        SipSummaryDTO dto = facade.getSipSummary();

        assertThat(dto.getTotalProfitLoss()).isEqualByComparingTo("-2000.00");
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private void stubSummaryServices(
            BigDecimal totalExpenses, Map<String, BigDecimal> expensesByCategory,
            BigDecimal investmentValue, BigDecimal investmentProfitLoss,
            BigDecimal loanBalance,
            BigDecimal sipValue, BigDecimal sipInvestment,
            BigDecimal avgMonthlyExpense) {

        when(expenseService.getTotalExpenses(any(), any())).thenReturn(totalExpenses);
        when(expenseService.getExpensesByCategory(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(expensesByCategory);
        when(expenseService.getAverageMonthlyExpense(null)).thenReturn(avgMonthlyExpense);

        when(sipService.getLinkedInvestmentIds()).thenReturn(List.of());
        when(investmentService.getTotalInvestmentValueExcluding(any())).thenReturn(investmentValue);
        when(investmentService.getTotalProfitLossExcluding(any())).thenReturn(investmentProfitLoss);

        when(loanService.getTotalLoanBalance()).thenReturn(loanBalance);

        when(sipService.getTotalSipValue()).thenReturn(sipValue);
        when(sipService.getTotalSipInvestment()).thenReturn(sipInvestment);
    }
}
