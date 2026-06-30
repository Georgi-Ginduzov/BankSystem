package com.banksystem.service;

import com.banksystem.model.Loan;
import com.banksystem.model.Repayment;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class RepaymentPlanService {

    public List<Repayment> generateRepaymentSchedule(Loan loan) {
        List<Repayment> schedule = new ArrayList<>();

        BigDecimal monthlyRate = getMonthlyRate(loan);
        BigDecimal remainingPrincipal = normalizeCurrency(loan.getInitialAmount());
        BigDecimal monthlyPayment = normalizeCurrency(loan.getMonthlyPayment());
        int totalMonths = loan.getTermMonths();

        for (int month = 1; month <= totalMonths; month++) {
            BigDecimal interestPortion = remainingPrincipal
                    .multiply(monthlyRate)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal principalPortion = monthlyPayment.subtract(interestPortion);

            if (month == totalMonths || principalPortion.compareTo(remainingPrincipal) >= 0) {
                principalPortion = remainingPrincipal;
                monthlyPayment = principalPortion.add(interestPortion).setScale(2, RoundingMode.HALF_UP);
            }

            remainingPrincipal = normalizeCurrency(remainingPrincipal.subtract(principalPortion));
            BigDecimal remaining = month == totalMonths ? BigDecimal.ZERO : remainingPrincipal;

            schedule.add(buildPendingRepayment(
                    loan,
                    month,
                    loan.getStartDate().plusMonths(month),
                    monthlyPayment,
                    interestPortion,
                    principalPortion,
                    remaining
            ));
        }

        return schedule;
    }

    public BigDecimal calculateMonthlyPayment(BigDecimal amount, Integer termMonths, BigDecimal annualInterestRate) {
        if (annualInterestRate.compareTo(BigDecimal.ZERO) == 0) {
            return normalizeCurrency(amount.divide(
                    BigDecimal.valueOf(termMonths),
                    2,
                    RoundingMode.HALF_UP
            ));
        }

        double r = annualInterestRate.doubleValue() / 12.0 / 100.0;
        double n = termMonths;
        double p = amount.doubleValue();
        double pmt = p * r / (1 - Math.pow(1 + r, -n));

        return normalizeCurrency(BigDecimal.valueOf(pmt));
    }

    public BigDecimal getMonthlyRate(Loan loan) {
        return loan.getLoanType()
                .getInterestRate()
                .divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
    }

    public BigDecimal normalizeCurrency(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal normalized = value.setScale(2, RoundingMode.HALF_UP);
        return normalized.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : normalized;
    }

    public Repayment buildPendingRepayment(
            Loan loan,
            int monthNumber,
            LocalDate dueDate,
            BigDecimal paymentAmount,
            BigDecimal interestAmount,
            BigDecimal principalAmount,
            BigDecimal remainingAmount
    ) {
        return Repayment.builder()
                .loan(loan)
                .monthNumber(monthNumber)
                .dueDate(dueDate)
                .status(Repayment.RepaymentStatus.PENDING)
                .expectedPaymentAmount(normalizeCurrency(paymentAmount))
                .expectedInterestAmount(normalizeCurrency(interestAmount))
                .expectedPrincipalAmount(normalizeCurrency(principalAmount))
                .expectedRemainingToPay(normalizeCurrency(remainingAmount))
                .build();
    }
}
