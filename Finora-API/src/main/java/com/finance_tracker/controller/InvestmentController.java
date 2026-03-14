package com.finance_tracker.controller;

import com.finance_tracker.dto.InvestmentRequestDTO;
import com.finance_tracker.dto.InvestmentResponseDTO;
import com.finance_tracker.dto.InvestmentSummaryDTO;
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

    @GetMapping("/summary")
    public InvestmentSummaryDTO getInvestmentSummary() {
        var totalValue = investmentService.getTotalInvestmentValue();
        var totalProfitLoss = investmentService.getTotalProfitLoss();

        return InvestmentSummaryDTO.builder()
                .totalValue(totalValue)
                .totalProfitLoss(totalProfitLoss)
                .build();
    }

    /**
     * Fires a price/NAV refresh in a background thread and returns 202 immediately.
     * The frontend should reload data after a short delay (e.g. 8–10 seconds).
     */
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

    /**
     * Searches AMFI mutual fund schemes by name keyword.
     * Returns up to 15 matches with schemeCode, name, and current NAV.
     */
    @GetMapping("/search-mf")
    public List<Map<String, Object>> searchMf(@RequestParam(required = false, defaultValue = "") String q) {
        return amfiNavService.searchByName(q);
    }
}
