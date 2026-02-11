package com.finance_tracker.utils.scheduler;

import com.finance_tracker.service.InvestmentService;
import com.finance_tracker.service.LoanService;
import com.finance_tracker.service.SipService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@EnableScheduling
public class FinanceDataUpdateScheduler {
    private static final Logger logger = LoggerFactory.getLogger(FinanceDataUpdateScheduler.class);

    @Autowired
    private InvestmentService investmentService;

    @Autowired
    private SipService sipService;

    @Autowired
    private LoanService loanService;

    // Update stock and mutual fund prices 4 times a day: 9 AM, 12 PM, 3 PM, and 6 PM
    @Scheduled(cron = "0 0 9,12,15,18 * * *")
    public void updateInvestmentPrices() {
        logger.info("Attempting to update investment prices");
        try {
            investmentService.updateCurrentPrices();
            logger.info("Successfully updated investment prices");
        } catch (Exception e) {
            logger.error("Failed to update investment prices: {}", e.getMessage());
        }
    }

    // Update SIP NAVs 4 times a day: 9 AM, 12 PM, 3 PM, and 6 PM
    @Scheduled(cron = "0 0 9,12,15,18 * * *")
    public void updateSipNavs() {
        logger.info("Attempting to update SIP NAVs");
        try {
            sipService.updateCurrentNavs();
            logger.info("Successfully updated SIP NAVs");
        } catch (Exception e) {
            logger.error("Failed to update SIP NAVs: {}", e.getMessage());
        }
    }

    // Process monthly SIP investments on the 1st of each month
    @Scheduled(cron = "0 0 9 1 * *")
    public void processMonthlySipInvestments() {
        logger.info("Processing monthly SIP investments");
        try {
            sipService.processMonthlyInvestments();
            logger.info("Successfully processed monthly SIP investments");
        } catch (Exception e) {
            logger.error("Failed to process monthly SIP investments: {}", e.getMessage());
        }
    }

    // Update loan balances daily at midnight
    @Scheduled(cron = "0 0 0 * * *")
    public void updateLoanBalances() {
        logger.info("Updating loan balances");
        try {
            loanService.updateLoanBalances();
            logger.info("Successfully updated loan balances");
        } catch (Exception e) {
            logger.error("Failed to update loan balances: {}", e.getMessage());
        }
    }
}