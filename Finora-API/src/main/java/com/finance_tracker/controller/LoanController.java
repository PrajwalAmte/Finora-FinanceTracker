package com.finance_tracker.controller;

import com.finance_tracker.dto.LoanRequestDTO;
import com.finance_tracker.dto.LoanResponseDTO;
import com.finance_tracker.dto.LoanSummaryDTO;
import com.finance_tracker.mapper.LoanMapper;
import com.finance_tracker.model.Loan;
import com.finance_tracker.service.LoanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {
    private final LoanService loanService;
    private final LoanMapper loanMapper;

    @GetMapping
    public List<LoanResponseDTO> getAllLoans() {
        List<Loan> loans = loanService.getAllLoans();
        return loanMapper.toDTOList(loans);
    }

    @GetMapping("/{id}")
    public LoanResponseDTO getLoanById(@PathVariable Long id) {
        Loan loan = loanService.getLoanById(id);
        return loanMapper.toDTO(loan);
    }

    @PostMapping
    public LoanResponseDTO createLoan(@Valid @RequestBody LoanRequestDTO loanDTO) {
        Loan loan = loanMapper.toEntity(loanDTO);
        Loan savedLoan = loanService.saveLoan(loan);
        return loanMapper.toDTO(savedLoan);
    }

    @PutMapping("/{id}")
    public LoanResponseDTO updateLoan(@PathVariable Long id, @Valid @RequestBody LoanRequestDTO loanDTO) {
        // Verify loan exists
        loanService.getLoanById(id);
        
        Loan loan = loanMapper.toEntity(loanDTO);
        loan.setId(id);
        Loan updatedLoan = loanService.saveLoan(loan);
        return loanMapper.toDTO(updatedLoan);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLoan(@PathVariable Long id) {
        loanService.deleteLoan(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/summary")
    public LoanSummaryDTO getLoanSummary() {
        var totalBalance = loanService.getTotalLoanBalance();

        return LoanSummaryDTO.builder()
                .totalBalance(totalBalance)
                .build();
    }
}
