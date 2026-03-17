package com.finance_tracker.mapper;

import com.finance_tracker.dto.InvestmentRequestDTO;
import com.finance_tracker.dto.InvestmentResponseDTO;
import com.finance_tracker.model.Investment;
import com.finance_tracker.model.InvestmentType;
import com.finance_tracker.utils.factory.InvestmentFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvestmentMapperTest {

    @Mock
    private InvestmentFactory investmentFactory;

    @InjectMocks
    private InvestmentMapper mapper;

    private Investment investment(Long id) {
        Investment inv = new Investment();
        inv.setId(id);
        inv.setName("Reliance Industries");
        inv.setSymbol("RELIANCE.NS");
        inv.setType(InvestmentType.STOCK);
        inv.setQuantity(new BigDecimal("10"));
        inv.setPurchasePrice(new BigDecimal("2000.00"));
        inv.setCurrentPrice(new BigDecimal("2500.00"));
        inv.setPurchaseDate(LocalDate.of(2023, 5, 1));
        return inv;
    }

    @Test
    void toEntity_delegatesToFactory() {
        InvestmentRequestDTO dto = new InvestmentRequestDTO();
        dto.setName("TCS");
        Investment expected = new Investment();
        when(investmentFactory.createInvestment(dto)).thenReturn(expected);

        Investment result = mapper.toEntity(dto);
        assertThat(result).isSameAs(expected);
    }

    @Test
    void toDTO_mapsAllFields() {
        Investment inv = investment(7L);
        InvestmentResponseDTO dto = mapper.toDTO(inv);

        assertThat(dto.getId()).isEqualTo(7L);
        assertThat(dto.getName()).isEqualTo("Reliance Industries");
        assertThat(dto.getSymbol()).isEqualTo("RELIANCE.NS");
        assertThat(dto.getType()).isEqualTo(InvestmentType.STOCK);
        assertThat(dto.getQuantity()).isEqualByComparingTo("10");
        assertThat(dto.getPurchasePrice()).isEqualByComparingTo("2000.00");
        assertThat(dto.getCurrentPrice()).isEqualByComparingTo("2500.00");
    }

    @Test
    void toDTO_exposesComputedFields() {
        Investment inv = investment(8L);
        InvestmentResponseDTO dto = mapper.toDTO(inv);

        assertThat(dto.getCurrentValue()).isEqualByComparingTo("25000.00"); // 10 * 2500
        assertThat(dto.getProfitLoss()).isEqualByComparingTo("5000.00");    // 25000 - 20000
    }

    @Test
    void toDTOList_mapsEachElement() {
        List<InvestmentResponseDTO> list = mapper.toDTOList(List.of(investment(1L), investment(2L)));
        assertThat(list).hasSize(2);
        assertThat(list.get(0).getId()).isEqualTo(1L);
    }
}
