package com.finance_tracker.controller;

import com.finance_tracker.dto.InvestmentRequestDTO;
import com.finance_tracker.dto.InvestmentResponseDTO;
import com.finance_tracker.dto.InvestmentSummaryDTO;
import com.finance_tracker.dto.InvestmentTradeRequestDTO;
import com.finance_tracker.mapper.InvestmentMapper;
import com.finance_tracker.model.Investment;
import com.finance_tracker.service.AmfiNavService;
import com.finance_tracker.service.InvestmentService;
import com.finance_tracker.service.SipService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/investments")
@RequiredArgsConstructor
public class InvestmentController {
    private static final Logger logger = LoggerFactory.getLogger(InvestmentController.class);

    private final InvestmentService investmentService;
    private final InvestmentMapper investmentMapper;
    private final SipService sipService;
    private final AmfiNavService amfiNavService;

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

    @DeleteMapping("/bulk")
    public ResponseEntity<Map<String, Integer>> bulkDeleteInvestments(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.getOrDefault("ids", List.of());
        int deleted = investmentService.bulkDelete(ids);
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }

    @GetMapping("/summary")
    public InvestmentSummaryDTO getInvestmentSummary() {
        var sipLinkedIds = sipService.getLinkedInvestmentIds();
        var totalValue      = investmentService.getTotalInvestmentValueExcluding(sipLinkedIds);
        var totalProfitLoss = investmentService.getTotalProfitLossExcluding(sipLinkedIds);

        return InvestmentSummaryDTO.builder()
                .totalValue(totalValue)
                .totalProfitLoss(totalProfitLoss)
                .build();
    }

    @PostMapping("/refresh-prices")
    public ResponseEntity<Map<String, String>> refreshPrices() {
        Thread.ofVirtual().start(() -> {
            try {
                investmentService.updateCurrentPrices();
                sipService.updateCurrentNavs();
            } catch (Exception e) {
                logger.error("Background price refresh failed: {}", e.getMessage(), e);
            }
        });
        return ResponseEntity.accepted().body(Map.of(
                "status", "refresh_started",
                "message", "Price refresh running in background. Reload in ~10 seconds."
        ));
    }

    @GetMapping("/search-mf")
    public List<Map<String, Object>> searchMf(@RequestParam(required = false, defaultValue = "") String q) {
        return amfiNavService.searchByName(q);
    }

    @PostMapping("/{id}/add-units")
    public InvestmentResponseDTO addUnits(
            @PathVariable Long id,
            @Valid @RequestBody InvestmentTradeRequestDTO dto) {
        Investment updated = investmentService.addUnits(id, dto.getQuantity(), dto.getPrice());
        return investmentMapper.toDTO(updated);
    }

    @PostMapping("/{id}/sell-units")
    public ResponseEntity<?> sellUnits(
            @PathVariable Long id,
            @Valid @RequestBody InvestmentTradeRequestDTO dto) {
        return investmentService.sellUnits(id, dto.getQuantity(), dto.getPrice())
                .<ResponseEntity<?>>map(inv -> ResponseEntity.ok(investmentMapper.toDTO(inv)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
