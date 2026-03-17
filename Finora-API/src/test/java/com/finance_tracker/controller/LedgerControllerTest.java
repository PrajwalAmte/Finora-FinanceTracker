package com.finance_tracker.controller;

import com.finance_tracker.dto.LedgerIntegrityResultDTO;
import com.finance_tracker.model.LedgerEvent;
import com.finance_tracker.service.LedgerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.finance_tracker.utils.security.JwtService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = LedgerController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class, org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class})
class LedgerControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private JwtService jwtService;


    @MockitoBean
    private LedgerService ledgerService;

    private void setAuth(String userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList()));
    }

    @Test
    void verifyIntegrity_returnsResult() throws Exception {
        setAuth("1");
        LedgerIntegrityResultDTO result = LedgerIntegrityResultDTO.builder()
                .valid(true).eventCount(42).build();
        when(ledgerService.verifyIntegrity("1")).thenReturn(result);

        mockMvc.perform(get("/api/ledger/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.eventCount").value(42));
    }

    @Test
    void getTimeline_returnsEvents() throws Exception {
        when(ledgerService.getTimeline("EXPENSE", "5")).thenReturn(List.of(new LedgerEvent()));

        mockMvc.perform(get("/api/ledger/entity/EXPENSE/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }
}
