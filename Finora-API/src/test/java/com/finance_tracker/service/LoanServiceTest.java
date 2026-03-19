package com.finance_tracker.service;

import com.finance_tracker.exception.ResourceNotFoundException;
import com.finance_tracker.model.CompoundingFrequency;
import com.finance_tracker.model.Loan;
import com.finance_tracker.model.LoanInterestType;
import com.finance_tracker.repository.LoanRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private LedgerService ledgerService;

    @InjectMocks
    private LoanService loanService;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        setupAuth(USER_ID);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setupAuth(Long userId) {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn(userId.toString());
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    private Loan buildLoan(Long id, Long userId) {
        Loan l = new Loan();
        l.setId(id);
        l.setUserId(userId);
        l.setName("Home Loan");
        l.setPrincipalAmount(new BigDecimal("100000.00"));
        l.setInterestRate(new BigDecimal("8.50"));
        l.setInterestType(LoanInterestType.SIMPLE);
        l.setCompoundingFrequency(CompoundingFrequency.MONTHLY);
        l.setTenureMonths(120);
        l.setCurrentBalance(new BigDecimal("90000.00"));
        l.setStartDate(LocalDate.of(2023, 1, 1));
        l.setLastUpdated(LocalDate.of(2024, 1, 1));
        return l;
    }

    // ── getAllLoans ───────────────────────────────────────────────────────────

    @Test
    void getAllLoans_returnsUserLoans() {
        when(loanRepository.findByUserId(USER_ID)).thenReturn(List.of(buildLoan(1L, USER_ID)));

        assertThat(loanService.getAllLoans()).hasSize(1);
    }

    // ── getLoanById ───────────────────────────────────────────────────────────

    @Test
    void getLoanById_found_returnsLoan() {
        Loan l = buildLoan(1L, USER_ID);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(l));

        assertThat(loanService.getLoanById(1L)).isEqualTo(l);
    }

    @Test
    void getLoanById_notFound_throws() {
        when(loanRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.getLoanById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getLoanById_differentOwner_throws() {
        Loan l = buildLoan(1L, 999L);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(l));

        assertThatThrownBy(() -> loanService.getLoanById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Loan not found");
    }

    // ── findLoanById ──────────────────────────────────────────────────────────

    @Test
    void findLoanById_found_returnsOptional() {
        Loan l = buildLoan(1L, USER_ID);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(l));

        assertThat(loanService.findLoanById(1L)).contains(l);
    }

    @Test
    void findLoanById_notFound_returnsEmpty() {
        when(loanRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(loanService.findLoanById(99L)).isEmpty();
    }

    // ── saveLoan (create) ─────────────────────────────────────────────────────

    @Test
    void saveLoan_create_setsUserIdAndDefaultDate() {
        Loan l = buildLoan(null, null);
        l.setLastUpdated(null);
        l.setEmiAmount(null);
        Loan saved = buildLoan(1L, USER_ID);
        when(loanRepository.save(any())).thenReturn(saved);

        Loan result = loanService.saveLoan(l);

        assertThat(l.getUserId()).isEqualTo(USER_ID);
        assertThat(l.getLastUpdated()).isNotNull();
        verify(ledgerService).recordEvent(eq("LOAN"), any(), eq("CREATE"), isNull(), eq(saved), any());
    }

    @Test
    void saveLoan_create_calculatesEmiAndBalance_whenMissing() {
        Loan l = new Loan();
        l.setName("Car Loan");
        l.setPrincipalAmount(new BigDecimal("500000"));
        l.setInterestRate(new BigDecimal("9.00"));
        l.setTenureMonths(60);
        l.setInterestType(LoanInterestType.SIMPLE);
        l.setCompoundingFrequency(CompoundingFrequency.MONTHLY);

        Loan saved = buildLoan(1L, USER_ID);
        when(loanRepository.save(any())).thenReturn(saved);

        loanService.saveLoan(l);

        assertThat(l.getEmiAmount()).isNotNull().isPositive();
        assertThat(l.getCurrentBalance()).isEqualByComparingTo("500000");
    }

    @Test
    void saveLoan_create_doesNotOverwriteExistingEmi() {
        Loan l = buildLoan(null, null);
        l.setId(null);
        l.setEmiAmount(new BigDecimal("1500.00"));
        Loan saved = buildLoan(1L, USER_ID);
        when(loanRepository.save(any())).thenReturn(saved);

        loanService.saveLoan(l);

        assertThat(l.getEmiAmount()).isEqualByComparingTo("1500.00");
    }

    // ── saveLoan (update) ─────────────────────────────────────────────────────

    @Test
    void saveLoan_update_recordsUpdateEvent() {
        Loan before = buildLoan(1L, USER_ID);
        Loan update = buildLoan(1L, USER_ID);
        update.setName("Updated");

        when(loanRepository.findById(1L)).thenReturn(Optional.of(before));
        when(loanRepository.save(any())).thenReturn(update);

        loanService.saveLoan(update);

        verify(ledgerService).recordEvent(eq("LOAN"), any(), eq("UPDATE"), eq(before), eq(update), any());
    }

    @Test
    void saveLoan_update_differentOwner_throws() {
        Loan owned = buildLoan(1L, 999L);
        Loan update = buildLoan(1L, USER_ID);

        when(loanRepository.findById(1L)).thenReturn(Optional.of(owned));

        assertThatThrownBy(() -> loanService.saveLoan(update))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── deleteLoan ────────────────────────────────────────────────────────────

    @Test
    void deleteLoan_success_deletesAndRecords() {
        Loan l = buildLoan(1L, USER_ID);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(l));

        loanService.deleteLoan(1L);

        verify(loanRepository).deleteById(1L);
        verify(ledgerService).recordEvent(eq("LOAN"), eq("1"), eq("DELETE"), eq(l), isNull(), any());
    }

    @Test
    void deleteLoan_notFound_throws() {
        when(loanRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.deleteLoan(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteLoan_differentOwner_throws() {
        Loan l = buildLoan(1L, 999L);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(l));

        assertThatThrownBy(() -> loanService.deleteLoan(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getTotalLoanBalance ───────────────────────────────────────────────────

    @Test
    void getTotalLoanBalance_sumsCurrentBalances() {
        when(loanRepository.sumCurrentBalanceByUserId(USER_ID)).thenReturn(new BigDecimal("80000.00"));

        assertThat(loanService.getTotalLoanBalance()).isEqualByComparingTo("80000.00");
    }

    @Test
    void getTotalLoanBalance_noLoans_returnsZero() {
        when(loanRepository.sumCurrentBalanceByUserId(USER_ID)).thenReturn(BigDecimal.ZERO);

        assertThat(loanService.getTotalLoanBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── updateLoanBalances ────────────────────────────────────────────────────

    @Test
    void updateLoanBalances_skipsIfUpdatedToday() {
        Loan l = buildLoan(1L, USER_ID);
        l.setLastUpdated(LocalDate.now());
        when(loanRepository.findAll()).thenReturn(List.of(l));

        loanService.updateLoanBalances();

        verify(loanRepository, never()).save(any());
    }

    @Test
    void updateLoanBalances_skipsIfNoLastUpdatedAndNoStartDate() {
        Loan l = buildLoan(1L, USER_ID);
        l.setLastUpdated(null);
        l.setStartDate(null);
        when(loanRepository.findAll()).thenReturn(List.of(l));

        loanService.updateLoanBalances();

        verify(loanRepository, never()).save(any());
    }

    @Test
    void updateLoanBalances_simpleInterest_updatesBalance() {
        Loan l = buildLoan(1L, USER_ID);
        l.setInterestType(LoanInterestType.SIMPLE);
        l.setCompoundingFrequency(CompoundingFrequency.MONTHLY);
        l.setInterestRate(new BigDecimal("12.00"));
        l.setEmiAmount(new BigDecimal("9000.00"));
        l.setCurrentBalance(new BigDecimal("100000.00"));
        l.setLastUpdated(LocalDate.now().minusMonths(3));

        when(loanRepository.findAll()).thenReturn(List.of(l));
        when(loanRepository.save(any())).thenReturn(l);

        loanService.updateLoanBalances();

        verify(loanRepository).save(l);
        assertThat(l.getLastUpdated()).isEqualTo(LocalDate.now());
    }

    @Test
    void updateLoanBalances_compoundQuarterly_updatesBalance() {
        Loan l = buildLoan(1L, USER_ID);
        l.setInterestType(LoanInterestType.COMPOUND);
        l.setCompoundingFrequency(CompoundingFrequency.QUARTERLY);
        l.setInterestRate(new BigDecimal("8.00"));
        l.setEmiAmount(new BigDecimal("5000.00"));
        l.setCurrentBalance(new BigDecimal("50000.00"));
        l.setLastUpdated(LocalDate.now().minusMonths(2));

        when(loanRepository.findAll()).thenReturn(List.of(l));
        when(loanRepository.save(any())).thenReturn(l);

        loanService.updateLoanBalances();

        verify(loanRepository).save(l);
    }

    @Test
    void updateLoanBalances_compoundYearly_updatesBalance() {
        Loan l = buildLoan(1L, USER_ID);
        l.setInterestType(LoanInterestType.COMPOUND);
        l.setCompoundingFrequency(CompoundingFrequency.YEARLY);
        l.setInterestRate(new BigDecimal("10.00"));
        l.setEmiAmount(new BigDecimal("4500.00"));
        l.setCurrentBalance(new BigDecimal("40000.00"));
        l.setLastUpdated(LocalDate.now().minusMonths(2));

        when(loanRepository.findAll()).thenReturn(List.of(l));
        when(loanRepository.save(any())).thenReturn(l);

        loanService.updateLoanBalances();

        verify(loanRepository).save(l);
    }

    @Test
    void updateLoanBalances_balanceDropsToZero_stops() {
        Loan l = buildLoan(1L, USER_ID);
        l.setInterestType(LoanInterestType.SIMPLE);
        l.setCompoundingFrequency(CompoundingFrequency.MONTHLY);
        l.setEmiAmount(new BigDecimal("200000.00"));
        l.setCurrentBalance(new BigDecimal("100.00"));
        l.setLastUpdated(LocalDate.now().minusMonths(1));

        when(loanRepository.findAll()).thenReturn(List.of(l));
        when(loanRepository.save(any())).thenReturn(l);

        loanService.updateLoanBalances();

        assertThat(l.getCurrentBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void updateLoanBalances_zeroMonthsPassed_doesNotSave() {
        Loan l = buildLoan(1L, USER_ID);
        l.setLastUpdated(LocalDate.now().minusDays(15));
        when(loanRepository.findAll()).thenReturn(List.of(l));

        loanService.updateLoanBalances();

        verify(loanRepository, never()).save(any());
    }

    @Test
    void updateLoanBalances_usesStartDateWhenLastUpdatedNull() {
        Loan l = buildLoan(1L, USER_ID);
        l.setLastUpdated(null);
        l.setStartDate(LocalDate.now().minusMonths(2));
        l.setInterestType(LoanInterestType.SIMPLE);
        l.setCompoundingFrequency(CompoundingFrequency.MONTHLY);
        l.setEmiAmount(new BigDecimal("5000.00"));
        l.setCurrentBalance(new BigDecimal("50000.00"));

        when(loanRepository.findAll()).thenReturn(List.of(l));
        when(loanRepository.save(any())).thenReturn(l);

        loanService.updateLoanBalances();

        verify(loanRepository).save(l);
    }

    // ── bulkDelete ────────────────────────────────────────────────────────────

    @Test
    void bulkDelete_skipsNotFound_countsDeleted() {
        Loan l = buildLoan(2L, USER_ID);
        when(loanRepository.findById(1L)).thenReturn(Optional.empty());
        when(loanRepository.findById(2L)).thenReturn(Optional.of(l));

        int count = loanService.bulkDelete(List.of(1L, 2L));

        assertThat(count).isEqualTo(1);
        verify(loanRepository, never()).deleteById(1L);
        verify(loanRepository).deleteById(2L);
    }

    @Test
    void bulkDelete_differentOwner_throws() {
        Loan l = buildLoan(1L, 999L);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(l));

        assertThatThrownBy(() -> loanService.bulkDelete(List.of(1L)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void bulkDelete_emptyList_returnsZero() {
        assertThat(loanService.bulkDelete(List.of())).isEqualTo(0);
    }
}
