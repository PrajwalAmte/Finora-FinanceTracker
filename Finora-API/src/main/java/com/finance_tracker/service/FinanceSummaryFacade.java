package com.finance_tracker.service;

import com.finance_tracker.dto.ExpenseSummaryDTO;
import com.finance_tracker.dto.InvestmentSummaryDTO;
import com.finance_tracker.dto.LoanSummaryDTO;
import com.finance_tracker.dto.SipSummaryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class FinanceSummaryFacade {

    private final ExpenseService expenseService;
    private final InvestmentService investmentService;
    private final LoanService loanService;
    private final SipService sipService;

    private <T> CompletableFuture<T> runAsync(Supplier<T> supplier) {
        SecurityContext context = SecurityContextHolder.getContext();
        return CompletableFuture.supplyAsync(() -> {
            SecurityContextHolder.setContext(context);
            try {
                return supplier.get();
            } finally {
                SecurityContextHolder.clearContext();
            }
        });
    }

    public ComprehensiveFinanceSummary getComprehensiveSummary(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        final LocalDate start = startDate;
        final LocalDate end = endDate;

        CompletableFuture<ExpenseSummaryDTO> expenseFuture = runAsync(() -> getExpenseSummary(start, end));
        CompletableFuture<InvestmentSummaryDTO> investmentFuture = runAsync(this::getInvestmentSummary);
        CompletableFuture<LoanSummaryDTO> loanFuture = runAsync(this::getLoanSummary);
        CompletableFuture<SipSummaryDTO> sipFuture = runAsync(this::getSipSummary);
        CompletableFuture<BigDecimal> avgExpenseFuture = runAsync(() -> expenseService.getAverageMonthlyExpense(null));

        CompletableFuture.allOf(expenseFuture, investmentFuture, loanFuture, sipFuture, avgExpenseFuture).join();

        ExpenseSummaryDTO expenseSummary = expenseFuture.join();
        InvestmentSummaryDTO investmentSummary = investmentFuture.join();
        LoanSummaryDTO loanSummary = loanFuture.join();
        SipSummaryDTO sipSummary = sipFuture.join();
        BigDecimal averageMonthlyExpense = avgExpenseFuture.join();

        BigDecimal totalAssets = investmentSummary.getTotalValue()
                .add(sipSummary.getTotalCurrentValue());
        BigDecimal totalLiabilities = loanSummary.getTotalBalance();
        BigDecimal netWorth = totalAssets.subtract(totalLiabilities);

        return ComprehensiveFinanceSummary.builder()
                .expenseSummary(expenseSummary)
                .investmentSummary(investmentSummary)
                .loanSummary(loanSummary)
                .sipSummary(sipSummary)
                .totalAssets(totalAssets)
                .totalLiabilities(totalLiabilities)
                .netWorth(netWorth)
                .averageMonthlyExpense(averageMonthlyExpense)
                .build();
    }

    public ExpenseSummaryDTO getExpenseSummary(LocalDate startDate, LocalDate endDate) {
        BigDecimal totalExpenses = expenseService.getTotalExpenses(startDate, endDate);
        var expensesByCategory = expenseService.getExpensesByCategory(startDate, endDate);
        
        return ExpenseSummaryDTO.builder()
                .totalExpenses(totalExpenses)
                .expensesByCategory(expensesByCategory)
                .build();
    }

    public InvestmentSummaryDTO getInvestmentSummary() {
        var sipLinkedIds = sipService.getLinkedInvestmentIds();
        BigDecimal totalValue      = investmentService.getTotalInvestmentValueExcluding(sipLinkedIds);
        BigDecimal totalProfitLoss = investmentService.getTotalProfitLossExcluding(sipLinkedIds);
        
        return InvestmentSummaryDTO.builder()
                .totalValue(totalValue)
                .totalProfitLoss(totalProfitLoss)
                .build();
    }

    public LoanSummaryDTO getLoanSummary() {
        BigDecimal totalBalance = loanService.getTotalLoanBalance();
        
        return LoanSummaryDTO.builder()
                .totalBalance(totalBalance)
                .build();
    }

    public SipSummaryDTO getSipSummary() {
        BigDecimal totalValue = sipService.getTotalSipValue();
        BigDecimal totalInvestment = sipService.getTotalSipInvestment();
        BigDecimal totalProfitLoss = totalValue.subtract(totalInvestment);
        
        return SipSummaryDTO.builder()
                .totalInvestment(totalInvestment)
                .totalCurrentValue(totalValue)
                .totalProfitLoss(totalProfitLoss)
                .build();
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ComprehensiveFinanceSummary {
        private ExpenseSummaryDTO expenseSummary;
        private InvestmentSummaryDTO investmentSummary;
        private LoanSummaryDTO loanSummary;
        private SipSummaryDTO sipSummary;
        private BigDecimal totalAssets;
        private BigDecimal totalLiabilities;
        private BigDecimal netWorth;
        private BigDecimal averageMonthlyExpense;
    }
}

