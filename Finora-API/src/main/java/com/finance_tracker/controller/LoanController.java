package com.finance_tracker.controller;

import com.finance_tracker.dto.LoanRequestDTO;
import com.finance_tracker.dto.LoanResponseDTO;
import com.finance_tracker.dto.LoanSummaryDTO;
import com.finance_tracker.mapper.LoanMapper;
import com.finance_tracker.model.Loan;
import com.finance_tracker.service.LoanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

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

    @DeleteMapping("/bulk")
    public ResponseEntity<Map<String, Integer>> bulkDeleteLoans(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.getOrDefault("ids", List.of());
        int deleted = loanService.bulkDelete(ids);
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }

    @GetMapping("/summary")
    public LoanSummaryDTO getLoanSummary() {
        var totalBalance = loanService.getTotalLoanBalance();

        return LoanSummaryDTO.builder()
                .totalBalance(totalBalance)
                .build();
    }
}
