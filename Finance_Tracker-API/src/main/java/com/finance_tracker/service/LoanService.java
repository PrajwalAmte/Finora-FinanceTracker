package com.finance_tracker.service;

import com.finance_tracker.model.Loan;
import com.finance_tracker.repository.LoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LoanService {
    private final LoanRepository loanRepository;

    public List<Loan> getAllLoans() {
        return loanRepository.findAll();
    }

    public Optional<Loan> getLoanById(Long id) {
        return loanRepository.findById(id);
    }

    public Loan saveLoan(Loan loan) {
        if (loan.getLastUpdated() == null) {
            loan.setLastUpdated(LocalDate.now());
        }

        // If this is a new loan and EMI is not set, calculate it
        if (loan.getId() == null && loan.getEmiAmount() == null &&
                loan.getPrincipalAmount() != null && loan.getInterestRate() != null &&
                loan.getTenureMonths() != null) {

            loan.setEmiAmount(calculateEmi(loan));

            // Set initial current balance to principal amount
            if (loan.getCurrentBalance() == null) {
                loan.setCurrentBalance(loan.getPrincipalAmount());
            }
        }

        return loanRepository.save(loan);
    }

    public void deleteLoan(Long id) {
        loanRepository.deleteById(id);
    }

    public BigDecimal getTotalLoanBalance() {
        return getAllLoans().stream()
                .map(Loan::getCurrentBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Calculate EMI using the formula: P * r * (1+r)^n / ((1+r)^n - 1)
    private BigDecimal calculateEmi(Loan loan) {
        BigDecimal principal = loan.getPrincipalAmount();
        BigDecimal annualRate = loan.getInterestRate().divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP);
        BigDecimal monthlyRate = annualRate.divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP);
        int tenure = loan.getTenureMonths();

        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal powValue = onePlusR.pow(tenure, new MathContext(10));

        BigDecimal numerator = principal.multiply(monthlyRate).multiply(powValue);
        BigDecimal denominator = powValue.subtract(BigDecimal.ONE);

        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    // Method to update loan balances (would be called by the scheduler)
    @Transactional
    public void updateLoanBalances() {
        LocalDate today = LocalDate.now();
        List<Loan> loans = getAllLoans();

        for (Loan loan : loans) {
            // Skip if already updated today
            if (loan.getLastUpdated() != null && loan.getLastUpdated().equals(today)) {
                continue;
            }

            LocalDate lastUpdated = loan.getLastUpdated() != null ? loan.getLastUpdated() : loan.getStartDate();
            if (lastUpdated == null) {
                continue; // Skip if no reference date available
            }

            // Calculate months passed since last update
            long monthsPassed = lastUpdated.until(today, ChronoUnit.MONTHS);
            if (monthsPassed > 0) {
                // Recalculate current balance based on EMI payments
                BigDecimal currentBalance = loan.getCurrentBalance();
                BigDecimal emiAmount = loan.getEmiAmount();

                for (int i = 0; i < monthsPassed; i++) {
                    // Calculate interest for the month
                    BigDecimal monthlyInterest = calculateMonthlyInterest(loan, currentBalance);

                    // Apply EMI payment
                    BigDecimal principalPortion = emiAmount.subtract(monthlyInterest);
                    currentBalance = currentBalance.subtract(principalPortion);

                    // Ensure balance doesn't go below zero
                    if (currentBalance.compareTo(BigDecimal.ZERO) < 0) {
                        currentBalance = BigDecimal.ZERO;
                        break;
                    }
                }

                // Update loan with new balance
                loan.setCurrentBalance(currentBalance);
                loan.setLastUpdated(today);
                loanRepository.save(loan);
            }
        }
    }

    private BigDecimal calculateMonthlyInterest(Loan loan, BigDecimal currentBalance) {
        BigDecimal annualRate = loan.getInterestRate().divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP);
        BigDecimal monthlyRate;

        if (com.finance_tracker.model.LoanInterestType.SIMPLE.equals(loan.getInterestType())) {
            // For simple interest, just divide annual rate by 12
            monthlyRate = annualRate.divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP);
        } else {
            // For compound interest, use the effective monthly rate based on compounding frequency
            switch (loan.getCompoundingFrequency()) {
                case MONTHLY:
                    monthlyRate = annualRate.divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP);
                    break;
                case QUARTERLY:
                    // Convert quarterly rate to monthly
                    BigDecimal quarterlyRate = annualRate.divide(new BigDecimal("4"), 10, RoundingMode.HALF_UP);
                    BigDecimal effectiveQuarterlyRate = BigDecimal.ONE.add(quarterlyRate);
                    BigDecimal effectiveMonthlyRate = effectiveQuarterlyRate.pow(1, new MathContext(10))
                            .subtract(BigDecimal.ONE);
                    monthlyRate = effectiveMonthlyRate;
                    break;
                case YEARLY:
                    // Convert yearly rate to monthly
                    BigDecimal effectiveYearlyRate = BigDecimal.ONE.add(annualRate);
                    BigDecimal effectiveMonthlyRate2 = effectiveYearlyRate.pow(1, new MathContext(10))
                            .subtract(BigDecimal.ONE);
                    monthlyRate = effectiveMonthlyRate2.divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP);
                    break;
                default:
                    monthlyRate = annualRate.divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP);
            }
        }

        return currentBalance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
    }
}

