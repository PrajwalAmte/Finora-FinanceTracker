package com.finance_tracker.exception;

/**
 * Thrown when a backup export or import operation fails.
 * Covers encryption/decryption errors, corrupted backup files,
 * integrity mismatches, and I/O failures during backup processing.
 */
public class BackupException extends RuntimeException {

    public BackupException(String message) {
        super(message);
    }

    public BackupException(String message, Throwable cause) {
        super(message, cause);
    }
}
