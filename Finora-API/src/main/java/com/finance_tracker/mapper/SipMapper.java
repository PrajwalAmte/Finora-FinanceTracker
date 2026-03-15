package com.finance_tracker.mapper;

import com.finance_tracker.dto.SipRequestDTO;
import com.finance_tracker.dto.SipResponseDTO;
import com.finance_tracker.model.Investment;
import com.finance_tracker.model.Sip;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Component
public class SipMapper {

    public Sip toEntity(SipRequestDTO dto) {
        Sip sip = new Sip();
        sip.setName(dto.getName());
        sip.setSchemeCode(dto.getSchemeCode());
        sip.setMonthlyAmount(dto.getMonthlyAmount());
        sip.setStartDate(dto.getStartDate());
        sip.setDurationMonths(dto.getDurationMonths() != null ? dto.getDurationMonths() : 120);
        if (dto.getCurrentNav() != null) sip.setCurrentNav(dto.getCurrentNav());
        if (dto.getTotalUnits() != null) sip.setTotalUnits(dto.getTotalUnits());
        if (dto.getTotalInvested() != null) sip.setTotalInvested(dto.getTotalInvested());
        sip.setIsin(dto.getIsin());
        sip.setImportSource(dto.getImportSource());
        sip.setInvestmentId(dto.getInvestmentId());
        return sip;
    }

    public SipResponseDTO toDTO(Sip sip, Investment linkedInvestment) {
        BigDecimal currentNav    = sip.getCurrentNav();
        BigDecimal totalUnits    = sip.getTotalUnits();
        BigDecimal totalInvested = sip.getTotalInvested();

        if (linkedInvestment != null) {
            currentNav    = linkedInvestment.getCurrentPrice();
            totalUnits    = linkedInvestment.getQuantity();
            totalInvested = linkedInvestment.getQuantity()
                    .multiply(linkedInvestment.getPurchasePrice())
                    .setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal currentValue = (totalUnits != null && currentNav != null)
                ? totalUnits.multiply(currentNav)
                : BigDecimal.ZERO;
        BigDecimal invested  = totalInvested != null ? totalInvested : BigDecimal.ZERO;
        BigDecimal profitLoss = currentValue.subtract(invested);

        return SipResponseDTO.builder()
                .id(sip.getId())
                .name(sip.getName())
                .schemeCode(sip.getSchemeCode())
                .monthlyAmount(sip.getMonthlyAmount())
                .startDate(sip.getStartDate())
                .durationMonths(sip.getDurationMonths())
                .currentNav(currentNav)
                .totalUnits(totalUnits)
                .lastUpdated(sip.getLastUpdated())
                .lastInvestmentDate(sip.getLastInvestmentDate())
                .currentValue(currentValue)
                .completedInstallments(sip.getCompletedInstallments())
                .totalInvested(invested)
                .profitLoss(profitLoss)
                .isin(sip.getIsin())
                .importSource(sip.getImportSource())
                .investmentId(sip.getInvestmentId())
                .build();
    }

    public SipResponseDTO toDTO(Sip sip) {
        return toDTO(sip, null);
    }

    public List<SipResponseDTO> toDTOList(List<Sip> sips, Map<Long, Investment> linkedInvestments) {
        return sips.stream()
                .map(sip -> toDTO(sip, linkedInvestments.get(sip.getInvestmentId())))
                .toList();
    }

    public List<SipResponseDTO> toDTOList(List<Sip> sips) {
        return sips.stream()
                .map(this::toDTO)
                .toList();
    }
}

