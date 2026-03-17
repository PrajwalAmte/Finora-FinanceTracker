package com.finance_tracker.utils.factory;

import com.finance_tracker.dto.InvestmentRequestDTO;
import com.finance_tracker.model.Investment;
import com.finance_tracker.model.InvestmentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class InvestmentFactoryTest {

    private final InvestmentFactory factory = new InvestmentFactory();

    private InvestmentRequestDTO dto(BigDecimal currentPrice, LocalDate purchaseDate) {
        InvestmentRequestDTO dto = new InvestmentRequestDTO();
        dto.setName("Infosys");
        dto.setSymbol("INFY.NS");
        dto.setType(InvestmentType.STOCK);
        dto.setQuantity(new BigDecimal("5"));
        dto.setPurchasePrice(new BigDecimal("1500.00"));
        dto.setCurrentPrice(currentPrice);
        dto.setPurchaseDate(purchaseDate);
        return dto;
    }

    @Test
    void createInvestment_mapsAllFields() {
        LocalDate date = LocalDate.of(2023, 3, 15);
        Investment inv = factory.createInvestment(dto(new BigDecimal("1800.00"), date));

        assertThat(inv.getName()).isEqualTo("Infosys");
        assertThat(inv.getSymbol()).isEqualTo("INFY.NS");
        assertThat(inv.getType()).isEqualTo(InvestmentType.STOCK);
        assertThat(inv.getQuantity()).isEqualByComparingTo("5");
        assertThat(inv.getPurchasePrice()).isEqualByComparingTo("1500.00");
        assertThat(inv.getCurrentPrice()).isEqualByComparingTo("1800.00");
        assertThat(inv.getPurchaseDate()).isEqualTo(date);
    }

    @Test
    void createInvestment_nullCurrentPriceFallsBackToPurchasePrice() {
        Investment inv = factory.createInvestment(dto(null, LocalDate.now()));
        assertThat(inv.getCurrentPrice()).isEqualByComparingTo("1500.00");
    }

    @Test
    void createInvestment_zeroCurrrentPriceFallsBackToPurchasePrice() {
        Investment inv = factory.createInvestment(dto(BigDecimal.ZERO, LocalDate.now()));
        assertThat(inv.getCurrentPrice()).isEqualByComparingTo("1500.00");
    }

    @Test
    void createInvestment_nullPurchaseDateDefaultsToToday() {
        Investment inv = factory.createInvestment(dto(new BigDecimal("1600"), null));
        assertThat(inv.getPurchaseDate()).isEqualTo(LocalDate.now());
    }
}
