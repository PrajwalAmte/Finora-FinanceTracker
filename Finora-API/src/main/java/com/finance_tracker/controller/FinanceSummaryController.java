package com.finance_tracker.controller;

import com.finance_tracker.service.FinanceSummaryFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;


@RestController
@RequestMapping("/api/finance-summary")
@RequiredArgsConstructor
public class FinanceSummaryController {
    
    private final FinanceSummaryFacade financeSummaryFacade;

    @GetMapping
    public FinanceSummaryFacade.ComprehensiveFinanceSummary getComprehensiveSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return financeSummaryFacade.getComprehensiveSummary(startDate, endDate);
    }
}

