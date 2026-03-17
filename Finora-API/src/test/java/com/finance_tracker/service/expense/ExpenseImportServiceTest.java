package com.finance_tracker.service.expense;

import com.finance_tracker.dto.expense.ExpenseImportRequest;
import com.finance_tracker.dto.expense.ExpenseImportResultDTO;
import com.finance_tracker.dto.expense.ExpensePreviewDTO;
import com.finance_tracker.exception.StatementParseException;
import com.finance_tracker.model.Expense;
import com.finance_tracker.service.ExpenseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExpenseImportServiceTest {

    @Mock
    private BankStatementParser parser;

    @Mock
    private ExpenseService expenseService;

    @InjectMocks
    private ExpenseImportService expenseImportService;

    // ─── preview ──────────────────────────────────────────────────────────────

    @Test
    void preview_success() throws IOException {
        byte[] bytes = "date,description,amount\n2024-01-01,Coffee,150".getBytes();
        MultipartFile file = mock(MultipartFile.class);
        when(file.getBytes()).thenReturn(bytes);
        when(file.getOriginalFilename()).thenReturn("statement.csv");

        ExpensePreviewDTO dto = ExpensePreviewDTO.builder()
                .bankName("HDFC")
                .totalDebits(1)
                .totalCredits(0)
                .build();
        when(parser.parse(bytes, "statement.csv")).thenReturn(dto);

        ExpensePreviewDTO result = expenseImportService.preview(file);

        assertThat(result.getBankName()).isEqualTo("HDFC");
        assertThat(result.getTotalDebits()).isEqualTo(1);
    }

    @Test
    void preview_fileReadFailure() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getBytes()).thenThrow(new IOException("disk read error"));

        assertThatThrownBy(() -> expenseImportService.preview(file))
                .isInstanceOf(StatementParseException.class)
                .hasMessageContaining("disk read error");
    }

    // ─── confirmImport ────────────────────────────────────────────────────────

    private ExpenseImportRequest.ExpenseEntry validEntry(String desc, String cat) {
        ExpenseImportRequest.ExpenseEntry e = new ExpenseImportRequest.ExpenseEntry();
        e.setDescription(desc);
        e.setAmount(new BigDecimal("250.00"));
        e.setCategory(cat);
        e.setPaymentMethod("UPI");
        e.setDate("2024-01-15");
        return e;
    }

    private ExpenseImportRequest requestOf(ExpenseImportRequest.ExpenseEntry... entries) {
        ExpenseImportRequest req = new ExpenseImportRequest();
        req.setExpenses(List.of(entries));
        return req;
    }

    @Test
    void confirmImport_allValid() {
        ExpenseImportRequest req = requestOf(
                validEntry("Coffee", "Food"),
                validEntry("Bus ticket", "Transport"));

        ExpenseImportResultDTO result = expenseImportService.confirmImport(req);

        assertThat(result.getImported()).isEqualTo(2);
        assertThat(result.getSkipped()).isEqualTo(0);
        verify(expenseService, times(2)).saveExpense(any());
    }

    @Test
    void confirmImport_skipsNullAmount() {
        ExpenseImportRequest.ExpenseEntry e = validEntry("Lunch", "Food");
        e.setAmount(null);

        ExpenseImportResultDTO result = expenseImportService.confirmImport(requestOf(e));

        assertThat(result.getSkipped()).isEqualTo(1);
        assertThat(result.getImported()).isEqualTo(0);
        verifyNoInteractions(expenseService);
    }

    @Test
    void confirmImport_skipsZeroAmount() {
        ExpenseImportRequest.ExpenseEntry e = validEntry("Lunch", "Food");
        e.setAmount(BigDecimal.ZERO);

        ExpenseImportResultDTO result = expenseImportService.confirmImport(requestOf(e));

        assertThat(result.getSkipped()).isEqualTo(1);
        assertThat(result.getImported()).isEqualTo(0);
        verifyNoInteractions(expenseService);
    }

    @Test
    void confirmImport_skipsBlankDescription() {
        ExpenseImportRequest.ExpenseEntry e = validEntry("   ", "Food");

        ExpenseImportResultDTO result = expenseImportService.confirmImport(requestOf(e));

        assertThat(result.getSkipped()).isEqualTo(1);
        assertThat(result.getImported()).isEqualTo(0);
    }

    @Test
    void confirmImport_skipsBlankCategory() {
        ExpenseImportRequest.ExpenseEntry e = validEntry("Coffee", "");

        ExpenseImportResultDTO result = expenseImportService.confirmImport(requestOf(e));

        assertThat(result.getSkipped()).isEqualTo(1);
        assertThat(result.getImported()).isEqualTo(0);
    }

    @Test
    void confirmImport_defaultsPaymentMethodWhenBlank() {
        ExpenseImportRequest.ExpenseEntry e = validEntry("Rent", "Housing");
        e.setPaymentMethod("   ");

        expenseImportService.confirmImport(requestOf(e));

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseService).saveExpense(captor.capture());
        assertThat(captor.getValue().getPaymentMethod()).isEqualTo("Net Banking");
    }

    @Test
    void confirmImport_parsesValidDate() {
        ExpenseImportRequest.ExpenseEntry e = validEntry("Grocery", "Food");
        e.setDate("2024-01-15");

        expenseImportService.confirmImport(requestOf(e));

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseService).saveExpense(captor.capture());
        assertThat(captor.getValue().getDate()).isEqualTo(LocalDate.of(2024, 1, 15));
    }

    @Test
    void confirmImport_usesTodayForInvalidDate() {
        ExpenseImportRequest.ExpenseEntry e = validEntry("Grocery", "Food");
        e.setDate("not-a-date");

        expenseImportService.confirmImport(requestOf(e));

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseService).saveExpense(captor.capture());
        assertThat(captor.getValue().getDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void confirmImport_serviceExceptionCountsAsSkipped() {
        doThrow(new RuntimeException("DB write failed")).when(expenseService).saveExpense(any());
        ExpenseImportRequest.ExpenseEntry e = validEntry("Coffee", "Food");

        ExpenseImportResultDTO result = expenseImportService.confirmImport(requestOf(e));

        assertThat(result.getSkipped()).isEqualTo(1);
        assertThat(result.getImported()).isEqualTo(0);
    }
}
