package com.finance_tracker.utils.scheduler;

import com.finance_tracker.service.LoanService;
import com.finance_tracker.service.SipService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FinanceDataUpdateSchedulerTest {

    @Mock
    private SipService sipService;

    @Mock
    private LoanService loanService;

    @InjectMocks
    private FinanceDataUpdateScheduler scheduler;

    @Test
    void processMonthlySipInvestments_delegatesToSipService() {
        scheduler.processMonthlySipInvestments();
        verify(sipService).processMonthlyInvestments();
    }

    @Test
    void processMonthlySipInvestments_swallowsException() {
        doThrow(new RuntimeException("NAV fetch failed")).when(sipService).processMonthlyInvestments();
        scheduler.processMonthlySipInvestments(); // must not throw
    }

    @Test
    void updateLoanBalances_delegatesToLoanService() {
        scheduler.updateLoanBalances();
        verify(loanService).updateLoanBalances();
    }

    @Test
    void updateLoanBalances_swallowsException() {
        doThrow(new RuntimeException("DB timeout")).when(loanService).updateLoanBalances();
        scheduler.updateLoanBalances(); // must not throw
    }
}
