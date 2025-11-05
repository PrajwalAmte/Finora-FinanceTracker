package com.finance_tracker.controller;

import com.finance_tracker.model.Investment;
import com.finance_tracker.service.InvestmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/investments")
@RequiredArgsConstructor
public class InvestmentController {
    private final InvestmentService investmentService;

    @GetMapping
    public List<Investment> getAllInvestments() {
        return investmentService.getAllInvestments();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Investment> getInvestmentById(@PathVariable Long id) {
        return investmentService.getInvestmentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Investment createInvestment(@Valid @RequestBody Investment investment) {
        return investmentService.saveInvestment(investment);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Investment> updateInvestment(@PathVariable Long id, @Valid @RequestBody Investment investment) {
        return investmentService.getInvestmentById(id)
                .map(existingInvestment -> {
                    investment.setId(id);
                    return ResponseEntity.ok(investmentService.saveInvestment(investment));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvestment(@PathVariable Long id) {
        return investmentService.getInvestmentById(id)
                .map(investment -> {
                    investmentService.deleteInvestment(id);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/summary")
    public ResponseEntity<Object> getInvestmentSummary() {
        BigDecimal totalValue = investmentService.getTotalInvestmentValue();
        BigDecimal totalProfitLoss = investmentService.getTotalProfitLoss();

        return ResponseEntity.ok(Map.of(
                "totalValue", totalValue,
                "totalProfitLoss", totalProfitLoss
        ));
    }
}
