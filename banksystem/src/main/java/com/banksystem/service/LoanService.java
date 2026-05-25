package com.banksystem.service;

import com.banksystem.dto.LoanApplicationDTO;
import com.banksystem.dto.LoanSummaryDto;
import com.banksystem.exception.LoanTypeCriteriaMismatchException;
import com.banksystem.exception.ResourceNotFoundException;
import com.banksystem.model.*;
import com.banksystem.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class LoanService {
    private final LoanRepository loanRepository;
    private final AccountRepository accountRepository;
    private final LoanTypeRepository loanTypeRepository;
    private final ClientRepository clientRepository;
    private final RepaymentRepository repaymentRepository;
    private final EmployeeRepository employeeRepository;
    private static final ThreadLocalRandom RNG = ThreadLocalRandom.current();

    public LoanService(LoanRepository loanRepository,
                       AccountRepository accountRepository,
                       LoanTypeRepository loanTypeRepository,
                       ClientRepository clientRepository,
                       RepaymentRepository repaymentRepository,
                       EmployeeRepository employeeRepository) {
        this.loanRepository = loanRepository;
        this.accountRepository = accountRepository;
        this.loanTypeRepository = loanTypeRepository;
        this.clientRepository = clientRepository;
        this.repaymentRepository = repaymentRepository;
        this.employeeRepository = employeeRepository;
    }

    private String generateUniqueIban() {
        String iban;
        int attempts = 0;

        do {
            if (attempts++ > 10) {
                throw new IllegalStateException("Failed to generate unique IBAN");
            }
            // BG + 2 контролни цифри + 4 букви банков код + 18 цифри
            iban = "BG"
                    + String.format("%02d", (RNG.nextInt(100)))
                    + "BANK"
                    + String.format("%018d", (RNG.nextLong(0, 1_000_000_000_000_000_000L)));

        } while (accountRepository.existsByIban(iban));

        return iban;
    }

    private Account createLoanAccount(String clientId, BigDecimal loanAmount) {
        String iban = generateUniqueIban();

        Account account = new Account(
                clientId,
                iban,
                loanAmount,
                Account.AccountType.LOAN
        );

        return accountRepository.save(account);
    }

    private BigDecimal calculateLoanApplicationMonthlyPayment(BigDecimal amount, Integer termMonths, BigDecimal annualInterestRate) {
        if (annualInterestRate.compareTo(BigDecimal.ZERO) == 0) {
            return amount.divide(
                    BigDecimal.valueOf(termMonths),
                    2,
                    RoundingMode.HALF_UP
            );
        }

        // r = годишна лихва / 12 / 100  (например 6% → 0.005)
        double r = annualInterestRate.doubleValue() / 12.0 / 100.0;
        double n = termMonths;
        double p = amount.doubleValue();

        // PMT = P * r / (1 - (1 + r)^(-n))
        double pmt = p * r / (1 - Math.pow(1 + r, -n));

        return BigDecimal.valueOf(pmt).setScale(2, RoundingMode.HALF_UP);
    }

    private List<Repayment> generateRepaymentSchedule(Loan loan)
    {
        List<Repayment> schedule = new ArrayList<>();

        var annualRate = loan.getLoanType().getInterestRate();
        var monthlyRate = annualRate
                .divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);

        var remainingPrincipal = loan.getInitialAmount();
        var monthlyPayment = loan.getMonthlyPayment();
        var totalMonths = loan.getTermMonths();

        for (int month = 1; month <= totalMonths; month++)
        {
            BigDecimal interestPortion = remainingPrincipal
                    .multiply(monthlyRate)
                    .setScale(2, RoundingMode.HALF_UP);

            var principalPortion = monthlyPayment
                    .subtract(interestPortion);

            // Последна вноска — коригираме за натрупани грешки при закръгляне
            if (month == totalMonths)
            {
                principalPortion = remainingPrincipal;
                monthlyPayment = principalPortion.add(interestPortion);
            }

            remainingPrincipal = remainingPrincipal
                    .subtract(principalPortion)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal remaining = month == totalMonths
                    ? BigDecimal.ZERO
                    : remainingPrincipal;

            Repayment repayment = Repayment.builder()
                    .loan(loan)
                    .monthNumber(month)
                    .dueDate(loan.getStartDate().plusMonths(month))
                    .status(Repayment.RepaymentStatus.PENDING)
                    .expectedPaymentAmount(monthlyPayment)
                    .expectedInterestAmount(interestPortion)
                    .expectedPrincipalAmount(principalPortion)
                    .expectedRemainingToPay(remaining)
                    .build();

            schedule.add(repayment);
        }

        return schedule;
    }

    public void applyForLoan(LoanApplicationDTO request) {
        var client = clientRepository
                .findById(request.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        var loanType = loanTypeRepository
                .findByName(request.getLoanType())
                .orElseThrow(() -> new ResourceNotFoundException("LoanType not found"));

        if (loanType.getMaxAmount().compareTo(request.getAmount()) < 0) {
            throw new LoanTypeCriteriaMismatchException("loan application exceeds max loan type amount");
        }

        if (loanType.getMaxTermMonths() < request.getTermMonths()) {
            throw new LoanTypeCriteriaMismatchException("loan application term exceeds max amount");
        }

        var account = createLoanAccount(request.getClientId(), request.getAmount());

        var loan = Loan
                .builder()
                .loanType(loanType)
                .client(client)
                .account(account)
                .initialAmount(request.getAmount())
                .startDate(request.getStartDate())
                .status(Loan.LoanStatus.PENDING)
                .monthlyPayment(calculateLoanApplicationMonthlyPayment(
                        request.getAmount(),
                        request.getTermMonths(),
                        loanType.getInterestRate()))
                .termMonths(request.getTermMonths())
                .remainingAmount(request.getAmount())
                .build();
        loanRepository.save(loan);
    }

    public LoanSummaryDto getLoanById(Integer id) {
        return loanRepository
                .findById(id)
                .map(loan -> LoanSummaryDto
                        .builder()
                        .id(loan.getId())
                        .initialAmount(loan.getInitialAmount())
                        .status(loan.getStatus())
                        .startDate(loan.getStartDate())
                        .remainingAmount(loan.getRemainingAmount())
                        .paidInstallments(loan.getPaidInstallments())
                        .loanTypeName(loan.getLoanType().getName())
                        .termMonths(loan.getTermMonths())
                        .monthlyPayment(loan.getMonthlyPayment())
                        .build()
                )
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));
    }

    public void approveLoan(Integer loanId, Integer employeeId)
    {
        var loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));

        if (!loan.getStatus().equals(Loan.LoanStatus.PENDING)) {
            throw new IllegalStateException("Only PENDING loans can be approved");
        }

        var employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        loan.setReviewedBy(employee);
        loan.setStatus(Loan.LoanStatus.ACTIVE);
        loanRepository.save(loan);

        var schedule = generateRepaymentSchedule(loan);
        repaymentRepository.saveAll(schedule);
    }
}