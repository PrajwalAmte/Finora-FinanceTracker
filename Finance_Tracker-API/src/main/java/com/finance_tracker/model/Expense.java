package com.finance_tracker.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "expenses")
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
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
}
