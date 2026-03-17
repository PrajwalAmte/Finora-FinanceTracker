package com.finance_tracker.config;

import com.finance_tracker.service.LoanService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StartupDataInitializerTest {

    @Mock
    private LoanService loanService;

    @InjectMocks
    private StartupDataInitializer initializer;

    @Test
    void run_callsUpdateLoanBalances() throws Exception {
        initializer.run(new DefaultApplicationArguments());
        verify(loanService).updateLoanBalances();
    }

    @Test
    void run_swallowsExceptionFromLoanService() throws Exception {
        doThrow(new RuntimeException("DB unavailable")).when(loanService).updateLoanBalances();
        initializer.run(new DefaultApplicationArguments()); // must not throw
    }
}
