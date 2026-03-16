package com.finance_tracker.controller;

import com.finance_tracker.dto.ApiResponse;
import com.finance_tracker.dto.expense.ExpenseImportRequest;
import com.finance_tracker.dto.expense.ExpenseImportResultDTO;
import com.finance_tracker.dto.expense.ExpensePreviewDTO;
import com.finance_tracker.service.expense.ExpenseImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/expense-import")
@RequiredArgsConstructor
public class ExpenseImportController {

    private final ExpenseImportService expenseImportService;

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ExpensePreviewDTO>> preview(
            @RequestParam("file") MultipartFile file) {
        ExpensePreviewDTO preview = expenseImportService.preview(file);
        String msg = preview.getTotalDebits() + " debit(s) and "
                + preview.getTotalCredits() + " credit(s) found.";
        return ResponseEntity.ok(ApiResponse.success(msg, preview));
    }

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<ExpenseImportResultDTO>> confirm(
            @RequestBody ExpenseImportRequest request) {
        ExpenseImportResultDTO result = expenseImportService.confirmImport(request);
        String msg = result.getImported() + " expense(s) imported, "
                + result.getSkipped() + " skipped.";
        return ResponseEntity.ok(ApiResponse.success(msg, result));
    }
}
