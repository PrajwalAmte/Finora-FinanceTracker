package com.finance_tracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance_tracker.dto.statement.StatementConfirmRequest;
import com.finance_tracker.dto.statement.StatementImportResultDTO;
import com.finance_tracker.dto.statement.StatementPreviewDTO;
import com.finance_tracker.service.statement.StatementImportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.finance_tracker.utils.security.JwtService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = StatementController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class, org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class})
class StatementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private JwtService jwtService;


    @MockitoBean
    private StatementImportService statementImportService;

    private void setAuth() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("1", null, Collections.emptyList()));
    }

    @Test
    void preview_returnsPreviewDto() throws Exception {
        setAuth();
        StatementPreviewDTO preview = StatementPreviewDTO.builder()
                .holdings(List.of())
                .mfHoldings(List.of())
                .warnings(List.of())
                .build();

        when(statementImportService.preview(any(), eq("CDSL"), isNull(), eq(1L))).thenReturn(preview);

        MockMultipartFile file = new MockMultipartFile("file", "stmt.pdf", "application/pdf", new byte[]{1, 2});
        mockMvc.perform(multipart("/api/statements/preview")
                        .file(file)
                        .param("statementType", "CDSL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.holdings").isArray());
    }

    @Test
    void confirm_returnsImportResult() throws Exception {
        setAuth();
        StatementImportResultDTO result = StatementImportResultDTO.builder()
                .imported(5).updated(2).skipped(1).skippedReasons(Map.of()).warnings(List.of()).build();

        when(statementImportService.confirmImport(any(), eq(1L))).thenReturn(result);

        StatementConfirmRequest req = new StatementConfirmRequest();
        req.setStatementType("CDSL");
        req.setSelectedIsins(List.of("INF001"));
        req.setHoldings(List.of());
        req.setMfHoldings(List.of());

        mockMvc.perform(post("/api/statements/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imported").value(5))
                .andExpect(jsonPath("$.data.skipped").value(1));
    }
}
