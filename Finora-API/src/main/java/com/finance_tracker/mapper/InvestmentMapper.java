package com.finance_tracker.mapper;

import com.finance_tracker.dto.InvestmentRequestDTO;
import com.finance_tracker.dto.InvestmentResponseDTO;
import com.finance_tracker.utils.factory.InvestmentFactory;
import com.finance_tracker.model.Investment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InvestmentMapper {

    private final InvestmentFactory investmentFactory;

    public InvestmentMapper(InvestmentFactory investmentFactory) {
        this.investmentFactory = investmentFactory;
    }

    public Investment toEntity(InvestmentRequestDTO dto) {
        return investmentFactory.createInvestment(dto);
    }

    public InvestmentResponseDTO toDTO(Investment investment) {
        return InvestmentResponseDTO.builder()
                .id(investment.getId())
                .name(investment.getName())
                .symbol(investment.getSymbol())
                .type(investment.getType())
                .quantity(investment.getQuantity())
                .purchasePrice(investment.getPurchasePrice())
                .currentPrice(investment.getCurrentPrice())
                .purchaseDate(investment.getPurchaseDate())
                .lastUpdated(investment.getLastUpdated())
                .currentValue(investment.getCurrentValue())
                .profitLoss(investment.getProfitLoss())
                .returnPercentage(investment.getReturnPercentage())
                .isin(investment.getIsin())
                .importSource(investment.getImportSource())
                .build();
    }

    public List<InvestmentResponseDTO> toDTOList(List<Investment> investments) {
        return investments.stream()
                .map(this::toDTO)
                .toList();
    }
}

