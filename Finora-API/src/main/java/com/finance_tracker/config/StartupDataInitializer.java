package com.finance_tracker.config;

import com.finance_tracker.service.InvestmentService;
import com.finance_tracker.service.LoanService;
import com.finance_tracker.service.SipService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StartupDataInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(StartupDataInitializer.class);

    private final InvestmentService investmentService;
    private final SipService sipService;
    private final LoanService loanService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("Startup: refreshing investment prices, SIP NAVs, and loan balances");
        try {
            investmentService.updateCurrentPrices();
            sipService.updateCurrentNavs();
            loanService.updateLoanBalances();
            logger.info("Startup data refresh complete");
        } catch (Exception e) {
            logger.error("Startup data refresh failed (non-fatal): {}", e.getMessage(), e);
        }
    }
}
