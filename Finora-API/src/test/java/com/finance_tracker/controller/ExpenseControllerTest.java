package com.finance_tracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance_tracker.dto.ExpenseRequestDTO;
import com.finance_tracker.dto.ExpenseResponseDTO;
import com.finance_tracker.dto.ExpenseSummaryDTO;
import com.finance_tracker.mapper.ExpenseMapper;
import com.finance_tracker.model.Expense;
import com.finance_tracker.service.ExpenseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import com.finance_tracker.utils.security.JwtService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ExpenseController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class, org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class})
class ExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private JwtService jwtService;


    @MockitoBean
    private ExpenseService expenseService;

    @MockitoBean
    private ExpenseMapper expenseMapper;

    private Expense expense(Long id) {
        Expense e = new Expense();
        e.setId(id);
        e.setDescription("Coffee");
        e.setAmount(new BigDecimal("50.00"));
        e.setDate(LocalDate.of(2024, 1, 10));
        e.setCategory("Food");
        return e;
    }

    private ExpenseResponseDTO responseDTO(Long id) {
        return ExpenseResponseDTO.builder()
                .id(id).description("Coffee").amount(new BigDecimal("50.00"))
                .date(LocalDate.of(2024, 1, 10)).category("Food").build();
    }

    @Test
    void getAllExpenses_returnsList() throws Exception {
        when(expenseService.getAllExpenses(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(expense(1L))));
        when(expenseMapper.toDTOList(anyList())).thenReturn(List.of(responseDTO(1L)));

        mockMvc.perform(get("/api/expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getExpenseById_returnsExpense() throws Exception {
        when(expenseService.getExpenseById(1L)).thenReturn(expense(1L));
        when(expenseMapper.toDTO(any())).thenReturn(responseDTO(1L));

        mockMvc.perform(get("/api/expenses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Coffee"));
    }

    @Test
    void createExpense_validRequest_returnsCreated() throws Exception {
        ExpenseRequestDTO req = new ExpenseRequestDTO();
        req.setDescription("Lunch");
        req.setAmount(new BigDecimal("200.00"));
        req.setDate(LocalDate.now());
        req.setCategory("Food");
        req.setPaymentMethod("UPI");

        when(expenseMapper.toEntity(any())).thenReturn(expense(null));
        when(expenseService.saveExpense(any())).thenReturn(expense(2L));
        when(expenseMapper.toDTO(any())).thenReturn(responseDTO(2L));

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void deleteExpense_returns200() throws Exception {
        mockMvc.perform(delete("/api/expenses/1"))
                .andExpect(status().isOk());
        verify(expenseService).deleteExpense(1L);
    }

    @Test
    void bulkDeleteExpenses_returnsDeletedCount() throws Exception {
        when(expenseService.bulkDelete(anyList())).thenReturn(3);

        mockMvc.perform(delete("/api/expenses/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[1,2,3]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(3));
    }

    @Test
    void getExpenseSummary_returnsDto() throws Exception {
        when(expenseService.getTotalExpenses(any(), any())).thenReturn(new BigDecimal("1500.00"));
        when(expenseService.getExpensesByCategory(any(), any())).thenReturn(Map.of("Food", new BigDecimal("500.00")));

        mockMvc.perform(get("/api/expenses/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpenses").value(1500.00));
    }

    @Test
    void updateExpense_returns200() throws Exception {
        ExpenseRequestDTO req = new ExpenseRequestDTO();
        req.setDescription("Dinner");
        req.setAmount(new BigDecimal("300.00"));
        req.setDate(LocalDate.now());
        req.setCategory("Food");
        req.setPaymentMethod("Cash");

        when(expenseService.getExpenseById(1L)).thenReturn(expense(1L));
        when(expenseMapper.toEntity(any())).thenReturn(expense(1L));
        when(expenseService.saveExpense(any())).thenReturn(expense(1L));
        when(expenseMapper.toDTO(any())).thenReturn(responseDTO(1L));

        mockMvc.perform(put("/api/expenses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void bulkUpdateExpenses_returnsUpdatedCount() throws Exception {
        when(expenseService.bulkUpdate(anyList(), anyString(), anyString())).thenReturn(2);

        mockMvc.perform(put("/api/expenses/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[1,2],\"category\":\"Food\",\"paymentMethod\":\"UPI\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(2));
    }

    @Test
    void getExpensesByDateRange_returnsList() throws Exception {
        when(expenseService.getExpensesBetweenDates(any(), any())).thenReturn(List.of(expense(1L)));
        when(expenseMapper.toDTOList(anyList())).thenReturn(List.of(responseDTO(1L)));

        mockMvc.perform(get("/api/expenses/by-date-range")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getExpensesByCategory_returnsList() throws Exception {
        when(expenseService.getExpensesByCategory("Food")).thenReturn(List.of(expense(1L)));
        when(expenseMapper.toDTOList(anyList())).thenReturn(List.of(responseDTO(1L)));

        mockMvc.perform(get("/api/expenses/by-category").param("category", "Food"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getAverageMonthlyExpense_returnsValue() throws Exception {
        when(expenseService.getAverageMonthlyExpense(null)).thenReturn(new BigDecimal("1200.00"));

        mockMvc.perform(get("/api/expenses/average-monthly"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(1200.00));
    }
}
