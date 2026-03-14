package com.finance_tracker.config;

import com.finance_tracker.service.LoanService;
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

    private final LoanService loanService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("Startup: recalculating loan balances");
        try {
            loanService.updateLoanBalances();
            logger.info("Startup loan balance update complete");
        } catch (Exception e) {
            logger.error("Startup loan balance update failed (non-fatal): {}", e.getMessage(), e);
        }
    }
}
