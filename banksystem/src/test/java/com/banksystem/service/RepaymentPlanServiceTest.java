package com.banksystem.service;

import com.banksystem.model.Account;
import com.banksystem.model.Customer;
import com.banksystem.model.Loan;
import com.banksystem.model.LoanType;
import com.banksystem.model.Repayment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RepaymentPlanServiceTest {

    private RepaymentPlanService repaymentPlanService;
    private Loan loan;

    @BeforeEach
    void setUp() {
        repaymentPlanService = new RepaymentPlanService();

        LoanType loanType = LoanType.builder()
                .id(1)
                .name("CONSUMER")
                .interestRate(new BigDecimal("6.00"))
                .maxAmount(new BigDecimal("50000.00"))
                .maxTermMonths(60)
                .build();

        loan = Loan.builder()
                .id(10)
                .client(new Customer("1234567890", "Ivan", "Petrov"))
                .account(new Account("1234567890", "BG80BANK123456789012345678", BigDecimal.ZERO, Account.AccountType.LOAN))
                .loanType(loanType)
                .initialAmount(new BigDecimal("12000.00"))
                .remainingAmount(new BigDecimal("12000.00"))
                .monthlyPayment(repaymentPlanService.calculateMonthlyPayment(
                        new BigDecimal("12000.00"),
                        12,
                        loanType.getInterestRate()
                ))
                .termMonths(12)
                .startDate(LocalDate.of(2026, 6, 1))
                .status(Loan.LoanStatus.ACTIVE)
                .paidInstallments(0)
                .build();
    }

    @Test
    void generateRepaymentSchedule_createsInstallmentForEachMonth() {
        List<Repayment> schedule = repaymentPlanService.generateRepaymentSchedule(loan);

        assertThat(schedule).hasSize(12);
        assertThat(schedule.get(0).getMonthNumber()).isEqualTo(1);
        assertThat(schedule.get(11).getMonthNumber()).isEqualTo(12);
    }

    @Test
    void generateRepaymentSchedule_lastInstallmentLeavesZeroRemainingAmount() {
        List<Repayment> schedule = repaymentPlanService.generateRepaymentSchedule(loan);

        assertThat(schedule.get(schedule.size() - 1).getExpectedRemainingToPay())
                .isEqualByComparingTo("0.00");
    }

    @Test
    void generateRepaymentSchedule_expectedAmountsStayPositive() {
        List<Repayment> schedule = repaymentPlanService.generateRepaymentSchedule(loan);

        assertThat(schedule)
                .allSatisfy(item -> {
                    assertThat(item.getExpectedPaymentAmount()).isGreaterThan(BigDecimal.ZERO);
                    assertThat(item.getExpectedPrincipalAmount()).isGreaterThan(BigDecimal.ZERO);
                    assertThat(item.getExpectedInterestAmount()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
                });
    }

    @Test
    void calculateMonthlyPayment_zeroInterest_splitsPrincipalEvenly() {
        BigDecimal monthlyPayment = repaymentPlanService.calculateMonthlyPayment(
                new BigDecimal("12000.00"),
                12,
                BigDecimal.ZERO
        );

        assertThat(monthlyPayment).isEqualByComparingTo("1000.00");
    }

    @Test
    void calculateMonthlyPayment_standardInterest_usesExpectedRoundedValue() {
        BigDecimal monthlyPayment = repaymentPlanService.calculateMonthlyPayment(
                new BigDecimal("15000.00"),
                36,
                new BigDecimal("6.00")
        );

        assertThat(monthlyPayment).isEqualByComparingTo("456.33");
    }
}
