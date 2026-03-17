package com.finance_tracker.controller;

import com.finance_tracker.dto.ApiResponse;
import com.finance_tracker.dto.LedgerIntegrityResultDTO;
import com.finance_tracker.model.LedgerEvent;
import com.finance_tracker.service.LedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ledger")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerService ledgerService;

    @GetMapping("/verify")
    public ResponseEntity<ApiResponse<LedgerIntegrityResultDTO>> verifyIntegrity() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        LedgerIntegrityResultDTO result = ledgerService.verifyIntegrity(userId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/entity/{type}/{id}")
    public ResponseEntity<ApiResponse<List<LedgerEvent>>> getTimeline(
            @PathVariable String type,
            @PathVariable String id) {
        List<LedgerEvent> timeline = ledgerService.getTimeline(type, id);
        return ResponseEntity.ok(ApiResponse.success(timeline));
    }
}
