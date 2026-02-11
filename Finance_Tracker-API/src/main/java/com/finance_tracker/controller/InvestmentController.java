package com.finance_tracker.controller;

import com.finance_tracker.dto.InvestmentRequestDTO;
import com.finance_tracker.dto.InvestmentResponseDTO;
import com.finance_tracker.dto.InvestmentSummaryDTO;
import com.finance_tracker.mapper.InvestmentMapper;
import com.finance_tracker.model.Investment;
import com.finance_tracker.service.InvestmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/api/investments")
@RequiredArgsConstructor
public class InvestmentController {
    private final InvestmentService investmentService;
    private final InvestmentMapper investmentMapper;

    @GetMapping
    public List<InvestmentResponseDTO> getAllInvestments() {
        List<Investment> investments = investmentService.getAllInvestments();
        return investmentMapper.toDTOList(investments);
    }

    @GetMapping("/{id}")
    public InvestmentResponseDTO getInvestmentById(@PathVariable Long id) {
        Investment investment = investmentService.getInvestmentById(id);
        return investmentMapper.toDTO(investment);
    }

    @PostMapping
    public InvestmentResponseDTO createInvestment(@Valid @RequestBody InvestmentRequestDTO investmentDTO) {
        Investment investment = investmentMapper.toEntity(investmentDTO);
        Investment savedInvestment = investmentService.saveInvestment(investment);
        return investmentMapper.toDTO(savedInvestment);
    }

    @PutMapping("/{id}")
    public InvestmentResponseDTO updateInvestment(@PathVariable Long id, @Valid @RequestBody InvestmentRequestDTO investmentDTO) {
        investmentService.getInvestmentById(id);
        
        Investment investment = investmentMapper.toEntity(investmentDTO);
        investment.setId(id);
        Investment updatedInvestment = investmentService.saveInvestment(investment);
        return investmentMapper.toDTO(updatedInvestment);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvestment(@PathVariable Long id) {
        investmentService.deleteInvestment(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/summary")
    public InvestmentSummaryDTO getInvestmentSummary() {
        var totalValue = investmentService.getTotalInvestmentValue();
        var totalProfitLoss = investmentService.getTotalProfitLoss();

        return InvestmentSummaryDTO.builder()
                .totalValue(totalValue)
                .totalProfitLoss(totalProfitLoss)
                .build();
    }
}
