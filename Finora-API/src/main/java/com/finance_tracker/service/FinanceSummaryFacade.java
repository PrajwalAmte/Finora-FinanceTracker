package com.finance_tracker.service;

import com.finance_tracker.dto.ExpenseSummaryDTO;
import com.finance_tracker.dto.InvestmentSummaryDTO;
import com.finance_tracker.dto.LoanSummaryDTO;
import com.finance_tracker.dto.SipSummaryDTO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class FinanceSummaryFacade {
    
    private static final Logger logger = LoggerFactory.getLogger(FinanceSummaryFacade.class);
    
    private final ExpenseService expenseService;
    private final InvestmentService investmentService;
    private final LoanService loanService;
    private final SipService sipService;

    public ComprehensiveFinanceSummary getComprehensiveSummary(LocalDate startDate, LocalDate endDate) {
        logger.info("Generating comprehensive finance summary");

        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        ExpenseSummaryDTO expenseSummary = getExpenseSummary(startDate, endDate);
        InvestmentSummaryDTO investmentSummary = getInvestmentSummary();
        LoanSummaryDTO loanSummary = getLoanSummary();
        SipSummaryDTO sipSummary = getSipSummary();

        // investmentSummary.totalValue = non-SIP investments only (SIP-linked MF rows are excluded).
        // sipSummary.totalCurrentValue  = linked MF investment values + standalone SIP values.
        // Together they cover all holdings exactly once.
        BigDecimal totalAssets = investmentSummary.getTotalValue()
                .add(sipSummary.getTotalCurrentValue());
        BigDecimal totalLiabilities = loanSummary.getTotalBalance();
        BigDecimal netWorth = totalAssets.subtract(totalLiabilities);

        BigDecimal averageMonthlyExpense = expenseService.getAverageMonthlyExpense(null);
        
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
        // Exclude SIP-linked MF investments — their value is counted in sipSummary instead.
        var sipLinkedIds    = sipService.getLinkedInvestmentIds();
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

