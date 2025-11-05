package com.finance_tracker.controller;

import com.finance_tracker.model.Loan;
import com.finance_tracker.service.LoanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {
    private final LoanService loanService;

    @GetMapping
    public List<Loan> getAllLoans() {
        return loanService.getAllLoans();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Loan> getLoanById(@PathVariable Long id) {
        return loanService.getLoanById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Loan createLoan(@Valid @RequestBody Loan loan) {
        return loanService.saveLoan(loan);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Loan> updateLoan(@PathVariable Long id, @Valid @RequestBody Loan loan) {
        return loanService.getLoanById(id)
                .map(existingLoan -> {
                    loan.setId(id);
                    return ResponseEntity.ok(loanService.saveLoan(loan));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLoan(@PathVariable Long id) {
        return loanService.getLoanById(id)
                .map(loan -> {
                    loanService.deleteLoan(id);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/summary")
    public ResponseEntity<Object> getLoanSummary() {
        BigDecimal totalBalance = loanService.getTotalLoanBalance();

        return ResponseEntity.ok(Map.of(
                "totalBalance", totalBalance
        ));
    }
}
