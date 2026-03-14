package com.finance_tracker.exception;

import java.util.List;

/** Thrown when a statement file cannot be parsed. Maps to HTTP 422 via GlobalExceptionHandler. */
public class StatementParseException extends RuntimeException {

    private final List<String> rowErrors;

    public StatementParseException(String message) {
        super(message);
        this.rowErrors = List.of();
    }

    public StatementParseException(String message, List<String> rowErrors) {
        super(message);
        this.rowErrors = rowErrors != null ? List.copyOf(rowErrors) : List.of();
    }

    public StatementParseException(String message, Throwable cause) {
        super(message, cause);
        this.rowErrors = List.of();
    }

    public List<String> getRowErrors() {
        return rowErrors;
    }
}
