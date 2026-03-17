package com.finance_tracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance_tracker.dto.InvestmentRequestDTO;
import com.finance_tracker.dto.InvestmentResponseDTO;
import com.finance_tracker.dto.InvestmentSummaryDTO;
import com.finance_tracker.dto.InvestmentTradeRequestDTO;
import com.finance_tracker.mapper.InvestmentMapper;
import com.finance_tracker.model.Investment;
import com.finance_tracker.model.InvestmentType;
import com.finance_tracker.service.AmfiNavService;
import com.finance_tracker.service.InvestmentService;
import com.finance_tracker.service.SipService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import com.finance_tracker.utils.security.JwtService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = InvestmentController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class, org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class})
class InvestmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private JwtService jwtService;


    @MockitoBean
    private InvestmentService investmentService;

    @MockitoBean
    private InvestmentMapper investmentMapper;

    @MockitoBean
    private SipService sipService;

    @MockitoBean
    private AmfiNavService amfiNavService;

    private Investment investment(Long id) {
        Investment inv = new Investment();
        inv.setId(id);
        inv.setName("Reliance");
        inv.setSymbol("RELIANCE.NS");
        inv.setType(InvestmentType.STOCK);
        inv.setQuantity(new BigDecimal("10"));
        inv.setPurchasePrice(new BigDecimal("2000"));
        inv.setCurrentPrice(new BigDecimal("2500"));
        inv.setPurchaseDate(LocalDate.now());
        return inv;
    }

    private InvestmentResponseDTO responseDTO(Long id) {
        return InvestmentResponseDTO.builder()
                .id(id).name("Reliance").symbol("RELIANCE.NS")
                .type(InvestmentType.STOCK).quantity(new BigDecimal("10"))
                .purchasePrice(new BigDecimal("2000")).currentPrice(new BigDecimal("2500"))
                .build();
    }

    @Test
    void getAllInvestments_returnsList() throws Exception {
        when(investmentService.getAllInvestments()).thenReturn(List.of(investment(1L)));
        when(investmentMapper.toDTOList(anyList())).thenReturn(List.of(responseDTO(1L)));

        mockMvc.perform(get("/api/investments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getInvestmentById_returnsInvestment() throws Exception {
        when(investmentService.getInvestmentById(1L)).thenReturn(investment(1L));
        when(investmentMapper.toDTO(any())).thenReturn(responseDTO(1L));

        mockMvc.perform(get("/api/investments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("RELIANCE.NS"));
    }

    @Test
    void createInvestment_validRequest_returnsInvestment() throws Exception {
        InvestmentRequestDTO req = new InvestmentRequestDTO();
        req.setName("TCS");
        req.setSymbol("TCS.NS");
        req.setType(InvestmentType.STOCK);
        req.setQuantity(new BigDecimal("5"));
        req.setPurchasePrice(new BigDecimal("3500"));

        when(investmentMapper.toEntity(any())).thenReturn(investment(null));
        when(investmentService.saveInvestment(any())).thenReturn(investment(2L));
        when(investmentMapper.toDTO(any())).thenReturn(responseDTO(2L));

        mockMvc.perform(post("/api/investments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void deleteInvestment_returns200() throws Exception {
        mockMvc.perform(delete("/api/investments/1"))
                .andExpect(status().isOk());
        verify(investmentService).deleteInvestment(1L);
    }

    @Test
    void getInvestmentSummary_returnsDto() throws Exception {
        when(sipService.getLinkedInvestmentIds()).thenReturn(List.of());
        when(investmentService.getTotalInvestmentValueExcluding(anyList())).thenReturn(new BigDecimal("100000"));
        when(investmentService.getTotalProfitLossExcluding(anyList())).thenReturn(new BigDecimal("15000"));

        mockMvc.perform(get("/api/investments/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalValue").value(100000));
    }

    @Test
    void refreshPrices_returns202() throws Exception {
        mockMvc.perform(post("/api/investments/refresh-prices"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("refresh_started"));
    }

    @Test
    void searchMf_delegatesToAmfiNavService() throws Exception {
        when(amfiNavService.searchByName("HDFC")).thenReturn(
                List.of(Map.of("schemeCode", "118989", "name", "HDFC Flexi Cap")));

        mockMvc.perform(get("/api/investments/search-mf").param("q", "HDFC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].schemeCode").value("118989"));
    }

    @Test
    void addUnits_returnsUpdatedInvestment() throws Exception {
        InvestmentTradeRequestDTO req = new InvestmentTradeRequestDTO();
        req.setQuantity(new BigDecimal("5"));
        req.setPrice(new BigDecimal("2600"));

        when(investmentService.addUnits(eq(1L), any(), any())).thenReturn(investment(1L));
        when(investmentMapper.toDTO(any())).thenReturn(responseDTO(1L));

        mockMvc.perform(post("/api/investments/1/add-units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void sellUnits_whenInvestmentPresent_returns200() throws Exception {
        InvestmentTradeRequestDTO req = new InvestmentTradeRequestDTO();
        req.setQuantity(new BigDecimal("2"));
        req.setPrice(new BigDecimal("2700"));

        when(investmentService.sellUnits(eq(1L), any(), any())).thenReturn(Optional.of(investment(1L)));
        when(investmentMapper.toDTO(any())).thenReturn(responseDTO(1L));

        mockMvc.perform(post("/api/investments/1/sell-units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void sellUnits_whenInvestmentGone_returns204() throws Exception {
        InvestmentTradeRequestDTO req = new InvestmentTradeRequestDTO();
        req.setQuantity(new BigDecimal("10"));
        req.setPrice(new BigDecimal("2700"));

        when(investmentService.sellUnits(eq(1L), any(), any())).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/investments/1/sell-units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());
    }
}
