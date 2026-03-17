package com.finance_tracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance_tracker.dto.expense.ExpenseImportRequest;
import com.finance_tracker.dto.expense.ExpenseImportResultDTO;
import com.finance_tracker.dto.expense.ExpensePreviewDTO;
import com.finance_tracker.service.expense.ExpenseImportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import com.finance_tracker.utils.security.JwtService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ExpenseImportController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class, org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class})
class ExpenseImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private JwtService jwtService;


    @MockitoBean
    private ExpenseImportService expenseImportService;

    @Test
    void preview_returnsParsedTransactions() throws Exception {
        ExpensePreviewDTO preview = ExpensePreviewDTO.builder()
                .transactions(List.of())
                .warnings(List.of())
                .bankName("HDFC")
                .totalDebits(10)
                .totalCredits(2)
                .build();
        when(expenseImportService.preview(any())).thenReturn(preview);

        MockMultipartFile file = new MockMultipartFile("file", "statement.xlsx",
                "application/vnd.ms-excel", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/expense-import/preview").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalDebits").value(10))
                .andExpect(jsonPath("$.data.bankName").value("HDFC"));
    }

    @Test
    void confirm_returnsImportResult() throws Exception {
        ExpenseImportResultDTO result = ExpenseImportResultDTO.builder().imported(8).skipped(2).build();
        when(expenseImportService.confirmImport(any())).thenReturn(result);

        ExpenseImportRequest req = new ExpenseImportRequest();
        req.setExpenses(List.of());

        mockMvc.perform(post("/api/expense-import/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imported").value(8))
                .andExpect(jsonPath("$.data.skipped").value(2));
    }
}
