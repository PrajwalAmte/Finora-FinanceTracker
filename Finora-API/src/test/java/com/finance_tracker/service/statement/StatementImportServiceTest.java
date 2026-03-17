package com.finance_tracker.service.statement;

import com.finance_tracker.dto.statement.ParsedHolding;
import com.finance_tracker.dto.statement.ParsedMFHolding;
import com.finance_tracker.dto.statement.StatementConfirmRequest;
import com.finance_tracker.dto.statement.StatementImportResultDTO;
import com.finance_tracker.model.Investment;
import com.finance_tracker.model.InvestmentType;
import com.finance_tracker.repository.InvestmentRepository;
import com.finance_tracker.service.AmfiNavService;
import com.finance_tracker.service.InvestmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StatementImportServiceTest {

    @Mock private CasStatementParser  casParser;
    @Mock private CamsStatementParser camsParser;
    @Mock private HoldingsExcelParser excelParser;
    @Mock private InvestmentRepository investmentRepository;
    @Mock private InvestmentService    investmentService;
    @Mock private AmfiNavService       amfiNavService;

    @InjectMocks
    private StatementImportService statementImportService;

    private static final Long USER_ID = 42L;

    // ─── helpers ──────────────────────────────────────────────────────────────

    private StatementConfirmRequest baseRequest() {
        StatementConfirmRequest req = new StatementConfirmRequest();
        req.setSelectedIsins(List.of());
        req.setStatementType("ZERODHA_EXCEL");
        req.setHoldings(List.of());
        req.setMfHoldings(List.of());
        return req;
    }

    private ParsedHolding equityHolding(String isin) {
        return ParsedHolding.builder()
                .isin(isin)
                .name("Test Corp")
                .symbol("TSTCORP")
                .quantity(new BigDecimal("10"))
                .avgCost(new BigDecimal("500"))
                .ltp(new BigDecimal("550"))
                .detectedType(InvestmentType.STOCK)
                .build();
    }

    private Investment savedInvestment(String importSource) {
        Investment inv = new Investment();
        inv.setId(99L);
        inv.setName("Test Corp");
        inv.setSymbol("TSTCORP.NS");
        inv.setType(InvestmentType.STOCK);
        inv.setQuantity(new BigDecimal("10"));
        inv.setPurchasePrice(new BigDecimal("500"));
        inv.setCurrentPrice(new BigDecimal("500"));
        inv.setPurchaseDate(LocalDate.now());
        inv.setLastUpdated(LocalDate.now());
        inv.setUserId(USER_ID);
        inv.setImportSource(importSource);
        return inv;
    }

    // ─── confirmImport ────────────────────────────────────────────────────────

    @Test
    void confirmImport_emptyRequest_returnsZeroCounts() {
        StatementImportResultDTO result = statementImportService.confirmImport(baseRequest(), USER_ID);

        assertThat(result.getImported()).isZero();
        assertThat(result.getUpdated()).isZero();
        assertThat(result.getSkipped()).isZero();
    }

    @Test
    void confirmImport_holdingNotSelected_skipped() {
        StatementConfirmRequest req = baseRequest();
        req.setSelectedIsins(List.of("INE999Z99999"));
        req.setHoldings(List.of(equityHolding("INE001A01036")));

        StatementImportResultDTO result = statementImportService.confirmImport(req, USER_ID);

        assertThat(result.getImported()).isZero();
        verifyNoInteractions(investmentRepository);
    }

    @Test
    void confirmImport_newHolding_imported() {
        String isin = "INE001A01036";
        StatementConfirmRequest req = baseRequest();
        req.setSelectedIsins(List.of(isin));
        req.setHoldings(List.of(equityHolding(isin)));

        when(investmentRepository.findByUserIdAndIsin(USER_ID, isin))
                .thenReturn(Optional.empty());

        StatementImportResultDTO result = statementImportService.confirmImport(req, USER_ID);

        assertThat(result.getImported()).isEqualTo(1);
        assertThat(result.getUpdated()).isZero();
        assertThat(result.getSkipped()).isZero();
        verify(investmentService).saveInvestment(any());
    }

    @Test
    void confirmImport_existingHoldingFromStatement_updated() {
        String isin = "INE001A01036";
        StatementConfirmRequest req = baseRequest();
        req.setSelectedIsins(List.of(isin));
        req.setHoldings(List.of(equityHolding(isin)));

        Investment existing = savedInvestment("NSDL");
        when(investmentRepository.findByUserIdAndIsin(USER_ID, isin))
                .thenReturn(Optional.of(existing));

        StatementImportResultDTO result = statementImportService.confirmImport(req, USER_ID);

        assertThat(result.getUpdated()).isEqualTo(1);
        assertThat(result.getImported()).isZero();
        assertThat(result.getSkipped()).isZero();
        verify(investmentService).saveInvestment(existing);
    }

    @Test
    void confirmImport_existingManualEntry_skipped() {
        String isin = "INE001A01036";
        StatementConfirmRequest req = baseRequest();
        req.setSelectedIsins(List.of(isin));
        req.setHoldings(List.of(equityHolding(isin)));

        Investment manual = savedInvestment(null);
        when(investmentRepository.findByUserIdAndIsin(USER_ID, isin))
                .thenReturn(Optional.of(manual));

        StatementImportResultDTO result = statementImportService.confirmImport(req, USER_ID);

        assertThat(result.getSkipped()).isEqualTo(1);
        assertThat(result.getImported()).isZero();
        assertThat(result.getUpdated()).isZero();
        verifyNoInteractions(investmentService);
    }

    @Test
    void confirmImport_mfHolding_newImport() {
        String isin = "INF123456789";
        ParsedMFHolding mf = ParsedMFHolding.builder()
                .isin(isin)
                .schemeName("Nifty 50 Index Fund")
                .schemeCode(null)
                .units(new BigDecimal("500"))
                .avgCost(new BigDecimal("40"))
                .nav(new BigDecimal("44"))
                .build();

        StatementConfirmRequest req = baseRequest();
        req.setSelectedIsins(List.of(isin));
        req.setMfHoldings(List.of(mf));

        when(investmentRepository.findByUserIdAndIsin(USER_ID, isin)).thenReturn(Optional.empty());
        when(amfiNavService.lookupSchemeCodeByIsin(isin)).thenReturn(Optional.of("119598"));

        StatementImportResultDTO result = statementImportService.confirmImport(req, USER_ID);

        assertThat(result.getImported()).isGreaterThanOrEqualTo(1);
        assertThat(result.getSkipped()).isZero();
        verify(investmentService).saveInvestment(any());
    }

    @Test
    void confirmImport_mfHoldingNoSchemeCode_warningAdded() {
        String schemeName = "Some Unlisted Debt Fund";
        ParsedMFHolding mf = ParsedMFHolding.builder()
                .isin(null)
                .schemeName(schemeName)
                .schemeCode(null)
                .units(new BigDecimal("300"))
                .avgCost(new BigDecimal("25"))
                .nav(new BigDecimal("27"))
                .build();

        StatementConfirmRequest req = baseRequest();
        req.setSelectedIsins(List.of(schemeName));
        req.setMfHoldings(List.of(mf));

        when(investmentRepository.findFirstByUserIdAndSymbol(USER_ID, schemeName))
                .thenReturn(Optional.empty());

        StatementImportResultDTO result = statementImportService.confirmImport(req, USER_ID);

        assertThat(result.getImported()).isGreaterThanOrEqualTo(1);
        assertThat(result.getWarnings()).isNotEmpty();
        assertThat(result.getWarnings().get(0)).contains(schemeName);
    }
}
