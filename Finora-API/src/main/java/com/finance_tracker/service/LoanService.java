package com.finance_tracker.service;

import com.finance_tracker.model.Loan;
import com.finance_tracker.repository.LoanRepository;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final LedgerService ledgerService;

    private Long resolveUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        try {
            return Long.parseLong(auth.getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void validateOwnership(Long resourceUserId, Long requestingUserId) {
        if (resourceUserId != null && requestingUserId != null
                && !resourceUserId.equals(requestingUserId)) {
            throw new com.finance_tracker.exception.ResourceNotFoundException("Loan not found");
        }
    }

    public List<Loan> getAllLoans() {
        Long userId = resolveUserId();
        return loanRepository.findByUserId(userId);
    }

    public Loan getLoanById(Long id) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new com.finance_tracker.exception.ResourceNotFoundException("Loan", id));
        validateOwnership(loan.getUserId(), resolveUserId());
        return loan;
    }

    public Optional<Loan> findLoanById(Long id) {
        return loanRepository.findById(id);
    }

    public Loan saveLoan(Loan loan) {
        Long userId = resolveUserId();
        if (loan.getLastUpdated() == null) {
            loan.setLastUpdated(LocalDate.now());
        }

        if (loan.getId() == null && loan.getEmiAmount() == null &&
                loan.getPrincipalAmount() != null && loan.getInterestRate() != null &&
                loan.getTenureMonths() != null) {

            loan.setEmiAmount(calculateEmi(loan));

            if (loan.getCurrentBalance() == null) {
                loan.setCurrentBalance(loan.getPrincipalAmount());
            }
        }

        if (loan.getId() != null) {
            Loan before = loanRepository.findById(loan.getId()).orElse(null);
            if (before != null) validateOwnership(before.getUserId(), userId);
            loan.setUserId(userId);
            Loan saved = loanRepository.save(loan);
            ledgerService.recordEvent("LOAN", String.valueOf(saved.getId()), "UPDATE", before, saved, String.valueOf(userId));
            return saved;
        }
        loan.setUserId(userId);
        Loan saved = loanRepository.save(loan);
        ledgerService.recordEvent("LOAN", String.valueOf(saved.getId()), "CREATE", null, saved, String.valueOf(userId));
        return saved;
    }

    public void deleteLoan(Long id) {
        Long userId = resolveUserId();
        Loan before = loanRepository.findById(id)
                .orElseThrow(() -> new com.finance_tracker.exception.ResourceNotFoundException("Loan", id));
        validateOwnership(before.getUserId(), userId);
        loanRepository.deleteById(id);
        ledgerService.recordEvent("LOAN", String.valueOf(id), "DELETE", before, null, String.valueOf(userId));
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

    @Transactional
    public void updateLoanBalances() {
        LocalDate today = LocalDate.now();
        List<Loan> loans = loanRepository.findAll(); // system-level: all users

        for (Loan loan : loans) {

            if (loan.getLastUpdated() != null && loan.getLastUpdated().equals(today)) {
                continue;
            }

            LocalDate lastUpdated = loan.getLastUpdated() != null ? loan.getLastUpdated() : loan.getStartDate();
            if (lastUpdated == null) {
                continue;
            }

            long monthsPassed = lastUpdated.until(today, ChronoUnit.MONTHS);
            if (monthsPassed > 0) {

                BigDecimal currentBalance = loan.getCurrentBalance();
                BigDecimal emiAmount = loan.getEmiAmount();

                for (int i = 0; i < monthsPassed; i++) {
                    BigDecimal monthlyInterest = calculateMonthlyInterest(loan, currentBalance);
                    BigDecimal principalPortion = emiAmount.subtract(monthlyInterest);
                    currentBalance = currentBalance.subtract(principalPortion);

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
            monthlyRate = annualRate.divide(new BigDecimal("12"), 10, RoundingMode.HALF_UP);
        } else {
            switch (loan.getCompoundingFrequency()) {
                case QUARTERLY:
                    BigDecimal quarterlyRate = annualRate.divide(new BigDecimal("4"), 10, RoundingMode.HALF_UP);
                    BigDecimal effectiveQuarterlyRate = BigDecimal.ONE.add(quarterlyRate);
                    monthlyRate = effectiveQuarterlyRate.pow(1, new MathContext(10))
                            .subtract(BigDecimal.ONE);
                    break;
                case YEARLY:
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

