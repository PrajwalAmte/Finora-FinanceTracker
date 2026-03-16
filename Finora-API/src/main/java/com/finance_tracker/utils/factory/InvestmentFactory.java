package com.finance_tracker.utils.factory;

import com.finance_tracker.dto.InvestmentRequestDTO;
import com.finance_tracker.model.Investment;
import com.finance_tracker.model.InvestmentType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class InvestmentFactory {

    public Investment createInvestment(InvestmentRequestDTO dto) {
        Investment investment = new Investment();
        investment.setName(dto.getName());
        investment.setSymbol(dto.getSymbol());
        investment.setType(dto.getType());
        investment.setQuantity(dto.getQuantity());
        investment.setPurchasePrice(dto.getPurchasePrice());
        investment.setPurchaseDate(dto.getPurchaseDate() != null ? dto.getPurchaseDate() : LocalDate.now());
        BigDecimal cp = dto.getCurrentPrice();
        investment.setCurrentPrice(
                cp != null && cp.compareTo(BigDecimal.ZERO) > 0 ? cp : dto.getPurchasePrice());
        return investment;
    }
}

