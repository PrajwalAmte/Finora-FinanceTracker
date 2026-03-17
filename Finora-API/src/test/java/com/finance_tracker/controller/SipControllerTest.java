package com.finance_tracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance_tracker.dto.SipRequestDTO;
import com.finance_tracker.dto.SipResponseDTO;
import com.finance_tracker.mapper.SipMapper;
import com.finance_tracker.model.Sip;
import com.finance_tracker.repository.InvestmentRepository;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SipController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class, org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class})
class SipControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private JwtService jwtService;


    @MockitoBean
    private SipService sipService;

    @MockitoBean
    private SipMapper sipMapper;

    @MockitoBean
    private InvestmentRepository investmentRepository;

    private Sip sip(Long id) {
        Sip s = new Sip();
        s.setId(id);
        s.setName("HDFC Flexi Cap");
        s.setMonthlyAmount(new BigDecimal("5000"));
        s.setStartDate(LocalDate.of(2023, 1, 1));
        s.setDurationMonths(120);
        s.setCurrentNav(new BigDecimal("100"));
        s.setTotalUnits(new BigDecimal("50"));
        s.setTotalInvested(new BigDecimal("50000"));
        return s;
    }

    private SipResponseDTO responseDTO(Long id) {
        return SipResponseDTO.builder()
                .id(id).name("HDFC Flexi Cap").monthlyAmount(new BigDecimal("5000"))
                .currentValue(new BigDecimal("5000")).totalInvested(new BigDecimal("50000")).build();
    }

    @Test
    void getAllSips_returnsList() throws Exception {
        when(sipService.getAllSips()).thenReturn(List.of(sip(1L)));
        when(investmentRepository.findAllById(anyList())).thenReturn(List.of());
        when(sipMapper.toDTOList(anyList(), anyMap())).thenReturn(List.of(responseDTO(1L)));

        mockMvc.perform(get("/api/sips"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getSipById_returnsSip() throws Exception {
        when(sipService.getSipById(1L)).thenReturn(sip(1L));
        when(sipMapper.toDTO(any(), any())).thenReturn(responseDTO(1L));

        mockMvc.perform(get("/api/sips/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("HDFC Flexi Cap"));
    }

    @Test
    void createSip_validRequest_returnsSip() throws Exception {
        SipRequestDTO req = new SipRequestDTO();
        req.setName("Axis Bluechip");
        req.setMonthlyAmount(new BigDecimal("2000"));

        when(sipMapper.toEntity(any())).thenReturn(sip(null));
        when(sipService.saveSip(any())).thenReturn(sip(2L));
        when(sipMapper.toDTO(any())).thenReturn(responseDTO(2L));

        mockMvc.perform(post("/api/sips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void deleteSip_returns200() throws Exception {
        mockMvc.perform(delete("/api/sips/1"))
                .andExpect(status().isOk());
        verify(sipService).deleteSip(1L);
    }

    @Test
    void getSipSummary_returnsDto() throws Exception {
        when(sipService.getTotalSipValue()).thenReturn(new BigDecimal("60000"));
        when(sipService.getTotalSipInvestment()).thenReturn(new BigDecimal("50000"));

        mockMvc.perform(get("/api/sips/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCurrentValue").value(60000))
                .andExpect(jsonPath("$.totalProfitLoss").value(10000));
    }

    @Test
    void recordPayment_returnsSip() throws Exception {
        when(sipService.recordPayment(1L)).thenReturn(sip(1L));
        when(sipMapper.toDTO(any())).thenReturn(responseDTO(1L));

        mockMvc.perform(post("/api/sips/1/pay"))
                .andExpect(status().isOk());
    }

    @Test
    void updateSip_returns200() throws Exception {
        SipRequestDTO req = new SipRequestDTO();
        req.setName("HDFC Flexi Cap Updated");
        req.setMonthlyAmount(new BigDecimal("6000"));

        when(sipService.getSipById(1L)).thenReturn(sip(1L));
        when(sipMapper.toEntity(any())).thenReturn(sip(1L));
        when(sipService.saveSip(any())).thenReturn(sip(1L));
        when(sipMapper.toDTO(any(Sip.class))).thenReturn(responseDTO(1L));

        mockMvc.perform(put("/api/sips/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void bulkDeleteSips_returnsDeletedCount() throws Exception {
        when(sipService.bulkDelete(anyList())).thenReturn(2);

        mockMvc.perform(delete("/api/sips/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[1,2]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(2));
    }
}
