package com.finance_tracker.service;

import com.finance_tracker.exception.BusinessLogicException;
import com.finance_tracker.exception.ResourceNotFoundException;
import com.finance_tracker.model.Investment;
import com.finance_tracker.model.InvestmentType;
import com.finance_tracker.repository.InvestmentRepository;
import com.finance_tracker.utils.strategy.PriceProviderService;
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
class InvestmentServiceTest {

    @Mock
    private InvestmentRepository investmentRepository;

    @Mock
    private PriceProviderService priceProviderService;

    @Mock
    private AmfiNavService amfiNavService;

    @Mock
    private LedgerService ledgerService;

    @InjectMocks
    private InvestmentService investmentService;

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

    private Investment buildInvestment(Long id, Long userId, String symbol) {
        Investment inv = new Investment();
        inv.setId(id);
        inv.setUserId(userId);
        inv.setName("Reliance");
        inv.setSymbol(symbol);
        inv.setType(InvestmentType.STOCK);
        inv.setQuantity(new BigDecimal("10.000000"));
        inv.setPurchasePrice(new BigDecimal("2500.000000"));
        inv.setCurrentPrice(new BigDecimal("2800.000000"));
        inv.setPurchaseDate(LocalDate.of(2023, 1, 1));
        inv.setLastUpdated(LocalDate.of(2024, 1, 1));
        return inv;
    }

    // ── getAllInvestments ─────────────────────────────────────────────────────

    @Test
    void getAllInvestments_returnsUserInvestments() {
        when(investmentRepository.findByUserId(USER_ID))
                .thenReturn(List.of(buildInvestment(1L, USER_ID, "RELIANCE.NS")));

        assertThat(investmentService.getAllInvestments()).hasSize(1);
    }

    // ── getInvestmentById ─────────────────────────────────────────────────────

    @Test
    void getInvestmentById_found_returnsInvestment() {
        Investment inv = buildInvestment(1L, USER_ID, "RELIANCE.NS");
        when(investmentRepository.findById(1L)).thenReturn(Optional.of(inv));

        assertThat(investmentService.getInvestmentById(1L)).isEqualTo(inv);
    }

    @Test
    void getInvestmentById_notFound_throws() {
        when(investmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> investmentService.getInvestmentById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getInvestmentById_differentOwner_throws() {
        Investment inv = buildInvestment(1L, 999L, "RELIANCE.NS");
        when(investmentRepository.findById(1L)).thenReturn(Optional.of(inv));

        assertThatThrownBy(() -> investmentService.getInvestmentById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Investment not found");
    }

    // ── saveInvestment – symbol normalisation ─────────────────────────────────

    @Test
    void saveInvestment_plainSymbol_appendsNSSuffix() {
        Investment inv = buildInvestment(null, null, "RELIANCE");
        Investment saved = buildInvestment(1L, USER_ID, "RELIANCE.NS");
        when(investmentRepository.save(any())).thenReturn(saved);

        investmentService.saveInvestment(inv);

        assertThat(inv.getSymbol()).isEqualTo("RELIANCE.NS");
    }

    @Test
    void saveInvestment_symbolAlreadyHasNSSuffix_notDoubled() {
        Investment inv = buildInvestment(null, null, "INFY.NS");
        Investment saved = buildInvestment(1L, USER_ID, "INFY.NS");
        when(investmentRepository.save(any())).thenReturn(saved);

        investmentService.saveInvestment(inv);

        assertThat(inv.getSymbol()).isEqualTo("INFY.NS");
    }

    @Test
    void saveInvestment_symbolWithBOSuffix_keptAsIs() {
        Investment inv = buildInvestment(null, null, "RELIANCE.BO");
        when(investmentRepository.save(any())).thenReturn(inv);

        investmentService.saveInvestment(inv);

        assertThat(inv.getSymbol()).isEqualTo("RELIANCE.BO");
    }

    @Test
    void saveInvestment_numericSymbol_notSuffixed() {
        Investment inv = buildInvestment(null, null, "119060");
        when(investmentRepository.save(any())).thenReturn(inv);

        investmentService.saveInvestment(inv);

        assertThat(inv.getSymbol()).isEqualTo("119060");
    }

    @Test
    void saveInvestment_isinSymbol_notSuffixed() {
        Investment inv = buildInvestment(null, null, "INE002A01018");
        when(investmentRepository.save(any())).thenReturn(inv);

        investmentService.saveInvestment(inv);

        assertThat(inv.getSymbol()).isEqualTo("INE002A01018");
    }

    @Test
    void saveInvestment_nullSymbol_remainsNull() {
        Investment inv = buildInvestment(null, null, null);
        when(investmentRepository.save(any())).thenReturn(inv);

        investmentService.saveInvestment(inv);

        assertThat(inv.getSymbol()).isNull();
    }

    @Test
    void saveInvestment_create_setsUserIdAndDate() {
        Investment inv = buildInvestment(null, null, "TCS.NS");
        inv.setLastUpdated(null);
        when(investmentRepository.save(any())).thenReturn(inv);

        investmentService.saveInvestment(inv);

        assertThat(inv.getUserId()).isEqualTo(USER_ID);
        assertThat(inv.getLastUpdated()).isNotNull();
        verify(ledgerService).recordEvent(eq("INVESTMENT"), any(), eq("CREATE"), isNull(), any(), any());
    }

    @Test
    void saveInvestment_update_recordsUpdateEvent() {
        Investment before = buildInvestment(1L, USER_ID, "TCS.NS");
        Investment updated = buildInvestment(1L, USER_ID, "TCS.NS");
        updated.setName("TCS");

        when(investmentRepository.findById(1L)).thenReturn(Optional.of(before));
        when(investmentRepository.save(any())).thenReturn(updated);

        investmentService.saveInvestment(updated);

        verify(ledgerService).recordEvent(eq("INVESTMENT"), any(), eq("UPDATE"), eq(before), eq(updated), any());
    }

    @Test
    void saveInvestment_update_differentOwner_throws() {
        Investment owned = buildInvestment(1L, 999L, "TCS.NS");
        Investment updated = buildInvestment(1L, USER_ID, "TCS.NS");

        when(investmentRepository.findById(1L)).thenReturn(Optional.of(owned));

        assertThatThrownBy(() -> investmentService.saveInvestment(updated))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── deleteInvestment ──────────────────────────────────────────────────────

    @Test
    void deleteInvestment_success_deletesAndRecords() {
        Investment inv = buildInvestment(1L, USER_ID, "RELIANCE.NS");
        when(investmentRepository.findById(1L)).thenReturn(Optional.of(inv));

        investmentService.deleteInvestment(1L);

        verify(investmentRepository).deleteById(1L);
        verify(ledgerService).recordEvent(eq("INVESTMENT"), eq("1"), eq("DELETE"), eq(inv), isNull(), any());
    }

    @Test
    void deleteInvestment_notFound_throws() {
        when(investmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> investmentService.deleteInvestment(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteInvestment_differentOwner_throws() {
        Investment inv = buildInvestment(1L, 999L, "RELIANCE.NS");
        when(investmentRepository.findById(1L)).thenReturn(Optional.of(inv));

        assertThatThrownBy(() -> investmentService.deleteInvestment(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── aggregate queries ─────────────────────────────────────────────────────

    @Test
    void getTotalInvestmentValue_sumsCurrentValues() {
        Investment i1 = buildInvestment(1L, USER_ID, "A.NS");
        Investment i2 = buildInvestment(2L, USER_ID, "B.NS");
        i1.setQuantity(new BigDecimal("10"));
        i1.setCurrentPrice(new BigDecimal("100"));
        i2.setQuantity(new BigDecimal("5"));
        i2.setCurrentPrice(new BigDecimal("200"));
        when(investmentRepository.findByUserId(USER_ID)).thenReturn(List.of(i1, i2));

        assertThat(investmentService.getTotalInvestmentValue()).isEqualByComparingTo("2000.00");
    }

    @Test
    void getTotalProfitLoss_sumsProfitLoss() {
        Investment inv = buildInvestment(1L, USER_ID, "RELIANCE.NS");
        inv.setQuantity(new BigDecimal("10"));
        inv.setPurchasePrice(new BigDecimal("100"));
        inv.setCurrentPrice(new BigDecimal("150"));
        when(investmentRepository.findByUserId(USER_ID)).thenReturn(List.of(inv));

        assertThat(investmentService.getTotalProfitLoss()).isEqualByComparingTo("500.00");
    }

    @Test
    void getTotalInvestmentValueExcluding_excludesMatchingIds() {
        Investment i1 = buildInvestment(1L, USER_ID, "A.NS");
        i1.setQuantity(new BigDecimal("10"));
        i1.setCurrentPrice(new BigDecimal("100"));
        Investment i2 = buildInvestment(2L, USER_ID, "B.NS");
        i2.setQuantity(new BigDecimal("5"));
        i2.setCurrentPrice(new BigDecimal("200"));
        when(investmentRepository.findByUserId(USER_ID)).thenReturn(List.of(i1, i2));

        BigDecimal result = investmentService.getTotalInvestmentValueExcluding(List.of(2L));

        assertThat(result).isEqualByComparingTo("1000.00");
    }

    @Test
    void getTotalProfitLossExcluding_excludesMatchingIds() {
        Investment i1 = buildInvestment(1L, USER_ID, "A.NS");
        i1.setQuantity(new BigDecimal("10"));
        i1.setPurchasePrice(new BigDecimal("100"));
        i1.setCurrentPrice(new BigDecimal("110"));
        Investment i2 = buildInvestment(2L, USER_ID, "B.NS");
        i2.setQuantity(new BigDecimal("5"));
        i2.setPurchasePrice(new BigDecimal("200"));
        i2.setCurrentPrice(new BigDecimal("150"));
        when(investmentRepository.findByUserId(USER_ID)).thenReturn(List.of(i1, i2));

        BigDecimal result = investmentService.getTotalProfitLossExcluding(List.of(2L));

        assertThat(result).isEqualByComparingTo("100.00");
    }

    // ── addUnits ──────────────────────────────────────────────────────────────

    @Test
    void addUnits_updatesQuantityAndAveragePrice() {
        Investment inv = buildInvestment(1L, USER_ID, "RELIANCE.NS");
        inv.setQuantity(new BigDecimal("10.000000"));
        inv.setPurchasePrice(new BigDecimal("100.000000"));
        when(investmentRepository.findById(1L)).thenReturn(Optional.of(inv));
        when(investmentRepository.save(any())).thenReturn(inv);

        Investment result = investmentService.addUnits(1L, new BigDecimal("10"), new BigDecimal("120"));

        assertThat(inv.getQuantity()).isEqualByComparingTo("20.000000");
        assertThat(inv.getPurchasePrice()).isEqualByComparingTo("110.000000");
        verify(ledgerService).recordEvent(eq("INVESTMENT"), any(), eq("ADD_UNITS"), any(), any(), any());
    }

    @Test
    void addUnits_notFound_throws() {
        when(investmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> investmentService.addUnits(99L, BigDecimal.ONE, BigDecimal.ONE))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addUnits_differentOwner_throws() {
        Investment inv = buildInvestment(1L, 999L, "RELIANCE.NS");
        when(investmentRepository.findById(1L)).thenReturn(Optional.of(inv));

        assertThatThrownBy(() -> investmentService.addUnits(1L, BigDecimal.ONE, BigDecimal.ONE))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── sellUnits ─────────────────────────────────────────────────────────────

    @Test
    void sellUnits_partial_reducesQuantity() {
        Investment inv = buildInvestment(1L, USER_ID, "RELIANCE.NS");
        inv.setQuantity(new BigDecimal("10.000000"));
        when(investmentRepository.findById(1L)).thenReturn(Optional.of(inv));
        when(investmentRepository.save(any())).thenReturn(inv);

        Optional<Investment> result = investmentService.sellUnits(1L, new BigDecimal("3"), new BigDecimal("2800"));

        assertThat(result).isPresent();
        assertThat(inv.getQuantity()).isEqualByComparingTo("7.000000");
        verify(ledgerService).recordEvent(eq("INVESTMENT"), any(), eq("SELL_UNITS"), any(), any(), any());
    }

    @Test
    void sellUnits_all_deletesAndReturnsEmpty() {
        Investment inv = buildInvestment(1L, USER_ID, "RELIANCE.NS");
        inv.setQuantity(new BigDecimal("5.000000"));
        when(investmentRepository.findById(1L)).thenReturn(Optional.of(inv));

        Optional<Investment> result = investmentService.sellUnits(1L, new BigDecimal("5"), new BigDecimal("2800"));

        assertThat(result).isEmpty();
        verify(investmentRepository).deleteById(1L);
        verify(ledgerService).recordEvent(eq("INVESTMENT"), any(), eq("SELL_ALL"), any(), isNull(), any());
    }

    @Test
    void sellUnits_moreThansHeld_deletesAndReturnsEmpty() {
        Investment inv = buildInvestment(1L, USER_ID, "RELIANCE.NS");
        inv.setQuantity(new BigDecimal("5.000000"));
        when(investmentRepository.findById(1L)).thenReturn(Optional.of(inv));

        Optional<Investment> result = investmentService.sellUnits(1L, new BigDecimal("10"), new BigDecimal("2800"));

        assertThat(result).isEmpty();
        verify(investmentRepository).deleteById(1L);
    }

    @Test
    void sellUnits_zeroQty_throws() {
        Investment inv = buildInvestment(1L, USER_ID, "RELIANCE.NS");
        when(investmentRepository.findById(1L)).thenReturn(Optional.of(inv));

        assertThatThrownBy(() -> investmentService.sellUnits(1L, BigDecimal.ZERO, new BigDecimal("2800")))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("greater than 0");
    }

    @Test
    void sellUnits_negativeQty_throws() {
        Investment inv = buildInvestment(1L, USER_ID, "RELIANCE.NS");
        when(investmentRepository.findById(1L)).thenReturn(Optional.of(inv));

        assertThatThrownBy(() -> investmentService.sellUnits(1L, new BigDecimal("-1"), new BigDecimal("2800")))
                .isInstanceOf(BusinessLogicException.class);
    }

    @Test
    void sellUnits_notFound_throws() {
        when(investmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> investmentService.sellUnits(99L, BigDecimal.ONE, BigDecimal.TEN))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void sellUnits_differentOwner_throws() {
        Investment inv = buildInvestment(1L, 999L, "RELIANCE.NS");
        when(investmentRepository.findById(1L)).thenReturn(Optional.of(inv));

        assertThatThrownBy(() -> investmentService.sellUnits(1L, BigDecimal.ONE, BigDecimal.TEN))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── bulkDelete ────────────────────────────────────────────────────────────

    @Test
    void bulkDelete_skipsNotFound_countsDeleted() {
        Investment inv = buildInvestment(2L, USER_ID, "TCS.NS");
        when(investmentRepository.findById(1L)).thenReturn(Optional.empty());
        when(investmentRepository.findById(2L)).thenReturn(Optional.of(inv));

        int count = investmentService.bulkDelete(List.of(1L, 2L));

        assertThat(count).isEqualTo(1);
        verify(investmentRepository, never()).deleteById(1L);
        verify(investmentRepository).deleteById(2L);
    }

    @Test
    void bulkDelete_differentOwner_throws() {
        Investment inv = buildInvestment(1L, 999L, "TCS.NS");
        when(investmentRepository.findById(1L)).thenReturn(Optional.of(inv));

        assertThatThrownBy(() -> investmentService.bulkDelete(List.of(1L)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void bulkDelete_emptyList_returnsZero() {
        assertThat(investmentService.bulkDelete(List.of())).isEqualTo(0);
    }

    // ── updateCurrentPrices ───────────────────────────────────────────────────

    @Test
    void updateCurrentPrices_stock_fetchesPriceAndSaves() {
        Investment inv = buildInvestment(1L, USER_ID, "RELIANCE.NS");
        inv.setType(InvestmentType.STOCK);
        when(investmentRepository.findAll()).thenReturn(List.of(inv));
        when(priceProviderService.fetchPrice("RELIANCE.NS", InvestmentType.STOCK))
                .thenReturn(new BigDecimal("3000.00"));
        when(investmentRepository.save(any())).thenReturn(inv);

        investmentService.updateCurrentPrices();

        assertThat(inv.getCurrentPrice()).isEqualByComparingTo("3000.000000");
        verify(investmentRepository).save(inv);
    }

    @Test
    void updateCurrentPrices_priceIsZero_skipsInvestment() {
        Investment inv = buildInvestment(1L, USER_ID, "RELIANCE.NS");
        inv.setType(InvestmentType.STOCK);
        when(investmentRepository.findAll()).thenReturn(List.of(inv));
        when(priceProviderService.fetchPrice(anyString(), any())).thenReturn(BigDecimal.ZERO);

        investmentService.updateCurrentPrices();

        verify(investmentRepository, never()).save(any());
    }

    @Test
    void updateCurrentPrices_mutualFund_noSymbol_skips() {
        Investment inv = buildInvestment(1L, USER_ID, null);
        inv.setType(InvestmentType.MUTUAL_FUND);
        inv.setSymbol(null);
        when(investmentRepository.findAll()).thenReturn(List.of(inv));

        investmentService.updateCurrentPrices();

        verify(investmentRepository, never()).save(any());
    }

    @Test
    void updateCurrentPrices_mutualFund_navFound_saves() {
        Investment inv = buildInvestment(1L, USER_ID, "119060");
        inv.setType(InvestmentType.MUTUAL_FUND);
        when(investmentRepository.findAll()).thenReturn(List.of(inv));
        when(amfiNavService.getNavBySchemeCode("119060")).thenReturn(Optional.of(new BigDecimal("50.00")));
        when(investmentRepository.save(any())).thenReturn(inv);

        investmentService.updateCurrentPrices();

        verify(investmentRepository).save(inv);
    }
}
