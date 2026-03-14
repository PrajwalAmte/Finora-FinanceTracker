package com.finance_tracker.utils.scheduler;

import com.finance_tracker.service.InvestmentService;
import com.finance_tracker.service.LoanService;
import com.finance_tracker.service.SipService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class FinanceDataUpdateScheduler {

    private static final Logger logger = LoggerFactory.getLogger(FinanceDataUpdateScheduler.class);

    private final InvestmentService investmentService;
    private final SipService sipService;
    private final LoanService loanService;

    @Scheduled(cron = "0 0 9,12,15,18 * * *")
    public void updateInvestmentPrices() {
        try {
            investmentService.updateCurrentPrices();
        } catch (Exception e) {
            logger.error("Failed to update investment prices: {}", e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 9,12,15,18 * * *")
    public void updateSipNavs() {
        try {
            sipService.updateCurrentNavs();
        } catch (Exception e) {
            logger.error("Failed to update SIP NAVs: {}", e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 9 1 * *")
    public void processMonthlySipInvestments() {
        try {
            sipService.processMonthlyInvestments();
        } catch (Exception e) {
            logger.error("Failed to process monthly SIP investments: {}", e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void updateLoanBalances() {
        try {
            loanService.updateLoanBalances();
        } catch (Exception e) {
            logger.error("Failed to update loan balances: {}", e.getMessage());
        }
    }
}
