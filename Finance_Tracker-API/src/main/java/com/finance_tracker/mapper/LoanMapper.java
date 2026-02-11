package com.finance_tracker.mapper;

import com.finance_tracker.dto.LoanRequestDTO;
import com.finance_tracker.dto.LoanResponseDTO;
import com.finance_tracker.model.Loan;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class LoanMapper {

    public Loan toEntity(LoanRequestDTO dto) {
        Loan loan = new Loan();
        loan.setName(dto.getName());
        loan.setPrincipalAmount(dto.getPrincipalAmount());
        loan.setInterestRate(dto.getInterestRate());
        loan.setInterestType(dto.getInterestType());
        loan.setCompoundingFrequency(dto.getCompoundingFrequency());
        loan.setStartDate(dto.getStartDate());
        loan.setTenureMonths(dto.getTenureMonths());
        return loan;
    }

    public LoanResponseDTO toDTO(Loan loan) {
        return LoanResponseDTO.builder()
                .id(loan.getId())
                .name(loan.getName())
                .principalAmount(loan.getPrincipalAmount())
                .interestRate(loan.getInterestRate())
                .interestType(loan.getInterestType())
                .compoundingFrequency(loan.getCompoundingFrequency())
                .startDate(loan.getStartDate())
                .tenureMonths(loan.getTenureMonths())
                .emiAmount(loan.getEmiAmount())
                .currentBalance(loan.getCurrentBalance())
                .lastUpdated(loan.getLastUpdated())
                .endDate(loan.getEndDate())
                .remainingMonths(loan.getRemainingMonths())
                .totalRepayment(loan.getTotalRepayment())
                .totalInterest(loan.getTotalInterest())
                .build();
    }

    public List<LoanResponseDTO> toDTOList(List<Loan> loans) {
        return loans.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}

