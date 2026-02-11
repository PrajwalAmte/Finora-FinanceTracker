package com.finance_tracker.mapper;

import com.finance_tracker.dto.ExpenseRequestDTO;
import com.finance_tracker.dto.ExpenseResponseDTO;
import com.finance_tracker.model.Expense;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ExpenseMapper {

    public Expense toEntity(ExpenseRequestDTO dto) {
        Expense expense = new Expense();
        expense.setDescription(dto.getDescription());
        expense.setAmount(dto.getAmount());
        expense.setDate(dto.getDate());
        expense.setCategory(dto.getCategory());
        expense.setPaymentMethod(dto.getPaymentMethod());
        return expense;
    }

    public ExpenseResponseDTO toDTO(Expense expense) {
        return ExpenseResponseDTO.builder()
                .id(expense.getId())
                .description(expense.getDescription())
                .amount(expense.getAmount())
                .date(expense.getDate())
                .category(expense.getCategory())
                .paymentMethod(expense.getPaymentMethod())
                .build();
    }

    public List<ExpenseResponseDTO> toDTOList(List<Expense> expenses) {
        return expenses.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}

