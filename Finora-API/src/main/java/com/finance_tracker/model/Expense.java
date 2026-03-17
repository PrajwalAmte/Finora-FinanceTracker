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

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "expenses")
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Convert(converter = EncryptedStringConverter.class)
    private String description;

    @Digits(integer = 17, fraction = 2)
    @DecimalMin(value = "0.00", inclusive = false)
    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    @NotNull
    private LocalDate date;

    @NotBlank
    private String category;

    @NotBlank
    private String paymentMethod;

    @Column(name = "user_id")
    private Long userId;
}
