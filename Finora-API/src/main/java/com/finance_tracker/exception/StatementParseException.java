package com.finance_tracker.exception;

import java.util.List;

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
