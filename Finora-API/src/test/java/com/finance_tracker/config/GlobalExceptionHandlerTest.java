package com.finance_tracker.config;

import com.finance_tracker.exception.BackupException;
import com.finance_tracker.exception.BusinessLogicException;
import com.finance_tracker.exception.ExternalApiException;
import com.finance_tracker.exception.ResourceNotFoundException;
import com.finance_tracker.exception.StatementParseException;
import com.finance_tracker.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.MapBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleResourceNotFoundException_returns404() {
        var resp = handler.handleResourceNotFoundException(new ResourceNotFoundException("Expense not found"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().isSuccess()).isFalse();
        assertThat(resp.getBody().getErrorCode()).isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void handleValidationException_returns400() {
        var resp = handler.handleValidationException(new ValidationException("Invalid amount"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getErrorCode()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void handleBusinessLogicException_returns400() {
        var resp = handler.handleBusinessLogicException(new BusinessLogicException("Already exists"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getErrorCode()).isEqualTo("BUSINESS_LOGIC_ERROR");
    }

    @Test
    void handleExternalApiException_returns503() {
        var resp = handler.handleExternalApiException(new ExternalApiException("BSE down"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(resp.getBody().getErrorCode()).isEqualTo("EXTERNAL_API_ERROR");
        assertThat(resp.getBody().getMessage()).isEqualTo("External service unavailable");
    }

    @Test
    void handleBackupException_returns400() {
        var resp = handler.handleBackupException(new BackupException("Corrupt file"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getErrorCode()).isEqualTo("BACKUP_ERROR");
    }

    @Test
    void handleStatementParseException_returns422WithRowErrors() {
        StatementParseException ex = new StatementParseException("Parse failed", List.of("Row 2 bad", "Row 5 bad"));
        ResponseEntity<?> resp = handler.handleStatementParseException(ex);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void handleStatementParseException_emptyRowErrors_dataIsNull() {
        StatementParseException ex = new StatementParseException("Parse failed");
        ResponseEntity<?> resp = handler.handleStatementParseException(ex);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void handleMethodArgumentNotValidException_returns400WithFieldErrors() {
        BindingResult br = new MapBindingResult(new java.util.HashMap<>(), "dto");
        br.addError(new FieldError("dto", "email", "must not be blank"));
        MethodArgumentNotValidException ex = Mockito.mock(MethodArgumentNotValidException.class);
        Mockito.when(ex.getBindingResult()).thenReturn(br);

        ResponseEntity<com.finance_tracker.dto.ApiResponse<Map<String, String>>> resp =
                handler.handleMethodArgumentNotValidException(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getData()).containsKey("email");
    }

    @Test
    void handleGenericException_returns500() {
        var resp = handler.handleGenericException(new RuntimeException("Unexpected"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resp.getBody().getErrorCode()).isEqualTo("INTERNAL_SERVER_ERROR");
    }
}
