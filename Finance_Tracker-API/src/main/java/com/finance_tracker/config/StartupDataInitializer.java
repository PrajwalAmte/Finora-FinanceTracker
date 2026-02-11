package com.finance_tracker.config;

import com.finance_tracker.service.InvestmentService;
import com.finance_tracker.service.LoanService;
import com.finance_tracker.service.SipService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;


@Component
public class StartupDataInitializer implements ApplicationRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(StartupDataInitializer.class);
    
    private final InvestmentService investmentService;
    private final SipService sipService;
    private final LoanService loanService;
    
    public StartupDataInitializer(InvestmentService investmentService, 
                                   SipService sipService, 
                                   LoanService loanService) {
        this.investmentService = investmentService;
        this.sipService = sipService;
        this.loanService = loanService;
    }
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("Application Startup: Initializing financial data");
        
        try {
            // Update investment prices
            logger.info("Starting initial investment price update...");
            investmentService.updateCurrentPrices();
            logger.info("Investment prices updated successfully on startup");
            
            // Update SIP NAVs
            logger.info("Starting initial SIP NAV update...");
            sipService.updateCurrentNavs();
            logger.info("SIP NAVs updated successfully on startup");
            
            // Update loan balances
            logger.info("Starting initial loan balance update...");
            loanService.updateLoanBalances();
            logger.info("Loan balances updated successfully on startup");
            
            logger.info("Startup data initialization completed successfully");
            logger.info("Scheduled tasks will continue to run as configured");
            
        } catch (Exception e) {
            logger.error("Error during startup data initialization: {}", e.getMessage(), e);
            logger.warn("Application will continue to start despite initialization errors");
        }
    }
}