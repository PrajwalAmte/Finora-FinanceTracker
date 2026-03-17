package com.finance_tracker.service;

import com.finance_tracker.exception.ResourceNotFoundException;
import com.finance_tracker.model.Expense;
import com.finance_tracker.repository.ExpenseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private LedgerService ledgerService;

    @InjectMocks
    private ExpenseService expenseService;

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

    private Expense buildExpense(Long id, Long userId) {
        Expense e = new Expense();
        e.setId(id);
        e.setUserId(userId);
        e.setDescription("Groceries");
        e.setAmount(new BigDecimal("150.00"));
        e.setDate(LocalDate.of(2024, 3, 1));
        e.setCategory("Food");
        e.setPaymentMethod("Cash");
        return e;
    }

    // ── getAllExpenses (pageable) ──────────────────────────────────────────────

    @Test
    void getAllExpenses_pageable_returnsUserPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Expense> page = new PageImpl<>(List.of(buildExpense(1L, USER_ID)));
        when(expenseRepository.findByUserId(USER_ID, pageable)).thenReturn(page);

        Page<Expense> result = expenseService.getAllExpenses(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(expenseRepository).findByUserId(USER_ID, pageable);
    }

    // ── getAllExpenses (list) ─────────────────────────────────────────────────

    @Test
    void getAllExpenses_list_delegatesToRepo() {
        when(expenseRepository.findByUserId(USER_ID))
                .thenReturn(List.of(buildExpense(1L, USER_ID)));

        assertThat(expenseService.getAllExpenses()).hasSize(1);
    }

    // ── getExpenseById ────────────────────────────────────────────────────────

    @Test
    void getExpenseById_found_returnsExpense() {
        Expense e = buildExpense(1L, USER_ID);
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(e));

        assertThat(expenseService.getExpenseById(1L)).isEqualTo(e);
    }

    @Test
    void getExpenseById_notFound_throws() {
        when(expenseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.getExpenseById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getExpenseById_differentOwner_throws() {
        Expense e = buildExpense(1L, 999L);
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(e));

        assertThatThrownBy(() -> expenseService.getExpenseById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Expense not found");
    }

    // ── saveExpense (create) ──────────────────────────────────────────────────

    @Test
    void saveExpense_create_setsUserIdAndDefaultDate() {
        Expense input = new Expense();
        input.setDescription("Dinner");
        input.setAmount(BigDecimal.TEN);
        input.setCategory("Food");
        input.setPaymentMethod("Card");

        Expense saved = buildExpense(1L, USER_ID);
        when(expenseRepository.save(any())).thenReturn(saved);

        Expense result = expenseService.saveExpense(input);

        assertThat(input.getDate()).isNotNull();
        assertThat(input.getUserId()).isEqualTo(USER_ID);
        verify(ledgerService).recordEvent(eq("EXPENSE"), any(), eq("CREATE"), isNull(), eq(saved), any());
    }

    @Test
    void saveExpense_create_preservesExistingDate() {
        LocalDate custom = LocalDate.of(2023, 6, 15);
        Expense input = buildExpense(null, null);
        input.setDate(custom);
        when(expenseRepository.save(any())).thenReturn(input);

        expenseService.saveExpense(input);

        assertThat(input.getDate()).isEqualTo(custom);
    }

    // ── saveExpense (update) ──────────────────────────────────────────────────

    @Test
    void saveExpense_update_recordsUpdateEvent() {
        Expense before = buildExpense(1L, USER_ID);
        Expense updated = buildExpense(1L, USER_ID);
        updated.setDescription("Updated");

        when(expenseRepository.findById(1L)).thenReturn(Optional.of(before));
        when(expenseRepository.save(any())).thenReturn(updated);

        expenseService.saveExpense(updated);

        verify(ledgerService).recordEvent(eq("EXPENSE"), any(), eq("UPDATE"), eq(before), eq(updated), any());
    }

    @Test
    void saveExpense_update_differentOwner_throws() {
        Expense owned = buildExpense(1L, 999L);
        Expense update = buildExpense(1L, USER_ID);

        when(expenseRepository.findById(1L)).thenReturn(Optional.of(owned));

        assertThatThrownBy(() -> expenseService.saveExpense(update))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void saveExpense_update_beforeNotFound_stillSaves() {
        Expense update = buildExpense(1L, USER_ID);
        when(expenseRepository.findById(1L)).thenReturn(Optional.empty());
        when(expenseRepository.save(any())).thenReturn(update);

        Expense result = expenseService.saveExpense(update);

        assertThat(result).isEqualTo(update);
    }

    // ── deleteExpense ─────────────────────────────────────────────────────────

    @Test
    void deleteExpense_success_deletesAndRecordsEvent() {
        Expense e = buildExpense(1L, USER_ID);
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(e));

        expenseService.deleteExpense(1L);

        verify(expenseRepository).deleteById(1L);
        verify(ledgerService).recordEvent(eq("EXPENSE"), eq("1"), eq("DELETE"), eq(e), isNull(), any());
    }

    @Test
    void deleteExpense_notFound_throws() {
        when(expenseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.deleteExpense(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteExpense_differentOwner_throws() {
        Expense e = buildExpense(1L, 999L);
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(e));

        assertThatThrownBy(() -> expenseService.deleteExpense(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getExpensesBetweenDates ───────────────────────────────────────────────

    @Test
    void getExpensesBetweenDates_delegatesToRepo() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);
        when(expenseRepository.findByUserIdAndDateBetween(USER_ID, start, end))
                .thenReturn(List.of(buildExpense(1L, USER_ID)));

        assertThat(expenseService.getExpensesBetweenDates(start, end)).hasSize(1);
    }

    // ── getExpensesByCategory (string) ────────────────────────────────────────

    @Test
    void getExpensesByCategory_string_delegatesToRepo() {
        when(expenseRepository.findByUserIdAndCategory(USER_ID, "Food"))
                .thenReturn(List.of(buildExpense(1L, USER_ID)));

        assertThat(expenseService.getExpensesByCategory("Food")).hasSize(1);
    }

    // ── getTotalExpenses ──────────────────────────────────────────────────────

    @Test
    void getTotalExpenses_returnsRepoValue() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);
        when(expenseRepository.sumExpensesByUserIdBetweenDates(USER_ID, start, end))
                .thenReturn(new BigDecimal("500.00"));

        assertThat(expenseService.getTotalExpenses(start, end)).isEqualByComparingTo("500.00");
    }

    @Test
    void getTotalExpenses_repoReturnsNull_returnsZero() {
        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now();
        when(expenseRepository.sumExpensesByUserIdBetweenDates(USER_ID, start, end)).thenReturn(null);

        assertThat(expenseService.getTotalExpenses(start, end)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── getExpensesByCategory (date range) ────────────────────────────────────

    @Test
    void getExpensesByCategory_dateRange_buildsCategoryMap() {
        LocalDate start = LocalDate.now().minusMonths(1);
        LocalDate end = LocalDate.now();
        Object[] row = {"Food", new BigDecimal("300.00")};
        List<Object[]> rows = new java.util.ArrayList<>();
        rows.add(row);
        when(expenseRepository.sumExpensesByUserIdAndCategoryBetweenDates(USER_ID, start, end))
                .thenReturn(rows);

        Map<String, BigDecimal> result = expenseService.getExpensesByCategory(start, end);

        assertThat(result).containsEntry("Food", new BigDecimal("300.00"));
    }

    @Test
    void getExpensesByCategory_dateRange_emptyRows_returnsEmptyMap() {
        LocalDate start = LocalDate.now().minusMonths(1);
        LocalDate end = LocalDate.now();
        when(expenseRepository.sumExpensesByUserIdAndCategoryBetweenDates(USER_ID, start, end))
                .thenReturn(List.of());

        assertThat(expenseService.getExpensesByCategory(start, end)).isEmpty();
    }

    // ── getAverageMonthlyExpense ──────────────────────────────────────────────

    @Test
    void getAverageMonthlyExpense_noCategory_dividesSumBySix() {
        when(expenseRepository.sumExpensesByUserIdBetweenDates(eq(USER_ID), any(), any()))
                .thenReturn(new BigDecimal("1200.00"));

        assertThat(expenseService.getAverageMonthlyExpense(null)).isEqualByComparingTo("200.00");
    }

    @Test
    void getAverageMonthlyExpense_noCategory_nullFromRepo_returnsZero() {
        when(expenseRepository.sumExpensesByUserIdBetweenDates(eq(USER_ID), any(), any())).thenReturn(null);

        assertThat(expenseService.getAverageMonthlyExpense(null)).isEqualByComparingTo("0.00");
    }

    @Test
    void getAverageMonthlyExpense_emptyCategory_treatedAsNoCategory() {
        when(expenseRepository.sumExpensesByUserIdBetweenDates(eq(USER_ID), any(), any()))
                .thenReturn(new BigDecimal("600.00"));

        assertThat(expenseService.getAverageMonthlyExpense("")).isEqualByComparingTo("100.00");
    }

    @Test
    void getAverageMonthlyExpense_withCategory_sumsFilteredExpenses() {
        Expense e = buildExpense(1L, USER_ID);
        e.setAmount(new BigDecimal("600.00"));
        when(expenseRepository.findByUserIdAndCategoryAndDateBetween(eq(USER_ID), eq("Food"), any(), any()))
                .thenReturn(List.of(e));

        assertThat(expenseService.getAverageMonthlyExpense("Food")).isEqualByComparingTo("100.00");
    }

    @Test
    void getAverageMonthlyExpense_withCategory_noExpenses_returnsZero() {
        when(expenseRepository.findByUserIdAndCategoryAndDateBetween(eq(USER_ID), eq("Travel"), any(), any()))
                .thenReturn(List.of());

        assertThat(expenseService.getAverageMonthlyExpense("Travel")).isEqualByComparingTo("0.00");
    }

    // ── bulkDelete ────────────────────────────────────────────────────────────

    @Test
    void bulkDelete_skipsNotFound_countsOnlyDeleted() {
        Expense e = buildExpense(2L, USER_ID);
        when(expenseRepository.findById(1L)).thenReturn(Optional.empty());
        when(expenseRepository.findById(2L)).thenReturn(Optional.of(e));

        int count = expenseService.bulkDelete(List.of(1L, 2L));

        assertThat(count).isEqualTo(1);
        verify(expenseRepository, never()).deleteById(1L);
        verify(expenseRepository).deleteById(2L);
    }

    @Test
    void bulkDelete_differentOwner_throws() {
        Expense e = buildExpense(1L, 999L);
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(e));

        assertThatThrownBy(() -> expenseService.bulkDelete(List.of(1L)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void bulkDelete_emptyList_returnsZero() {
        assertThat(expenseService.bulkDelete(List.of())).isEqualTo(0);
    }

    // ── bulkUpdate ────────────────────────────────────────────────────────────

    @Test
    void bulkUpdate_appliesCategoryAndPaymentMethod() {
        Expense e = buildExpense(1L, USER_ID);
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(e));
        when(expenseRepository.save(any())).thenReturn(e);

        int count = expenseService.bulkUpdate(List.of(1L), "Travel", "Card");

        assertThat(count).isEqualTo(1);
        assertThat(e.getCategory()).isEqualTo("Travel");
        assertThat(e.getPaymentMethod()).isEqualTo("Card");
    }

    @Test
    void bulkUpdate_blankValues_keepsOriginal() {
        Expense e = buildExpense(1L, USER_ID);
        e.setCategory("Food");
        e.setPaymentMethod("Cash");
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(e));
        when(expenseRepository.save(any())).thenReturn(e);

        expenseService.bulkUpdate(List.of(1L), "  ", "  ");

        assertThat(e.getCategory()).isEqualTo("Food");
        assertThat(e.getPaymentMethod()).isEqualTo("Cash");
    }

    @Test
    void bulkUpdate_nullValues_keepsOriginal() {
        Expense e = buildExpense(1L, USER_ID);
        e.setCategory("Food");
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(e));
        when(expenseRepository.save(any())).thenReturn(e);

        expenseService.bulkUpdate(List.of(1L), null, null);

        assertThat(e.getCategory()).isEqualTo("Food");
    }

    @Test
    void bulkUpdate_skipsNotFound() {
        when(expenseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(expenseService.bulkUpdate(List.of(99L), "Travel", "Card")).isEqualTo(0);
    }

    @Test
    void bulkUpdate_differentOwner_throws() {
        Expense e = buildExpense(1L, 999L);
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(e));

        assertThatThrownBy(() -> expenseService.bulkUpdate(List.of(1L), "Travel", "Card"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
