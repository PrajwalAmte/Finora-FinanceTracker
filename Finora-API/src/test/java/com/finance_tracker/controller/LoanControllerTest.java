package com.finance_tracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance_tracker.dto.LoanRequestDTO;
import com.finance_tracker.dto.LoanResponseDTO;
import com.finance_tracker.dto.LoanSummaryDTO;
import com.finance_tracker.mapper.LoanMapper;
import com.finance_tracker.model.CompoundingFrequency;
import com.finance_tracker.model.Loan;
import com.finance_tracker.model.LoanInterestType;
import com.finance_tracker.service.LoanService;
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

@WebMvcTest(controllers = LoanController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class, org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class})
class LoanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private JwtService jwtService;


    @MockitoBean
    private LoanService loanService;

    @MockitoBean
    private LoanMapper loanMapper;

    private Loan loan(Long id) {
        Loan l = new Loan();
        l.setId(id);
        l.setName("Home Loan");
        l.setPrincipalAmount(new BigDecimal("500000"));
        l.setInterestRate(new BigDecimal("8.5"));
        l.setInterestType(LoanInterestType.COMPOUND);
        l.setCompoundingFrequency(CompoundingFrequency.MONTHLY);
        l.setStartDate(LocalDate.of(2023, 1, 1));
        l.setTenureMonths(240);
        return l;
    }

    private LoanResponseDTO responseDTO(Long id) {
        return LoanResponseDTO.builder()
                .id(id).name("Home Loan")
                .principalAmount(new BigDecimal("500000"))
                .interestRate(new BigDecimal("8.5"))
                .interestType(LoanInterestType.COMPOUND)
                .build();
    }

    @Test
    void getAllLoans_returnsList() throws Exception {
        when(loanService.getAllLoans()).thenReturn(List.of(loan(1L)));
        when(loanMapper.toDTOList(anyList())).thenReturn(List.of(responseDTO(1L)));

        mockMvc.perform(get("/api/loans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getLoanById_returnsLoan() throws Exception {
        when(loanService.getLoanById(1L)).thenReturn(loan(1L));
        when(loanMapper.toDTO(any())).thenReturn(responseDTO(1L));

        mockMvc.perform(get("/api/loans/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Home Loan"));
    }

    @Test
    void createLoan_validRequest_returnsLoan() throws Exception {
        LoanRequestDTO req = new LoanRequestDTO();
        req.setName("Car Loan");
        req.setPrincipalAmount(new BigDecimal("300000"));
        req.setInterestRate(new BigDecimal("9.0"));
        req.setInterestType(LoanInterestType.SIMPLE);
        req.setCompoundingFrequency(CompoundingFrequency.YEARLY);
        req.setStartDate(LocalDate.now());
        req.setTenureMonths(60);

        when(loanMapper.toEntity(any())).thenReturn(loan(null));
        when(loanService.saveLoan(any())).thenReturn(loan(2L));
        when(loanMapper.toDTO(any())).thenReturn(responseDTO(2L));

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void deleteLoan_returns200() throws Exception {
        mockMvc.perform(delete("/api/loans/1"))
                .andExpect(status().isOk());
        verify(loanService).deleteLoan(1L);
    }

    @Test
    void getLoanSummary_returnsDto() throws Exception {
        when(loanService.getTotalLoanBalance()).thenReturn(new BigDecimal("1200000"));

        mockMvc.perform(get("/api/loans/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBalance").value(1200000));
    }

    @Test
    void updateLoan_returns200() throws Exception {
        LoanRequestDTO req = new LoanRequestDTO();
        req.setName("Home Loan Updated");
        req.setPrincipalAmount(new BigDecimal("450000"));
        req.setInterestRate(new BigDecimal("8.0"));
        req.setInterestType(LoanInterestType.COMPOUND);
        req.setCompoundingFrequency(CompoundingFrequency.MONTHLY);
        req.setStartDate(LocalDate.now());
        req.setTenureMonths(240);

        when(loanService.getLoanById(1L)).thenReturn(loan(1L));
        when(loanMapper.toEntity(any())).thenReturn(loan(1L));
        when(loanService.saveLoan(any())).thenReturn(loan(1L));
        when(loanMapper.toDTO(any())).thenReturn(responseDTO(1L));

        mockMvc.perform(put("/api/loans/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void bulkDeleteLoans_returnsDeletedCount() throws Exception {
        when(loanService.bulkDelete(anyList())).thenReturn(2);

        mockMvc.perform(delete("/api/loans/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[1,2]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(2));
    }
}
