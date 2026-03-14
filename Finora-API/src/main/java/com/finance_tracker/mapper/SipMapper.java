package com.finance_tracker.mapper;

import com.finance_tracker.dto.SipRequestDTO;
import com.finance_tracker.dto.SipResponseDTO;
import com.finance_tracker.model.Sip;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SipMapper {

    public Sip toEntity(SipRequestDTO dto) {
        Sip sip = new Sip();
        sip.setName(dto.getName());
        sip.setSchemeCode(dto.getSchemeCode());
        sip.setMonthlyAmount(dto.getMonthlyAmount());
        sip.setStartDate(dto.getStartDate());
        // Default to 120 months (10 years) when not specified
        sip.setDurationMonths(dto.getDurationMonths() != null ? dto.getDurationMonths() : 120);
        if (dto.getCurrentNav() != null) sip.setCurrentNav(dto.getCurrentNav());
        if (dto.getTotalUnits() != null) sip.setTotalUnits(dto.getTotalUnits());
        sip.setIsin(dto.getIsin());
        sip.setImportSource(dto.getImportSource());
        return sip;
    }

    public SipResponseDTO toDTO(Sip sip) {
        return SipResponseDTO.builder()
                .id(sip.getId())
                .name(sip.getName())
                .schemeCode(sip.getSchemeCode())
                .monthlyAmount(sip.getMonthlyAmount())
                .startDate(sip.getStartDate())
                .durationMonths(sip.getDurationMonths())
                .currentNav(sip.getCurrentNav())
                .totalUnits(sip.getTotalUnits())
                .lastUpdated(sip.getLastUpdated())
                .lastInvestmentDate(sip.getLastInvestmentDate())
                .currentValue(sip.getCurrentValue())
                .completedInstallments(sip.getCompletedInstallments())
                .totalInvested(sip.getTotalInvested())
                .profitLoss(sip.getProfitLoss())
                .isin(sip.getIsin())
                .importSource(sip.getImportSource())
                .build();
    }

    public List<SipResponseDTO> toDTOList(List<Sip> sips) {
        return sips.stream()
                .map(this::toDTO)
                .toList();
    }
}

