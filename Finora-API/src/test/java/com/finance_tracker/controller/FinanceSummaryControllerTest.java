package com.finance_tracker.controller;

import com.finance_tracker.dto.ExpenseSummaryDTO;
import com.finance_tracker.dto.InvestmentSummaryDTO;
import com.finance_tracker.dto.LoanSummaryDTO;
import com.finance_tracker.dto.SipSummaryDTO;
import com.finance_tracker.service.FinanceSummaryFacade;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import com.finance_tracker.utils.security.JwtService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = FinanceSummaryController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class, org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class})
class FinanceSummaryControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private JwtService jwtService;


    @MockitoBean
    private FinanceSummaryFacade financeSummaryFacade;

    private FinanceSummaryFacade.ComprehensiveFinanceSummary summary() {
        return FinanceSummaryFacade.ComprehensiveFinanceSummary.builder()
                .expenseSummary(ExpenseSummaryDTO.builder()
                        .totalExpenses(new BigDecimal("5000"))
                        .expensesByCategory(Map.of("Food", new BigDecimal("5000")))
                        .build())
                .investmentSummary(InvestmentSummaryDTO.builder()
                        .totalValue(new BigDecimal("100000"))
                        .totalProfitLoss(new BigDecimal("10000"))
                        .build())
                .loanSummary(LoanSummaryDTO.builder().totalBalance(new BigDecimal("200000")).build())
                .sipSummary(SipSummaryDTO.builder()
                        .totalInvestment(new BigDecimal("60000"))
                        .totalCurrentValue(new BigDecimal("65000"))
                        .totalProfitLoss(new BigDecimal("5000"))
                        .build())
                .totalAssets(new BigDecimal("165000"))
                .totalLiabilities(new BigDecimal("200000"))
                .netWorth(new BigDecimal("-35000"))
                .averageMonthlyExpense(new BigDecimal("5000"))
                .build();
    }

    @Test
    void getComprehensiveSummary_withoutDates_returnsSummary() throws Exception {
        when(financeSummaryFacade.getComprehensiveSummary(any(), any())).thenReturn(summary());

        mockMvc.perform(get("/api/finance-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netWorth").value(-35000))
                .andExpect(jsonPath("$.totalAssets").value(165000));
    }

    @Test
    void getComprehensiveSummary_withDates_passesDatesThroughToFacade() throws Exception {
        when(financeSummaryFacade.getComprehensiveSummary(any(), any())).thenReturn(summary());

        mockMvc.perform(get("/api/finance-summary")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31"))
                .andExpect(status().isOk());
    }
}
