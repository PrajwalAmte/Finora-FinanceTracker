package com.finance_tracker.model;

import com.finance_tracker.utils.converter.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "sips")
public class Sip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Convert(converter = EncryptedStringConverter.class)
    private String name;

    private String schemeCode;


    @Digits(integer = 17, fraction = 2)
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(precision = 19, scale = 2)
    private BigDecimal monthlyAmount;
    private LocalDate startDate;
    private Integer durationMonths;

    @Digits(integer = 13, fraction = 6)
    @Column(precision = 19, scale = 6)
    private BigDecimal currentNav;

    @Digits(integer = 16, fraction = 8)
    @Column(precision = 24, scale = 8)
    private BigDecimal totalUnits;
    private LocalDate lastUpdated;
    private LocalDate lastInvestmentDate;

    @Column(name = "total_invested", precision = 19, scale = 2)
    private BigDecimal totalInvested;

    @Column(name = "user_id")
    private Long userId;

    private String isin;

    @Column(name = "import_source")
    private String importSource;

    @Column(name = "investment_id")
    private Long investmentId;

    public BigDecimal getCurrentValue() {
        if (totalUnits == null || currentNav == null) return BigDecimal.ZERO;
        return totalUnits.multiply(currentNav);
    }

    public BigDecimal getTotalInvested() {
        return totalInvested != null ? totalInvested : BigDecimal.ZERO;
    }

    public Integer getCompletedInstallments() {
        if (monthlyAmount == null || monthlyAmount.compareTo(BigDecimal.ZERO) == 0) return 0;
        return getTotalInvested().divide(monthlyAmount, 0, java.math.RoundingMode.HALF_UP).intValue();
    }

    public BigDecimal getProfitLoss() {
        return getCurrentValue().subtract(getTotalInvested());
    }
}
