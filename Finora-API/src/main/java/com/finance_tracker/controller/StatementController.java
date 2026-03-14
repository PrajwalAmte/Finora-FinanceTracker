package com.finance_tracker.controller;

import com.finance_tracker.dto.ApiResponse;
import com.finance_tracker.dto.statement.StatementConfirmRequest;
import com.finance_tracker.dto.statement.StatementImportResultDTO;
import com.finance_tracker.dto.statement.StatementPreviewDTO;
import com.finance_tracker.service.statement.StatementImportService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Two-step statement import: POST /api/statements/preview and POST /api/statements/confirm. */
@RestController
@RequestMapping("/api/statements")
@RequiredArgsConstructor
public class StatementController {

    private static final Logger log = LoggerFactory.getLogger(StatementController.class);

    private final StatementImportService statementImportService;

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<StatementPreviewDTO>> preview(
            @RequestParam("file")          MultipartFile file,
            @RequestParam("statementType") String statementType,
            @RequestParam(value = "password", required = false) String password) {

        Long userId = resolveUserId();
        log.info("Statement preview request: type={}, file={}, user={}",
                statementType, file.getOriginalFilename(), userId);

        StatementPreviewDTO preview =
                statementImportService.preview(file, statementType, password, userId);

        int totalHoldings = preview.getHoldings().size() + preview.getMfHoldings().size();
        String msg = totalHoldings + " holding(s) parsed from " + statementType + " statement.";
        if (!preview.getWarnings().isEmpty()) {
            msg += " " + preview.getWarnings().size() + " warning(s) — see warnings list.";
        }

        return ResponseEntity.ok(ApiResponse.success(msg, preview));
    }

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<StatementImportResultDTO>> confirm(
            @RequestBody StatementConfirmRequest request) {

        Long userId = resolveUserId();
        log.info("Statement confirm request: type={}, selectedIsins={}, user={}",
                request.getStatementType(),
                request.getSelectedIsins() != null ? request.getSelectedIsins().size() : 0,
                userId);

        StatementImportResultDTO result = statementImportService.confirmImport(request, userId);

        String msg = result.getImported() + " imported, "
                + result.getUpdated()  + " updated, "
                + result.getSkipped()  + " skipped.";

        return ResponseEntity.ok(ApiResponse.success(msg, result));
    }

    private Long resolveUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new com.finance_tracker.exception.ResourceNotFoundException(
                    "Authentication required");
        }
        try {
            return Long.parseLong(auth.getName());
        } catch (NumberFormatException e) {
            throw new com.finance_tracker.exception.ResourceNotFoundException(
                    "Invalid authentication token");
        }
    }
}
