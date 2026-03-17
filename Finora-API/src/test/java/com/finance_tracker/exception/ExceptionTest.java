package com.finance_tracker.exception;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionTest {

    @Test
    void resourceNotFoundException_withNameAndId_formatsMessage() {
        var ex = new ResourceNotFoundException("Expense", 42L);
        assertThat(ex.getMessage()).contains("Expense").contains("42");
    }

    @Test
    void resourceNotFoundException_withMessage_returnsMessage() {
        var ex = new ResourceNotFoundException("custom msg");
        assertThat(ex.getMessage()).isEqualTo("custom msg");
    }

    @Test
    void businessLogicException_message() {
        var ex = new BusinessLogicException("business error");
        assertThat(ex.getMessage()).isEqualTo("business error");
    }

    @Test
    void businessLogicException_withCause() {
        var cause = new RuntimeException("root");
        var ex = new BusinessLogicException("msg", cause);
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void validationException_message() {
        var ex = new ValidationException("invalid");
        assertThat(ex.getMessage()).isEqualTo("invalid");
    }

    @Test
    void validationException_withCause() {
        var cause = new IllegalArgumentException("arg");
        var ex = new ValidationException("msg", cause);
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void externalApiException_message() {
        var ex = new ExternalApiException("api down");
        assertThat(ex.getMessage()).isEqualTo("api down");
    }

    @Test
    void externalApiException_withCause() {
        var cause = new RuntimeException("network");
        var ex = new ExternalApiException("msg", cause);
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void backupException_message() {
        var ex = new BackupException("backup failed");
        assertThat(ex.getMessage()).isEqualTo("backup failed");
    }

    @Test
    void backupException_withCause() {
        var cause = new RuntimeException("io");
        var ex = new BackupException("msg", cause);
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void statementParseException_messageOnly_emptyRowErrors() {
        var ex = new StatementParseException("parse failed");
        assertThat(ex.getMessage()).isEqualTo("parse failed");
        assertThat(ex.getRowErrors()).isEmpty();
    }

    @Test
    void statementParseException_withRowErrors_copiesErrors() {
        var ex = new StatementParseException("parse failed", List.of("row 1 error", "row 2 error"));
        assertThat(ex.getRowErrors()).containsExactly("row 1 error", "row 2 error");
    }

    @Test
    void statementParseException_nullRowErrors_treatedAsEmpty() {
        var ex = new StatementParseException("msg", (List<String>) null);
        assertThat(ex.getRowErrors()).isEmpty();
    }

    @Test
    void statementParseException_withCause_emptyRowErrors() {
        var cause = new RuntimeException("io");
        var ex = new StatementParseException("msg", cause);
        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(ex.getRowErrors()).isEmpty();
    }
}
