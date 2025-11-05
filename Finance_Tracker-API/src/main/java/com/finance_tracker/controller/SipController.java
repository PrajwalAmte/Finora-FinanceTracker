package com.finance_tracker.controller;

import com.finance_tracker.model.Sip;
import com.finance_tracker.service.SipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sips")
@RequiredArgsConstructor
public class SipController {
    private final SipService sipService;

    @GetMapping
    public List<Sip> getAllSips() {
        return sipService.getAllSips();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Sip> getSipById(@PathVariable Long id) {
        return sipService.getSipById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Sip createSip(@Valid @RequestBody Sip sip) {
        return sipService.saveSip(sip);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Sip> updateSip(@PathVariable Long id, @Valid @RequestBody Sip sip) {
        return sipService.getSipById(id)
                .map(existingSip -> {
                    sip.setId(id);
                    return ResponseEntity.ok(sipService.saveSip(sip));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSip(@PathVariable Long id) {
        return sipService.getSipById(id)
                .map(sip -> {
                    sipService.deleteSip(id);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/summary")
    public ResponseEntity<Object> getSipSummary() {
        BigDecimal totalValue = sipService.getTotalSipValue();
        BigDecimal totalInvestment = sipService.getTotalSipInvestment();
        BigDecimal totalProfitLoss = totalValue.subtract(totalInvestment);

        return ResponseEntity.ok(Map.of(
                "totalInvestment", totalInvestment,
                "totalCurrentValue", totalValue,
                "totalProfitLoss", totalProfitLoss
        ));
    }
}
