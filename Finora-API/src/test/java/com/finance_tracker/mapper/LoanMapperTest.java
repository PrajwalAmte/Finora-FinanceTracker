package com.finance_tracker.mapper;

import com.finance_tracker.dto.LoanRequestDTO;
import com.finance_tracker.dto.LoanResponseDTO;
import com.finance_tracker.model.CompoundingFrequency;
import com.finance_tracker.model.Loan;
import com.finance_tracker.model.LoanInterestType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LoanMapperTest {

    private final LoanMapper mapper = new LoanMapper();

    private LoanRequestDTO request() {
        LoanRequestDTO dto = new LoanRequestDTO();
        dto.setName("Home Loan");
        dto.setPrincipalAmount(new BigDecimal("500000.00"));
        dto.setInterestRate(new BigDecimal("8.5"));
        dto.setInterestType(LoanInterestType.COMPOUND);
        dto.setCompoundingFrequency(CompoundingFrequency.MONTHLY);
        dto.setStartDate(LocalDate.of(2023, 1, 1));
        dto.setTenureMonths(240);
        return dto;
    }

    private Loan loan(Long id) {
        Loan loan = new Loan();
        loan.setId(id);
        loan.setName("Car Loan");
        loan.setPrincipalAmount(new BigDecimal("300000.00"));
        loan.setInterestRate(new BigDecimal("9.0"));
        loan.setInterestType(LoanInterestType.SIMPLE);
        loan.setCompoundingFrequency(CompoundingFrequency.YEARLY);
        loan.setStartDate(LocalDate.of(2022, 6, 1));
        loan.setTenureMonths(60);
        loan.setCurrentBalance(new BigDecimal("250000.00"));
        return loan;
    }

    @Test
    void toEntity_mapsAllRequestFields() {
        Loan entity = mapper.toEntity(request());

        assertThat(entity.getName()).isEqualTo("Home Loan");
        assertThat(entity.getPrincipalAmount()).isEqualByComparingTo("500000.00");
        assertThat(entity.getInterestRate()).isEqualByComparingTo("8.5");
        assertThat(entity.getInterestType()).isEqualTo(LoanInterestType.COMPOUND);
        assertThat(entity.getCompoundingFrequency()).isEqualTo(CompoundingFrequency.MONTHLY);
        assertThat(entity.getTenureMonths()).isEqualTo(240);
    }

    @Test
    void toDTO_mapsAllLoanFields() {
        LoanResponseDTO dto = mapper.toDTO(loan(5L));

        assertThat(dto.getId()).isEqualTo(5L);
        assertThat(dto.getName()).isEqualTo("Car Loan");
        assertThat(dto.getPrincipalAmount()).isEqualByComparingTo("300000.00");
        assertThat(dto.getInterestType()).isEqualTo(LoanInterestType.SIMPLE);
        assertThat(dto.getCurrentBalance()).isEqualByComparingTo("250000.00");
    }

    @Test
    void toDTOList_mapsEachElement() {
        List<LoanResponseDTO> list = mapper.toDTOList(List.of(loan(1L), loan(2L)));
        assertThat(list).hasSize(2);
        assertThat(list.get(0).getId()).isEqualTo(1L);
    }
}
