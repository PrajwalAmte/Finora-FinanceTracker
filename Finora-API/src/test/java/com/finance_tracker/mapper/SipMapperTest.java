package com.finance_tracker.mapper;

import com.finance_tracker.dto.SipRequestDTO;
import com.finance_tracker.dto.SipResponseDTO;
import com.finance_tracker.model.Investment;
import com.finance_tracker.model.InvestmentType;
import com.finance_tracker.model.Sip;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SipMapperTest {

    private final SipMapper mapper = new SipMapper();

    private SipRequestDTO request() {
        SipRequestDTO dto = new SipRequestDTO();
        dto.setName("HDFC Flexi Cap");
        dto.setSchemeCode("118989");
        dto.setMonthlyAmount(new BigDecimal("5000.00"));
        dto.setStartDate(LocalDate.of(2023, 6, 1));
        dto.setDurationMonths(60);
        dto.setCurrentNav(new BigDecimal("120.50"));
        dto.setTotalUnits(new BigDecimal("100.00"));
        dto.setTotalInvested(new BigDecimal("60000.00"));
        return dto;
    }

    private Sip sip(Long id) {
        Sip s = new Sip();
        s.setId(id);
        s.setName("Axis Bluechip");
        s.setSchemeCode("120503");
        s.setMonthlyAmount(new BigDecimal("2000.00"));
        s.setStartDate(LocalDate.of(2022, 1, 1));
        s.setDurationMonths(120);
        s.setCurrentNav(new BigDecimal("50.00"));
        s.setTotalUnits(new BigDecimal("200.00"));
        s.setTotalInvested(new BigDecimal("40000.00"));
        return s;
    }

    @Test
    void toEntity_mapsAllFields() {
        Sip entity = mapper.toEntity(request());

        assertThat(entity.getName()).isEqualTo("HDFC Flexi Cap");
        assertThat(entity.getSchemeCode()).isEqualTo("118989");
        assertThat(entity.getMonthlyAmount()).isEqualByComparingTo("5000.00");
        assertThat(entity.getDurationMonths()).isEqualTo(60);
        assertThat(entity.getCurrentNav()).isEqualByComparingTo("120.50");
        assertThat(entity.getTotalInvested()).isEqualByComparingTo("60000.00");
    }

    @Test
    void toEntity_defaultsDurationTo120WhenNull() {
        SipRequestDTO dto = new SipRequestDTO();
        dto.setName("Test");
        dto.setMonthlyAmount(new BigDecimal("1000"));
        dto.setDurationMonths(null);

        Sip entity = mapper.toEntity(dto);
        assertThat(entity.getDurationMonths()).isEqualTo(120);
    }

    @Test
    void toDTO_withNoLinkedInvestment_usesSipOwnFields() {
        Sip s = sip(3L);
        SipResponseDTO dto = mapper.toDTO(s, null);

        assertThat(dto.getId()).isEqualTo(3L);
        assertThat(dto.getCurrentNav()).isEqualByComparingTo("50.00");
        assertThat(dto.getTotalInvested()).isEqualByComparingTo("40000.00");
        assertThat(dto.getCurrentValue()).isEqualByComparingTo("10000.00"); // 200 * 50
        assertThat(dto.getProfitLoss()).isEqualByComparingTo("-30000.00"); // 10000 - 40000
    }

    @Test
    void toDTO_withLinkedInvestment_usesInvestmentPrices() {
        Sip s = sip(4L);

        Investment inv = new Investment();
        inv.setId(99L);
        inv.setCurrentPrice(new BigDecimal("75.00"));
        inv.setQuantity(new BigDecimal("300.00"));
        inv.setPurchasePrice(new BigDecimal("60.00"));
        inv.setType(InvestmentType.MUTUAL_FUND);

        SipResponseDTO dto = mapper.toDTO(s, inv);

        assertThat(dto.getCurrentNav()).isEqualByComparingTo("75.00");
        assertThat(dto.getTotalUnits()).isEqualByComparingTo("300.00");
        assertThat(dto.getCurrentValue()).isEqualByComparingTo("22500.00"); // 300 * 75
        assertThat(dto.getTotalInvested()).isEqualByComparingTo("18000.00"); // 300 * 60
    }

    @Test
    void toDTOList_mapsWithLinkedMap() {
        Sip s1 = sip(1L);
        Sip s2 = sip(2L);
        List<SipResponseDTO> list = mapper.toDTOList(List.of(s1, s2), new HashMap<>());

        assertThat(list).hasSize(2);
    }
}
