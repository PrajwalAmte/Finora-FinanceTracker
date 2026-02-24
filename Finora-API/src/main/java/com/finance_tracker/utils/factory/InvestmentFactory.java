package com.finance_tracker.utils.factory;

import com.finance_tracker.dto.InvestmentRequestDTO;
import com.finance_tracker.model.Investment;
import com.finance_tracker.model.InvestmentType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Factory for creating Investment entities based on investment type.
 * Handles type-specific initialization logic.
 */
@Component
public class InvestmentFactory {
    
    /**
     * Creates an Investment entity from a DTO with type-specific defaults
     * 
     * @param dto The investment request DTO
     * @return A new Investment entity with appropriate defaults
     */
    public Investment createInvestment(InvestmentRequestDTO dto) {
        Investment investment = new Investment();
        investment.setName(dto.getName());
        investment.setSymbol(dto.getSymbol());
        investment.setType(dto.getType());
        investment.setQuantity(dto.getQuantity());
        investment.setPurchasePrice(dto.getPurchasePrice());
        investment.setPurchaseDate(dto.getPurchaseDate() != null ? dto.getPurchaseDate() : LocalDate.now());
        
        // Type-specific initialization
        applyTypeSpecificDefaults(investment, dto.getType());
        
        return investment;
    }
    
    /**
     * Applies type-specific defaults based on investment type
     * 
     * @param investment The investment to configure
     * @param type The investment type
     */
    private void applyTypeSpecificDefaults(Investment investment, InvestmentType type) {
        switch (type) {
            case STOCK:
                // Stocks typically start with current price = purchase price
                if (investment.getCurrentPrice() == null) {
                    investment.setCurrentPrice(investment.getPurchasePrice());
                }
                break;
            case MUTUAL_FUND:
                // Mutual funds may have different handling
                if (investment.getCurrentPrice() == null) {
                    investment.setCurrentPrice(investment.getPurchasePrice());
                }
                break;
            case BOND:
                // Bonds might have different initialization
                if (investment.getCurrentPrice() == null) {
                    investment.setCurrentPrice(investment.getPurchasePrice());
                }
                break;
            default:
                // Default behavior
                if (investment.getCurrentPrice() == null) {
                    investment.setCurrentPrice(investment.getPurchasePrice());
                }
                break;
        }
    }

    public Investment createDefaultInvestment(String name, String symbol, InvestmentType type) {
        Investment investment = new Investment();
        investment.setName(name);
        investment.setSymbol(symbol);
        investment.setType(type);
        investment.setPurchaseDate(LocalDate.now());
        
        applyTypeSpecificDefaults(investment, type);
        
        return investment;
    }
}

