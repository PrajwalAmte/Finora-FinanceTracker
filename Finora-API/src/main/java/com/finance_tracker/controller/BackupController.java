package com.finance_tracker.controller;

import com.finance_tracker.dto.ApiResponse;
import com.finance_tracker.dto.BackupExportRequestDTO;
import com.finance_tracker.dto.BackupMetadataDTO;
import com.finance_tracker.service.BackupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * REST endpoints for encrypted backup export and import.
 *
 * <ul>
 *   <li>{@code POST /api/backup/export} — Download an encrypted backup of all user data</li>
 *   <li>{@code POST /api/backup/import} — Upload and restore from an encrypted backup file</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/backup")
@RequiredArgsConstructor
public class BackupController {

    private static final String BACKUP_CONTENT_TYPE = "application/octet-stream";
    private static final long MAX_BACKUP_SIZE = 50 * 1024 * 1024; // 50 MB

    private final BackupService backupService;

    /**
     * Exports all user data as an AES-256-GCM encrypted binary file.
     *
     * @param request contains the encryption password
     * @return encrypted backup file as a binary download
     */
    @PostMapping("/export")
    public ResponseEntity<byte[]> exportBackup(@Valid @RequestBody BackupExportRequestDTO request) {
        Long userId = getAuthenticatedUserId();

        byte[] encryptedBackup = backupService.exportBackup(userId, request.getPassword());

        String filename = String.format("finora-backup-%s.enc",
                OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss")));

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(encryptedBackup);
    }

    /**
     * Imports user data from an encrypted backup file, replacing all existing data.
     *
     * @param file     the encrypted backup file
     * @param password the decryption password
     * @return metadata of the imported backup
     */
    @PostMapping("/import")
    public ResponseEntity<ApiResponse<BackupMetadataDTO>> importBackup(
            @RequestParam("file") MultipartFile file,
            @RequestParam("password") String password) {

        Long userId = getAuthenticatedUserId();

        // Validate file
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Backup file is required", "VALIDATION_ERROR"));
        }

        if (file.getSize() > MAX_BACKUP_SIZE) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Backup file exceeds maximum size of 50 MB", "VALIDATION_ERROR"));
        }

        if (password == null || password.length() < 8) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Password must be at least 8 characters", "VALIDATION_ERROR"));
        }

        try {
            byte[] encryptedData = file.getBytes();
            BackupMetadataDTO metadata = backupService.importBackup(userId, encryptedData, password);

            return ResponseEntity.ok(ApiResponse.success("Backup imported successfully", metadata));
        } catch (java.io.IOException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to read uploaded file", "IO_ERROR"));
        }
    }

    private Long getAuthenticatedUserId() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return Long.valueOf(userId);
    }
}
