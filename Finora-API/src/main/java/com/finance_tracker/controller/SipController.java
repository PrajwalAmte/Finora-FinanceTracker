package com.finance_tracker.controller;

import com.finance_tracker.dto.SipRequestDTO;
import com.finance_tracker.dto.SipResponseDTO;
import com.finance_tracker.dto.SipSummaryDTO;
import com.finance_tracker.mapper.SipMapper;
import com.finance_tracker.model.Investment;
import com.finance_tracker.model.Sip;
import com.finance_tracker.repository.InvestmentRepository;
import com.finance_tracker.service.SipService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sips")
@RequiredArgsConstructor
public class SipController {
    private final SipService sipService;
    private final SipMapper sipMapper;
    private final InvestmentRepository investmentRepository;

    @GetMapping
    public List<SipResponseDTO> getAllSips() {
        List<Sip> sips = sipService.getAllSips();

        List<Long> linkedIds = sips.stream()
                .filter(s -> s.getInvestmentId() != null)
                .map(Sip::getInvestmentId)
                .toList();
        Map<Long, Investment> linkedInvestments = investmentRepository.findAllById(linkedIds)
                .stream()
                .collect(Collectors.toMap(Investment::getId, i -> i));

        return sipMapper.toDTOList(sips, linkedInvestments);
    }

    @GetMapping("/{id}")
    public SipResponseDTO getSipById(@PathVariable Long id) {
        Sip sip = sipService.getSipById(id);
        Investment linkedInv = sip.getInvestmentId() != null
                ? investmentRepository.findById(sip.getInvestmentId()).orElse(null)
                : null;
        return sipMapper.toDTO(sip, linkedInv);
    }

    @PostMapping
    public SipResponseDTO createSip(@Valid @RequestBody SipRequestDTO sipDTO) {
        Sip sip = sipMapper.toEntity(sipDTO);
        Sip savedSip = sipService.saveSip(sip);
        return sipMapper.toDTO(savedSip);
    }

    @PutMapping("/{id}")
    public SipResponseDTO updateSip(@PathVariable Long id, @Valid @RequestBody SipRequestDTO sipDTO) {
        sipService.getSipById(id);
        
        Sip sip = sipMapper.toEntity(sipDTO);
        sip.setId(id);
        Sip updatedSip = sipService.saveSip(sip);
        return sipMapper.toDTO(updatedSip);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSip(@PathVariable Long id) {
        sipService.deleteSip(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/summary")
    public SipSummaryDTO getSipSummary() {
        var totalValue = sipService.getTotalSipValue();
        var totalInvestment = sipService.getTotalSipInvestment();
        var totalProfitLoss = totalValue.subtract(totalInvestment);

        return SipSummaryDTO.builder()
                .totalInvestment(totalInvestment)
                .totalCurrentValue(totalValue)
                .totalProfitLoss(totalProfitLoss)
                .build();
    }

    @PostMapping("/{id}/pay")
    public SipResponseDTO recordPayment(@PathVariable Long id) {
        return sipMapper.toDTO(sipService.recordPayment(id));
    }
}
