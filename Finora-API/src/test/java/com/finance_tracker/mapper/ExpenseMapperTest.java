package com.finance_tracker.mapper;

import com.finance_tracker.dto.ExpenseRequestDTO;
import com.finance_tracker.dto.ExpenseResponseDTO;
import com.finance_tracker.model.Expense;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExpenseMapperTest {

    private final ExpenseMapper mapper = new ExpenseMapper();

    private ExpenseRequestDTO request(String desc, String category) {
        ExpenseRequestDTO dto = new ExpenseRequestDTO();
        dto.setDescription(desc);
        dto.setAmount(new BigDecimal("100.00"));
        dto.setDate(LocalDate.of(2024, 1, 15));
        dto.setCategory(category);
        dto.setPaymentMethod("UPI");
        return dto;
    }

    private Expense expense(Long id) {
        Expense e = new Expense();
        e.setId(id);
        e.setDescription("Coffee");
        e.setAmount(new BigDecimal("50.00"));
        e.setDate(LocalDate.of(2024, 3, 1));
        e.setCategory("Food");
        e.setPaymentMethod("Cash");
        return e;
    }

    @Test
    void toEntity_mapsAllFields() {
        Expense entity = mapper.toEntity(request("Lunch", "Food"));

        assertThat(entity.getDescription()).isEqualTo("Lunch");
        assertThat(entity.getAmount()).isEqualByComparingTo("100.00");
        assertThat(entity.getDate()).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(entity.getCategory()).isEqualTo("Food");
        assertThat(entity.getPaymentMethod()).isEqualTo("UPI");
    }

    @Test
    void toDTO_mapsAllFields() {
        ExpenseResponseDTO dto = mapper.toDTO(expense(1L));

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getDescription()).isEqualTo("Coffee");
        assertThat(dto.getAmount()).isEqualByComparingTo("50.00");
        assertThat(dto.getCategory()).isEqualTo("Food");
        assertThat(dto.getPaymentMethod()).isEqualTo("Cash");
    }

    @Test
    void toDTOList_mapsEachElement() {
        List<ExpenseResponseDTO> list = mapper.toDTOList(List.of(expense(1L), expense(2L)));
        assertThat(list).hasSize(2);
        assertThat(list.get(0).getId()).isEqualTo(1L);
        assertThat(list.get(1).getId()).isEqualTo(2L);
    }
}
