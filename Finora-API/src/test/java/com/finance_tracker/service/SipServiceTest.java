package com.finance_tracker.service;

import com.finance_tracker.exception.ResourceNotFoundException;
import com.finance_tracker.model.Investment;
import com.finance_tracker.model.Sip;
import com.finance_tracker.repository.InvestmentRepository;
import com.finance_tracker.repository.SipRepository;
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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SipServiceTest {

    @Mock
    private SipRepository sipRepository;

    @Mock
    private InvestmentRepository investmentRepository;

    @Mock
    private LedgerService ledgerService;

    @Mock
    private AmfiNavService amfiNavService;

    @InjectMocks
    private SipService sipService;

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

    private Sip buildSip(Long id, Long userId) {
        Sip s = new Sip();
        s.setId(id);
        s.setUserId(userId);
        s.setName("HDFC Mid Cap");
        s.setSchemeCode("118989");
        s.setMonthlyAmount(new BigDecimal("5000.00"));
        s.setStartDate(LocalDate.of(2023, 1, 1));
        s.setDurationMonths(36);
        s.setCurrentNav(new BigDecimal("80.00"));
        s.setTotalUnits(new BigDecimal("100.00000000"));
        s.setTotalInvested(new BigDecimal("8000.00"));
        s.setLastUpdated(LocalDate.of(2024, 1, 1));
        return s;
    }

    private Investment buildInvestment(Long id, BigDecimal currentValue) {
        Investment inv = new Investment();
        inv.setId(id);
        inv.setQuantity(new BigDecimal("10"));
        inv.setCurrentPrice(currentValue.divide(new BigDecimal("10")));
        inv.setPurchasePrice(new BigDecimal("50.00"));
        return inv;
    }

    // ── getAllSips ────────────────────────────────────────────────────────────

    @Test
    void getAllSips_returnsUserSips() {
        when(sipRepository.findByUserId(USER_ID)).thenReturn(List.of(buildSip(1L, USER_ID)));

        assertThat(sipService.getAllSips()).hasSize(1);
    }

    // ── getSipById ────────────────────────────────────────────────────────────

    @Test
    void getSipById_found_returnsSip() {
        Sip s = buildSip(1L, USER_ID);
        when(sipRepository.findById(1L)).thenReturn(Optional.of(s));

        assertThat(sipService.getSipById(1L)).isEqualTo(s);
    }

    @Test
    void getSipById_notFound_throws() {
        when(sipRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sipService.getSipById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getSipById_differentOwner_throws() {
        Sip s = buildSip(1L, 999L);
        when(sipRepository.findById(1L)).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> sipService.getSipById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("SIP not found");
    }

    // ── saveSip (create) ──────────────────────────────────────────────────────

    @Test
    void saveSip_create_setsDefaultsAndRecords() {
        Sip s = new Sip();
        s.setName("Test SIP");
        s.setMonthlyAmount(new BigDecimal("3000.00"));

        Sip saved = buildSip(1L, USER_ID);
        when(sipRepository.save(any())).thenReturn(saved);

        sipService.saveSip(s);

        assertThat(s.getUserId()).isEqualTo(USER_ID);
        assertThat(s.getLastUpdated()).isNotNull();
        assertThat(s.getTotalUnits()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(s.getCurrentNav()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(ledgerService).recordEvent(eq("SIP"), any(), eq("CREATE"), isNull(), any(), any());
    }

    @Test
    void saveSip_create_preservesExistingNavAndUnits() {
        Sip s = buildSip(null, null);
        s.setId(null);
        s.setCurrentNav(new BigDecimal("50.00"));
        s.setTotalUnits(new BigDecimal("20.00"));

        when(sipRepository.save(any())).thenReturn(s);

        sipService.saveSip(s);

        assertThat(s.getCurrentNav()).isEqualByComparingTo("50.00");
        assertThat(s.getTotalUnits()).isEqualByComparingTo("20.00");
    }

    // ── saveSip (update) ──────────────────────────────────────────────────────

    @Test
    void saveSip_update_recordsUpdateEvent() {
        Sip before = buildSip(1L, USER_ID);
        Sip updated = buildSip(1L, USER_ID);
        updated.setName("Updated SIP");

        when(sipRepository.findById(1L)).thenReturn(Optional.of(before));
        when(sipRepository.save(any())).thenReturn(updated);

        sipService.saveSip(updated);

        verify(ledgerService).recordEvent(eq("SIP"), any(), eq("UPDATE"), eq(before), eq(updated), any());
    }

    @Test
    void saveSip_update_syncsLinkedInvestmentName() {
        Sip updated = buildSip(1L, USER_ID);
        updated.setName("New Fund Name");
        updated.setInvestmentId(10L);

        Sip before = buildSip(1L, USER_ID);
        Investment linked = buildInvestment(10L, new BigDecimal("1000.00"));
        when(sipRepository.findById(1L)).thenReturn(Optional.of(before));
        when(sipRepository.save(any())).thenReturn(updated);
        when(investmentRepository.findById(10L)).thenReturn(Optional.of(linked));
        when(investmentRepository.save(any())).thenReturn(linked);

        sipService.saveSip(updated);

        assertThat(linked.getName()).isEqualTo("New Fund Name");
    }

    @Test
    void saveSip_update_differentOwner_throws() {
        Sip owned = buildSip(1L, 999L);
        Sip updated = buildSip(1L, USER_ID);

        when(sipRepository.findById(1L)).thenReturn(Optional.of(owned));

        assertThatThrownBy(() -> sipService.saveSip(updated))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── deleteSip ─────────────────────────────────────────────────────────────

    @Test
    void deleteSip_success_deletesAndRecords() {
        Sip s = buildSip(1L, USER_ID);
        when(sipRepository.findById(1L)).thenReturn(Optional.of(s));

        sipService.deleteSip(1L);

        verify(sipRepository).deleteById(1L);
        verify(ledgerService).recordEvent(eq("SIP"), eq("1"), eq("DELETE"), eq(s), isNull(), any());
    }

    @Test
    void deleteSip_notFound_throws() {
        when(sipRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sipService.deleteSip(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteSip_differentOwner_throws() {
        Sip s = buildSip(1L, 999L);
        when(sipRepository.findById(1L)).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> sipService.deleteSip(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getTotalSipValue ──────────────────────────────────────────────────────

    @Test
    void getTotalSipValue_standaloneOnly_sumsCurrentValue() {
        when(sipRepository.sumStandaloneCurrentValueByUserId(USER_ID)).thenReturn(new BigDecimal("1000.00"));
        when(sipRepository.findLinkedInvestmentIdsByUserId(USER_ID)).thenReturn(List.of());

        assertThat(sipService.getTotalSipValue()).isEqualByComparingTo("1000.00");
    }

    @Test
    void getTotalSipValue_linkedOnly_usesInvestmentCurrentValue() {
        when(sipRepository.sumStandaloneCurrentValueByUserId(USER_ID)).thenReturn(BigDecimal.ZERO);
        when(sipRepository.findLinkedInvestmentIdsByUserId(USER_ID)).thenReturn(List.of(10L));
        when(investmentRepository.sumCurrentValueByIds(List.of(10L))).thenReturn(new BigDecimal("2000.00"));

        assertThat(sipService.getTotalSipValue()).isEqualByComparingTo("2000.00");
    }

    @Test
    void getTotalSipValue_mixed_addsBoth() {
        when(sipRepository.sumStandaloneCurrentValueByUserId(USER_ID)).thenReturn(new BigDecimal("500.00"));
        when(sipRepository.findLinkedInvestmentIdsByUserId(USER_ID)).thenReturn(List.of(10L));
        when(investmentRepository.sumCurrentValueByIds(List.of(10L))).thenReturn(new BigDecimal("3000.00"));

        assertThat(sipService.getTotalSipValue()).isEqualByComparingTo("3500.00");
    }

    // ── getTotalSipInvestment ─────────────────────────────────────────────────

    @Test
    void getTotalSipInvestment_standalone_usesTotalInvested() {
        when(sipRepository.sumStandaloneTotalInvestedByUserId(USER_ID)).thenReturn(new BigDecimal("12000.00"));
        when(sipRepository.findLinkedInvestmentIdsByUserId(USER_ID)).thenReturn(List.of());

        assertThat(sipService.getTotalSipInvestment()).isEqualByComparingTo("12000.00");
    }

    @Test
    void getTotalSipInvestment_standalone_nullTotalInvested_usesZero() {
        when(sipRepository.sumStandaloneTotalInvestedByUserId(USER_ID)).thenReturn(BigDecimal.ZERO);
        when(sipRepository.findLinkedInvestmentIdsByUserId(USER_ID)).thenReturn(List.of());

        assertThat(sipService.getTotalSipInvestment()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getTotalSipInvestment_linked_usesQtyTimesPrice() {
        when(sipRepository.sumStandaloneTotalInvestedByUserId(USER_ID)).thenReturn(BigDecimal.ZERO);
        when(sipRepository.findLinkedInvestmentIdsByUserId(USER_ID)).thenReturn(List.of(10L));
        when(investmentRepository.sumCostBasisByIds(List.of(10L))).thenReturn(new BigDecimal("1000.00"));

        assertThat(sipService.getTotalSipInvestment()).isEqualByComparingTo("1000.00");
    }

    // ── getLinkedInvestmentIds ────────────────────────────────────────────────

    @Test
    void getLinkedInvestmentIds_returnsLinkedIds() {
        when(sipRepository.findLinkedInvestmentIdsByUserId(USER_ID)).thenReturn(List.of(10L));

        assertThat(sipService.getLinkedInvestmentIds()).containsExactly(10L);
    }

    // ── recordPayment ─────────────────────────────────────────────────────────

    @Test
    void recordPayment_withPositiveNav_addsUnitsAndInvested() {
        Sip s = buildSip(1L, USER_ID);
        s.setCurrentNav(new BigDecimal("100.00"));
        s.setMonthlyAmount(new BigDecimal("5000.00"));
        s.setTotalUnits(new BigDecimal("10.00000000"));
        s.setTotalInvested(new BigDecimal("1000.00"));
        s.setStartDate(LocalDate.of(2024, 1, 1));

        when(sipRepository.findById(1L)).thenReturn(Optional.of(s));
        when(sipRepository.save(any())).thenReturn(s);

        Sip result = sipService.recordPayment(1L);

        assertThat(s.getTotalInvested()).isEqualByComparingTo("6000.00");
        assertThat(s.getTotalUnits()).isGreaterThan(new BigDecimal("10.00000000"));
        verify(ledgerService).recordEvent(eq("SIP"), eq("1"), eq("PAY"), isNull(), any(), any());
    }

    @Test
    void recordPayment_navZeroOrNull_skipsUnitAddition() {
        Sip s = buildSip(1L, USER_ID);
        s.setCurrentNav(BigDecimal.ZERO);
        s.setMonthlyAmount(new BigDecimal("5000.00"));
        s.setTotalUnits(new BigDecimal("10.00"));
        s.setTotalInvested(new BigDecimal("0.00"));

        when(sipRepository.findById(1L)).thenReturn(Optional.of(s));
        when(sipRepository.save(any())).thenReturn(s);

        sipService.recordPayment(1L);

        assertThat(s.getTotalUnits()).isEqualByComparingTo("10.00");
        assertThat(s.getTotalInvested()).isEqualByComparingTo("5000.00");
    }

    @Test
    void recordPayment_advancesStartDate() {
        Sip s = buildSip(1L, USER_ID);
        s.setCurrentNav(new BigDecimal("100.00"));
        s.setMonthlyAmount(new BigDecimal("5000.00"));
        s.setTotalUnits(BigDecimal.ZERO);
        s.setTotalInvested(BigDecimal.ZERO);
        s.setStartDate(LocalDate.of(2024, 1, 15));

        when(sipRepository.findById(1L)).thenReturn(Optional.of(s));
        when(sipRepository.save(any())).thenReturn(s);

        sipService.recordPayment(1L);

        assertThat(s.getStartDate()).isEqualTo(LocalDate.of(2024, 2, 15));
    }

    @Test
    void recordPayment_notFound_throws() {
        when(sipRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sipService.recordPayment(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void recordPayment_differentOwner_throws() {
        Sip s = buildSip(1L, 999L);
        when(sipRepository.findById(1L)).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> sipService.recordPayment(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── updateCurrentNavs ─────────────────────────────────────────────────────

    @Test
    void updateCurrentNavs_emptyNavData_doesNothing() {
        when(amfiNavService.getAllNavs()).thenReturn(Map.of());

        sipService.updateCurrentNavs();

        verify(sipRepository, never()).save(any());
    }

    @Test
    void updateCurrentNavs_nullNavData_doesNothing() {
        when(amfiNavService.getAllNavs()).thenReturn(null);

        sipService.updateCurrentNavs();

        verify(sipRepository, never()).save(any());
    }

    @Test
    void updateCurrentNavs_navFound_updatesAndSaves() {
        Sip s = buildSip(1L, USER_ID);
        s.setSchemeCode("118989");

        when(amfiNavService.getAllNavs()).thenReturn(Map.of("118989", new BigDecimal("90.00")));
        when(sipRepository.findAll()).thenReturn(List.of(s));
        when(sipRepository.save(any())).thenReturn(s);

        sipService.updateCurrentNavs();

        assertThat(s.getCurrentNav()).isEqualByComparingTo("90.00");
        verify(sipRepository).save(s);
    }

    @Test
    void updateCurrentNavs_navNotFound_skips() {
        Sip s = buildSip(1L, USER_ID);
        s.setSchemeCode("UNKNOWN");

        when(amfiNavService.getAllNavs()).thenReturn(Map.of("118989", new BigDecimal("90.00")));
        when(sipRepository.findAll()).thenReturn(List.of(s));

        sipService.updateCurrentNavs();

        verify(sipRepository, never()).save(any());
    }

    @Test
    void updateCurrentNavs_noSchemeCode_resolvesViaIsin() {
        Sip s = buildSip(1L, USER_ID);
        s.setSchemeCode(null);
        s.setIsin("INF179K01VQ8");

        when(amfiNavService.getAllNavs()).thenReturn(Map.of("118989", new BigDecimal("75.00")));
        when(sipRepository.findAll()).thenReturn(List.of(s));
        when(amfiNavService.lookupSchemeCodeByIsin("INF179K01VQ8")).thenReturn(Optional.of("118989"));
        when(sipRepository.save(any())).thenReturn(s);

        sipService.updateCurrentNavs();

        assertThat(s.getSchemeCode()).isEqualTo("118989");
        verify(sipRepository).save(s);
    }

    @Test
    void updateCurrentNavs_noSchemeCodeAndNoIsin_skips() {
        Sip s = buildSip(1L, USER_ID);
        s.setSchemeCode(null);
        s.setIsin(null);

        when(amfiNavService.getAllNavs()).thenReturn(Map.of("118989", new BigDecimal("75.00")));
        when(sipRepository.findAll()).thenReturn(List.of(s));

        sipService.updateCurrentNavs();

        verify(sipRepository, never()).save(any());
    }

    // ── bulkDelete ────────────────────────────────────────────────────────────

    @Test
    void bulkDelete_skipsNotFound_countsDeleted() {
        Sip s = buildSip(2L, USER_ID);
        when(sipRepository.findById(1L)).thenReturn(Optional.empty());
        when(sipRepository.findById(2L)).thenReturn(Optional.of(s));

        int count = sipService.bulkDelete(List.of(1L, 2L));

        assertThat(count).isEqualTo(1);
        verify(sipRepository, never()).deleteById(1L);
        verify(sipRepository).deleteById(2L);
    }

    @Test
    void bulkDelete_differentOwner_throws() {
        Sip s = buildSip(1L, 999L);
        when(sipRepository.findById(1L)).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> sipService.bulkDelete(List.of(1L)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void bulkDelete_emptyList_returnsZero() {
        assertThat(sipService.bulkDelete(List.of())).isEqualTo(0);
    }
}
