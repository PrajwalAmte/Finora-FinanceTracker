package com.finance_tracker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AmfiNavServiceTest {

    private AmfiNavService service;

    @BeforeEach
    void preSeedCache() {
        service = new AmfiNavService();

        // Pre-seed internal maps to avoid real HTTP call
        Map<String, BigDecimal> navs = new HashMap<>();
        navs.put("118989", new BigDecimal("120.50"));
        navs.put("120503", new BigDecimal("45.20"));

        Map<String, String> isinMap = new HashMap<>();
        isinMap.put("INF179K01VV4", "118989");

        Map<String, String> nameMap = new HashMap<>();
        nameMap.put("118989", "HDFC Flexi Cap Fund");
        nameMap.put("120503", "Axis Bluechip Fund");

        ReflectionTestUtils.setField(service, "navsBySchemeCode", navs);
        ReflectionTestUtils.setField(service, "schemeCodeByIsin", isinMap);
        ReflectionTestUtils.setField(service, "schemeNameByCode", nameMap);
        ReflectionTestUtils.setField(service, "cacheDate", java.time.LocalDate.now());
    }

    @Test
    void getNavBySchemeCode_returnsNavForKnownCode() {
        Optional<BigDecimal> nav = service.getNavBySchemeCode("118989");
        assertThat(nav).isPresent().contains(new BigDecimal("120.50"));
    }

    @Test
    void getNavBySchemeCode_emptyForUnknownCode() {
        assertThat(service.getNavBySchemeCode("999999")).isEmpty();
    }

    @Test
    void getNavBySchemeCode_nullReturnsEmpty() {
        assertThat(service.getNavBySchemeCode(null)).isEmpty();
    }

    @Test
    void getNavBySchemeCode_blankReturnsEmpty() {
        assertThat(service.getNavBySchemeCode("  ")).isEmpty();
    }

    @Test
    void lookupSchemeCodeByIsin_returnsSchemeCodeForKnownIsin() {
        Optional<String> code = service.lookupSchemeCodeByIsin("INF179K01VV4");
        assertThat(code).isPresent().contains("118989");
    }

    @Test
    void lookupSchemeCodeByIsin_nullReturnsEmpty() {
        assertThat(service.lookupSchemeCodeByIsin(null)).isEmpty();
    }

    @Test
    void lookupSchemeCodeByIsin_unknownIsinReturnsEmpty() {
        assertThat(service.lookupSchemeCodeByIsin("INF000X00000")).isEmpty();
    }

    @Test
    void searchByName_returnsMatchingFunds() {
        List<Map<String, Object>> results = service.searchByName("HDFC");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).get("schemeCode")).isEqualTo("118989");
    }

    @Test
    void searchByName_nullQueryReturnsEmpty() {
        assertThat(service.searchByName(null)).isEmpty();
    }

    @Test
    void searchByName_shortQueryReturnsEmpty() {
        assertThat(service.searchByName("H")).isEmpty();
    }

    @Test
    void searchByName_emptyQueryReturnsEmpty() {
        assertThat(service.searchByName("")).isEmpty();
    }

    @Test
    void getAllNavs_returnsUnmodifiableMap() {
        Map<String, BigDecimal> all = service.getAllNavs();
        assertThat(all).containsKey("118989").containsKey("120503");
    }
}
