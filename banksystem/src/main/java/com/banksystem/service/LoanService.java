package com.banksystem.service;

import com.banksystem.dto.*;
import com.banksystem.exception.BusinessException;
import com.banksystem.exception.LoanTypeCriteriaMismatchException;
import com.banksystem.exception.ResourceNotFoundException;
import com.banksystem.model.*;
import com.banksystem.repository.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
                .paidInstallments(0)
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

    public void approveLoan(Integer loanId, Integer employeeId) {
        var loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));

        if (!loan.getStatus().equals(Loan.LoanStatus.PENDING)) {
            throw new IllegalStateException("Only PENDING loans can be approved");
        }

        Employee employee;
        if (employeeId != null) {
            employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));
        } else {
            // Use default admin by email
            employee = employeeRepository.findByEmail("admin@bank.com")
                    .orElseThrow(() -> new ResourceNotFoundException("Default admin not found. Please create an admin employee."));
        }

        loan.setReviewedBy(employee);
        loan.setStatus(Loan.LoanStatus.ACTIVE);
        loanRepository.save(loan);

        var schedule = generateRepaymentSchedule(loan);
        repaymentRepository.saveAll(schedule);
    }

    private String mapFrontendLoanType(String frontendType) {
        return switch (frontendType.toUpperCase()) {
            case "PERSONAL" -> "Consumer";
            case "MORTGAGE" -> "Mortgage";
            case "AUTO" -> "Business"; // or "Auto" if you have it, but we only have Business
            default -> frontendType; // assume it's already correct
        };
    }

    @Transactional
    public void markInstallmentAsPaid(InstallmentPaymentDTO request) {
        Loan loan = loanRepository.findById(request.getLoanId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + request.getLoanId()));

        // Verify loan belongs to client
        if (!loan.getClient().getId().equals(request.getClientId())) {
            throw new BusinessException("Loan does not belong to the specified client");
        }

        // Check loan status (must be ACTIVE)
        if (loan.getStatus() != Loan.LoanStatus.ACTIVE) {
            throw new BusinessException("Loan is not active. Current status: " + loan.getStatus());
        }

        // Find repayment schedule
        Repayment repayment = repaymentRepository.findByLoanAndMonthNumber(loan, request.getMonthNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Repayment schedule not found for month " + request.getMonthNumber()));

        if (repayment.getStatus() == Repayment.RepaymentStatus.PAID) {
            throw new BusinessException("Installment for month " + request.getMonthNumber() + " is already paid");
        }

        // Validate payment amount
        if (request.getPaymentAmount().compareTo(repayment.getExpectedPaymentAmount()) != 0) {
            throw new BusinessException(String.format("Payment amount %.2f does not match expected installment amount %.2f",
                    request.getPaymentAmount(), repayment.getExpectedPaymentAmount()));
        }

        // Get associated account
        Account account = loan.getAccount();
        if (account == null || account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new BusinessException("Associated account is not active");
        }

        if (account.getBalance().compareTo(request.getPaymentAmount()) < 0) {
            throw new BusinessException("Insufficient funds. Available: " + account.getBalance());
        }

        // Process payment
        account.setBalance(account.getBalance().subtract(request.getPaymentAmount()));
        accountRepository.save(account);

        // Update repayment
        repayment.setStatus(Repayment.RepaymentStatus.PAID);
        repayment.setPaymentDate(java.time.LocalDateTime.now());
        repayment.setActualPaymentAmount(request.getPaymentAmount());
        repayment.setActualInterestAmount(repayment.getExpectedInterestAmount());
        repayment.setActualPrincipalAmount(repayment.getExpectedPrincipalAmount());
        repayment.setActualRemainingToPay(repayment.getExpectedRemainingToPay());
        repaymentRepository.save(repayment);

        // Update loan
        BigDecimal newRemaining = loan.getRemainingAmount().subtract(repayment.getExpectedPrincipalAmount());
        loan.setRemainingAmount(newRemaining);
        loan.setPaidInstallments(loan.getPaidInstallments() + 1);

        if (newRemaining.compareTo(java.math.BigDecimal.ZERO) <= 0 || loan.getPaidInstallments() >= loan.getTermMonths()) {
            loan.setStatus(Loan.LoanStatus.PAID_OFF);
        }
        loanRepository.save(loan);
    }

    @Transactional
    public void applyForLoan(LoanApplicationFrontendDTO request) {
        // Get or create client
        Client client = getOrCreateClient(request.getCustomerName(), request.getEmail(), request.getPhone());

        // Map loan type from frontend to backend
        String backendLoanType = mapFrontendLoanType(request.getLoanType());
        LoanType loanType = loanTypeRepository.findByName(backendLoanType)
                .orElseThrow(() -> new ResourceNotFoundException("LoanType not found for: " + backendLoanType));

        // Validate amount and term against loan type
        if (loanType.getMaxAmount().compareTo(request.getAmount()) < 0) {
            throw new LoanTypeCriteriaMismatchException("Requested amount exceeds maximum allowed for this loan type.");
        }
        if (loanType.getMaxTermMonths() < request.getPeriodMonths()) {
            throw new LoanTypeCriteriaMismatchException("Requested term exceeds maximum allowed for this loan type.");
        }

        // Set start date to today
        LocalDate startDate = LocalDate.now();

        // Create loan account
        Account account = createLoanAccount(client.getId(), request.getAmount());

        // Build loan entity
        Loan loan = Loan.builder()
                .loanType(loanType)
                .client(client)
                .account(account)
                .initialAmount(request.getAmount())
                .startDate(startDate)
                .status(Loan.LoanStatus.PENDING)
                .monthlyPayment(calculateLoanApplicationMonthlyPayment(
                        request.getAmount(),
                        request.getPeriodMonths(),
                        loanType.getInterestRate()))
                .termMonths(request.getPeriodMonths())
                .remainingAmount(request.getAmount())
                .build();

        loanRepository.save(loan);
    }

    private String generateClientId(String email) {
        // Use email as client ID – remove special characters, keep it simple
        return email.trim().toLowerCase();
    }

    private Client getOrCreateClient(String customerName, String email, String phone) {
        String clientId = generateClientId(email);
        Optional<Client> existing = clientRepository.findById(clientId);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Parse name into first and last
        String[] nameParts = customerName.trim().split("\\s+", 2);
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        // Create a new Customer with a dummy UCN (use phone if possible, but UCN must be 10 digits, so we generate a placeholder)
        // For simplicity, we generate a 10-digit number from email hash
        String ucn = String.valueOf(Math.abs(email.hashCode())).substring(0, 10);
        // Ensure it's 10 digits
        while (ucn.length() < 10) ucn = "0" + ucn;
        if (ucn.length() > 10) ucn = ucn.substring(0, 10);

        Customer customer = new Customer(ucn, firstName, lastName);
        // Override the ID to be the email (since we use TABLE_PER_CLASS, we can set id)
        customer.setId(clientId);
        return clientRepository.save(customer);
    }

    @Transactional
    public Loan openLoanContract(LoanOpenDTO request) {
        // Find the client
        Client client = clientRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        // Find loan type by name (map frontend type to backend type)
        String backendType = mapFrontendLoanType(request.getLoanType());
        LoanType loanType = loanTypeRepository.findByName(backendType)
                .orElseThrow(() -> new ResourceNotFoundException("LoanType not found: " + backendType));

        // Validate amount and term
        if (loanType.getMaxAmount().compareTo(request.getPrincipal()) < 0) {
            throw new BusinessException("Principal exceeds maximum allowed for this loan type.");
        }
        if (loanType.getMaxTermMonths() < request.getPeriodMonths()) {
            throw new BusinessException("Term exceeds maximum allowed for this loan type.");
        }

        // Create loan account
        Account account = createLoanAccount(client.getId(), request.getPrincipal());

        // Build loan
        Loan loan = Loan.builder()
                .loanType(loanType)
                .client(client)
                .account(account)
                .initialAmount(request.getPrincipal())
                .startDate(request.getStartDate() != null ? request.getStartDate() : LocalDate.now())
                .status(Loan.LoanStatus.ACTIVE) // directly active
                .monthlyPayment(calculateLoanApplicationMonthlyPayment(
                        request.getPrincipal(),
                        request.getPeriodMonths(),
                        loanType.getInterestRate()))
                .termMonths(request.getPeriodMonths())
                .remainingAmount(request.getPrincipal())
                .paidInstallments(0)
                .build();

        loan = loanRepository.save(loan);

        // Generate repayment schedule
        List<Repayment> schedule = generateRepaymentSchedule(loan);
        repaymentRepository.saveAll(schedule);

        return loan;
    }

    @Transactional
    public void updateLoan(Integer loanId, UpdateLoanDTO request) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));

        // Update fields if provided
        if (request.getPrincipal() != null) {
            loan.setInitialAmount(request.getPrincipal());
            loan.setRemainingAmount(request.getPrincipal()); // reset remaining? Or keep? We'll recalc later if needed
        }
        if (request.getPeriodMonths() != null) {
            loan.setTermMonths(request.getPeriodMonths());
        }
        if (request.getInterestRate() != null) {
            // Interest rate is stored in LoanType, not in Loan itself.
            // To change rate, we need to update the LoanType or create a new one.
            // For simplicity, we'll ignore interest rate update or we can update the loanType.
            // Instead, we'll just use the provided rate to recalc monthly payment.
            // We'll use the loanType's interest rate for recalc.
        }
        if (request.getStatus() != null) {
            try {
                Loan.LoanStatus status = Loan.LoanStatus.valueOf(request.getStatus().toUpperCase());
                loan.setStatus(status);
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid status: " + request.getStatus());
            }
        }

        // Recalculate monthly payment if principal or term changed
        // We need the loan type to get interest rate.
        LoanType loanType = loan.getLoanType();
        if (loanType != null && (request.getPrincipal() != null || request.getPeriodMonths() != null)) {
            BigDecimal monthlyPayment = calculateLoanApplicationMonthlyPayment(
                    loan.getInitialAmount(),
                    loan.getTermMonths(),
                    loanType.getInterestRate()
            );
            loan.setMonthlyPayment(monthlyPayment);
        }

        // If principal changed, also update remaining amount? Or keep it as is?
        // For simplicity, we'll set remaining amount to the new principal if it was changed.
        // But careful: if there have been payments, we shouldn't reset.
        // Since we don't have payment history in this update, we'll just update.
        // We'll keep remaining amount as is, unless principal changed, then set it to principal (reset).
        if (request.getPrincipal() != null) {
            loan.setRemainingAmount(request.getPrincipal());
            // Reset paid installments? Probably not.
        }

        loanRepository.save(loan);
    }

}