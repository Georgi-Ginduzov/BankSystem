package com.banksystem.service;

import com.banksystem.dto.InstallmentPaymentDTO;
import com.banksystem.dto.LoanApplicationFrontendDTO;
import com.banksystem.dto.LoanReviewRequestDTO;
import com.banksystem.dto.LoanApplicationDTO;
import com.banksystem.dto.LoanSummaryDto;
import com.banksystem.dto.RepaymentUpdateRequestDTO;
import com.banksystem.dto.LoanUpdateRequestDTO;
import com.banksystem.exception.BusinessException;
import com.banksystem.exception.LoanTypeCriteriaMismatchException;
import com.banksystem.exception.ResourceNotFoundException;
import com.banksystem.model.*;
import com.banksystem.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
    private final RepaymentPlanService repaymentPlanService;
    private static final ThreadLocalRandom RNG = ThreadLocalRandom.current();

    public LoanService(LoanRepository loanRepository,
                       AccountRepository accountRepository,
                       LoanTypeRepository loanTypeRepository,
                       ClientRepository clientRepository,
                       RepaymentRepository repaymentRepository,
                       EmployeeRepository employeeRepository,
                       RepaymentPlanService repaymentPlanService) {
        this.loanRepository = loanRepository;
        this.accountRepository = accountRepository;
        this.loanTypeRepository = loanTypeRepository;
        this.clientRepository = clientRepository;
        this.repaymentRepository = repaymentRepository;
        this.employeeRepository = employeeRepository;
        this.repaymentPlanService = repaymentPlanService;
    }

    private Account createLoanAccount(String clientId, BigDecimal loanAmount) {
        String iban = generateUniqueIban();

        Account account = new Account(
                clientId,
                iban,
                loanAmount,
                Account.AccountType.LOAN
        );
        account.setStatus(Account.AccountStatus.CLOSED);

        return accountRepository.save(account);
    }

    private Account createMainCheckingAccount(String clientId) {
        Account account = new Account(
                clientId,
                generateUniqueIban(),
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                Account.AccountType.CHECKING
        );
        account.setStatus(Account.AccountStatus.ACTIVE);
        return accountRepository.save(account);
    }

    private String generateUniqueIban() {
        String iban;
        int attempts = 0;

        do {
            if (attempts++ > 10) {
                throw new IllegalStateException("Failed to generate unique IBAN");
            }
            iban = "BG"
                    + String.format("%02d", (RNG.nextInt(100)))
                    + "BANK"
                    + String.format("%018d", (RNG.nextLong(0, 1_000_000_000_000_000_000L)));

        } while (accountRepository.existsByIban(iban));

        return iban;
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
                .settlementAccount(resolveDefaultSettlementAccount(client.getId()))
                .initialAmount(request.getAmount())
                .startDate(request.getStartDate())
                .status(Loan.LoanStatus.PENDING)
                .monthlyPayment(repaymentPlanService.calculateMonthlyPayment(
                        request.getAmount(),
                        request.getTermMonths(),
                        loanType.getInterestRate()))
                .termMonths(request.getTermMonths())
                .remainingAmount(request.getAmount())
                .paidInstallments(0)
                .build();
        loanRepository.save(loan);
    }

    public List<LoanSummaryDto> getAllLoans(String status) {
        String normalizedStatus = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);

        return loanRepository.findAllByOrderByIdDesc()
                .stream()
                .filter(loan -> normalizedStatus.isEmpty() || loan.getStatus().name().equals(normalizedStatus))
                .map(this::mapLoanSummary)
                .toList();
    }

    public LoanSummaryDto getLoanById(Integer id) {
        return loanRepository
                .findById(id)
                .map(this::mapLoanSummary)
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
        activateLoan(loan);
        loanRepository.save(loan);
    }

    @Transactional
    public LoanSummaryDto reviewLoan(Integer loanId, String reviewerEmail, LoanReviewRequestDTO request) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));

        if (loan.getStatus() != Loan.LoanStatus.PENDING) {
            throw new BusinessException("Only pending loans can be reviewed");
        }

        Employee reviewer = employeeRepository.findByEmail(reviewerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        String action = request.getAction().trim().toUpperCase(Locale.ROOT);
        loan.setReviewedBy(reviewer);

        if ("APPROVE".equals(action)) {
            activateLoan(loan);
        } else if ("REJECT".equals(action) || "DISAPPROVE".equals(action)) {
            rejectLoan(loan);
        } else {
            throw new BusinessException("Unsupported review action");
        }

        return mapLoanSummary(loanRepository.save(loan));
    }

    @Transactional
    public LoanSummaryDto updateLoan(Integer loanId, LoanUpdateRequestDTO request) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));

        boolean amountChanged = false;
        boolean termChanged = false;

        if (request.getInitialAmount() != null) {
            if (request.getInitialAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("Initial amount must be positive");
            }
            loan.setInitialAmount(request.getInitialAmount());
            amountChanged = true;
        }

        if (request.getRemainingAmount() != null) {
            if (request.getRemainingAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("Remaining amount cannot be negative");
            }
            loan.setRemainingAmount(request.getRemainingAmount());
        }

        if (request.getTermMonths() != null) {
            if (request.getTermMonths() <= 0) {
                throw new BusinessException("Term months must be positive");
            }
            loan.setTermMonths(request.getTermMonths());
            termChanged = true;
        }

        if (request.getStatus() != null) {
            if (request.getStatus() == Loan.LoanStatus.ACTIVE) {
                activateLoan(loan);
            } else if (request.getStatus() == Loan.LoanStatus.REJECTED) {
                rejectLoan(loan);
            } else {
                loan.setStatus(request.getStatus());
            }
        }

        if (amountChanged || termChanged) {
            loan.setMonthlyPayment(repaymentPlanService.calculateMonthlyPayment(
                    loan.getInitialAmount(),
                    loan.getTermMonths(),
                    loan.getLoanType().getInterestRate()
            ));
        }

        if (loan.getAccount() != null && request.getRemainingAmount() != null) {
            loan.getAccount().setBalance(loan.getRemainingAmount());
            accountRepository.save(loan.getAccount());
        }

        return mapLoanSummary(loanRepository.save(loan));
    }

    private LoanType ensureLoanType(String name, BigDecimal interestRate, Integer maxTermMonths, BigDecimal maxAmount) {
        return loanTypeRepository.findByName(name)
                .orElseGet(() -> loanTypeRepository.save(LoanType.builder()
                        .name(name)
                        .interestRate(interestRate)
                        .maxTermMonths(maxTermMonths)
                        .maxAmount(maxAmount)
                        .build()));
    }

    private LoanType resolveFrontendLoanType(String frontendType) {
        String normalized = frontendType == null ? "" : frontendType.trim().toUpperCase();

        return switch (normalized) {
            case "PERSONAL", "CONSUMER" -> ensureLoanType(
                    "Consumer",
                    BigDecimal.valueOf(7.99),
                    60,
                    BigDecimal.valueOf(50000)
            );
            case "MORTGAGE" -> ensureLoanType(
                    "Mortgage",
                    BigDecimal.valueOf(3.50),
                    360,
                    BigDecimal.valueOf(500000)
            );
            case "AUTO", "BUSINESS" -> ensureLoanType(
                    "Business",
                    BigDecimal.valueOf(5.99),
                    120,
                    BigDecimal.valueOf(200000)
            );
            default -> loanTypeRepository.findByName(frontendType)
                    .orElseThrow(() -> new ResourceNotFoundException("LoanType not found for: " + frontendType));
        };
    }

    @Transactional
    public void markInstallmentAsPaid(InstallmentPaymentDTO request) {
        Loan loan = loanRepository.findById(request.getLoanId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + request.getLoanId()));

        if (!loan.getClient().getId().equals(request.getClientId())) {
            throw new BusinessException("Loan does not belong to the specified client");
        }

        if (loan.getStatus() != Loan.LoanStatus.ACTIVE) {
            throw new BusinessException("Loan is not active. Current status: " + loan.getStatus());
        }

        List<Repayment> schedule = getOrderedRepayments(loan);
        Repayment repayment = schedule.stream()
                .filter(item -> item.getMonthNumber().equals(request.getMonthNumber()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Repayment schedule not found for month " + request.getMonthNumber()));

        if (repayment.getStatus() == Repayment.RepaymentStatus.PAID) {
            throw new BusinessException("Installment for month " + request.getMonthNumber() + " is already paid");
        }

        Repayment nextOpenRepayment = schedule.stream()
                .filter(item -> item.getStatus() != Repayment.RepaymentStatus.PAID)
                .min(Comparator.comparing(Repayment::getMonthNumber))
                .orElse(null);

        if (nextOpenRepayment != null && !nextOpenRepayment.getMonthNumber().equals(request.getMonthNumber())) {
            throw new BusinessException("Only the next unpaid installment can be marked as paid");
        }

        BigDecimal paymentAmount = repaymentPlanService.normalizeCurrency(request.getPaymentAmount());
        BigDecimal expectedPaymentAmount = repaymentPlanService.normalizeCurrency(repayment.getExpectedPaymentAmount());

        if (paymentAmount.compareTo(expectedPaymentAmount) < 0) {
            throw new BusinessException(String.format(
                    "Payment amount %.2f is below the required installment amount %.2f",
                    paymentAmount,
                    expectedPaymentAmount
            ));
        }

        BigDecimal extraPayment = paymentAmount.subtract(expectedPaymentAmount);
        if (extraPayment.compareTo(BigDecimal.ZERO) > 0 && request.getOverpaymentStrategy() == null) {
            throw new BusinessException("Choose how the overpayment should affect the remaining installments");
        }

        Account repaymentAccount = resolveRepaymentAccount(loan);
        if (repaymentAccount == null || repaymentAccount.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new BusinessException("The linked repayment account is not active");
        }

        if (repaymentAccount.getType() == Account.AccountType.LOAN) {
            throw new BusinessException("Loan installments must be paid from a customer bank account");
        }

        if (repaymentAccount.getBalance().compareTo(paymentAmount) < 0) {
            throw new BusinessException("Insufficient funds. Available: " + repaymentAccount.getBalance());
        }

        BigDecimal maxExtraPrincipal = repaymentPlanService.normalizeCurrency(
                loan.getRemainingAmount().subtract(repayment.getExpectedPrincipalAmount())
        );
        if (extraPayment.compareTo(maxExtraPrincipal) > 0) {
            throw new BusinessException("Overpayment exceeds the remaining principal on this loan");
        }

        repaymentAccount.setBalance(repaymentAccount.getBalance().subtract(paymentAmount));
        accountRepository.save(repaymentAccount);

        BigDecimal actualPrincipal = repayment.getExpectedPrincipalAmount().add(extraPayment);
        BigDecimal newRemaining = repaymentPlanService.normalizeCurrency(loan.getRemainingAmount().subtract(actualPrincipal));

        repayment.setStatus(Repayment.RepaymentStatus.PAID);
        repayment.setPaymentDate(java.time.LocalDateTime.now());
        repayment.setActualPaymentAmount(paymentAmount);
        repayment.setActualInterestAmount(repayment.getExpectedInterestAmount());
        repayment.setActualPrincipalAmount(actualPrincipal);
        repayment.setActualRemainingToPay(newRemaining);
        repaymentRepository.save(repayment);

        loan.setRemainingAmount(newRemaining);
        loan.setPaidInstallments((loan.getPaidInstallments() == null ? 0 : loan.getPaidInstallments()) + 1);
        if (loan.getAccount() != null) {
            loan.getAccount().setBalance(newRemaining);
            accountRepository.save(loan.getAccount());
        }

        if (extraPayment.compareTo(BigDecimal.ZERO) > 0) {
            rebuildScheduleAfterPayment(loan, schedule, repayment, newRemaining, request.getOverpaymentStrategy());
        }

        if (newRemaining.compareTo(BigDecimal.ZERO) <= 0 || !hasOpenInstallments(loan)) {
            loan.setStatus(Loan.LoanStatus.PAID_OFF);
            loan.setRemainingAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        } else {
            loan.setStatus(Loan.LoanStatus.ACTIVE);
        }
        loanRepository.save(loan);
    }

    @Transactional
    public void updateInstallment(Integer loanId, Integer monthNumber, RepaymentUpdateRequestDTO request) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));

        if (loan.getStatus() != Loan.LoanStatus.ACTIVE) {
            throw new BusinessException("Only active loans can have editable installments");
        }

        List<Repayment> schedule = getOrderedRepayments(loan);
        Repayment nextOpenInstallment = schedule.stream()
                .filter(item -> item.getStatus() != Repayment.RepaymentStatus.PAID)
                .min(Comparator.comparing(Repayment::getMonthNumber))
                .orElse(null);
        Repayment installment = schedule.stream()
                .filter(item -> item.getMonthNumber().equals(monthNumber))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Installment not found"));

        if (installment.getStatus() == Repayment.RepaymentStatus.PAID) {
            throw new BusinessException("Paid installments cannot be modified");
        }

        if (installment.getDueDate() != null && installment.getDueDate().isBefore(LocalDate.now())) {
            throw new BusinessException("Previous installments cannot be modified");
        }

        BigDecimal outstandingBeforeInstallment = calculateOutstandingBeforeMonth(loan, schedule, monthNumber);
        BigDecimal paymentAmount = repaymentPlanService.normalizeCurrency(request.getExpectedPaymentAmount());
        BigDecimal interestAmount = repaymentPlanService.normalizeCurrency(
                outstandingBeforeInstallment.multiply(repaymentPlanService.getMonthlyRate(loan))
        );

        if (paymentAmount.compareTo(interestAmount) <= 0) {
            throw new BusinessException("Installment amount must be higher than the accrued monthly interest");
        }

        BigDecimal principalAmount = repaymentPlanService.normalizeCurrency(paymentAmount.subtract(interestAmount));
        if (principalAmount.compareTo(outstandingBeforeInstallment) > 0) {
            principalAmount = outstandingBeforeInstallment;
            paymentAmount = repaymentPlanService.normalizeCurrency(principalAmount.add(interestAmount));
        }

        BigDecimal remainingAfterInstallment = repaymentPlanService.normalizeCurrency(
                outstandingBeforeInstallment.subtract(principalAmount)
        );
        int totalEditableMonths = (int) schedule.stream()
                .filter(item -> item.getStatus() != Repayment.RepaymentStatus.PAID && item.getMonthNumber() >= monthNumber)
                .count();

        installment.setDueDate(request.getDueDate());
        installment.setExpectedPaymentAmount(paymentAmount);
        installment.setExpectedInterestAmount(interestAmount);
        installment.setExpectedPrincipalAmount(principalAmount);
        installment.setExpectedRemainingToPay(remainingAfterInstallment);
        repaymentRepository.save(installment);

        List<Repayment> futureInstallments = schedule.stream()
                .filter(item -> item.getStatus() != Repayment.RepaymentStatus.PAID && item.getMonthNumber() > monthNumber)
                .toList();

        if (!futureInstallments.isEmpty()) {
            repaymentRepository.deleteAll(futureInstallments);
        }

        List<Repayment> regeneratedInstallments = buildInstallmentCostReductionSchedule(
                loan,
                monthNumber + 1,
                request.getDueDate().plusMonths(1),
                remainingAfterInstallment,
                Math.max(
                        totalEditableMonths - 1,
                        remainingAfterInstallment.compareTo(BigDecimal.ZERO) > 0 ? 1 : 0
                )
        );

        if (!regeneratedInstallments.isEmpty()) {
            repaymentRepository.saveAll(regeneratedInstallments);
        }

        if (nextOpenInstallment != null && nextOpenInstallment.getMonthNumber().equals(monthNumber)) {
            loan.setMonthlyPayment(paymentAmount);
        }
        loan.setTermMonths(regeneratedInstallments.isEmpty() ? monthNumber : regeneratedInstallments.get(regeneratedInstallments.size() - 1).getMonthNumber());
        loanRepository.save(loan);
    }

    @Transactional
    public void applyForLoan(LoanApplicationFrontendDTO request) {
        Client client = resolveFrontendClient(request);

        LoanType loanType = resolveFrontendLoanType(request.getLoanType());

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
        Account settlementAccount = resolveSettlementAccount(client.getId(), request.getRepaymentAccountId());
        if (settlementAccount == null) {
            throw new BusinessException("An active customer account is required before requesting a loan");
        }

        Loan loan = Loan.builder()
                .loanType(loanType)
                .client(client)
                .account(account)
                .settlementAccount(settlementAccount)
                .initialAmount(request.getAmount())
                .startDate(startDate)
                .status(Loan.LoanStatus.PENDING)
                .monthlyPayment(repaymentPlanService.calculateMonthlyPayment(
                        request.getAmount(),
                        request.getPeriodMonths(),
                        loanType.getInterestRate()))
                .termMonths(request.getPeriodMonths())
                .remainingAmount(request.getAmount())
                .paidInstallments(0)
                .build();

        loanRepository.save(loan);
    }

    private Client resolveFrontendClient(LoanApplicationFrontendDTO request) {
        if (request.getClientId() != null && !request.getClientId().trim().isEmpty()) {
            return clientRepository.findById(request.getClientId().trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        }

        return getOrCreateClient(request.getCustomerName(), request.getEmail(), request.getPhone());
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

    private LoanSummaryDto mapLoanSummary(Loan loan) {
        return LoanSummaryDto
                .builder()
                .id(loan.getId())
                .clientId(loan.getClient().getId())
                .initialAmount(loan.getInitialAmount())
                .employeeId(loan.getReviewedBy() != null ? loan.getReviewedBy().getId() : null)
                .status(loan.getStatus())
                .startDate(loan.getStartDate())
                .remainingAmount(loan.getRemainingAmount())
                .paidInstallments(loan.getPaidInstallments())
                .loanTypeName(loan.getLoanType().getName())
                .termMonths(loan.getTermMonths())
                .monthlyPayment(loan.getMonthlyPayment())
                .settlementAccountId(loan.getSettlementAccount() != null ? loan.getSettlementAccount().getId() : null)
                .settlementAccountIban(loan.getSettlementAccount() != null ? loan.getSettlementAccount().getIban() : null)
                .build();
    }

    private void activateLoan(Loan loan) {
        loan.setStatus(Loan.LoanStatus.ACTIVE);
        loan.setPaidInstallments(loan.getPaidInstallments() == null ? 0 : loan.getPaidInstallments());

        Account settlementAccount = resolveRepaymentAccount(loan);
        if (settlementAccount == null) {
            settlementAccount = createMainCheckingAccount(loan.getClient().getId());
            loan.setSettlementAccount(settlementAccount);
        }

        Account account = loan.getAccount();
        if (account != null) {
            account.setStatus(Account.AccountStatus.ACTIVE);
            account.setBalance(loan.getRemainingAmount());
            accountRepository.save(account);
        }

        if (repaymentRepository.findByLoanOrderByMonthNumberAsc(loan).isEmpty()) {
            repaymentRepository.saveAll(repaymentPlanService.generateRepaymentSchedule(loan));
        }
    }

    private void rejectLoan(Loan loan) {
        loan.setStatus(Loan.LoanStatus.REJECTED);
        loan.setPaidInstallments(0);
        repaymentRepository.deleteByLoan(loan);

        Account account = loan.getAccount();
        if (account != null) {
            account.setStatus(Account.AccountStatus.CLOSED);
            account.setBalance(BigDecimal.ZERO);
            accountRepository.save(account);
        }
    }

    private List<Repayment> getOrderedRepayments(Loan loan) {
        return repaymentRepository.findByLoanOrderByMonthNumberAsc(loan);
    }

    private Account resolveRepaymentAccount(Loan loan) {
        if (loan.getSettlementAccount() != null) {
            return loan.getSettlementAccount();
        }

        Account fallbackAccount = resolveDefaultSettlementAccount(loan.getClient().getId());
        if (fallbackAccount != null) {
            loan.setSettlementAccount(fallbackAccount);
        }
        return fallbackAccount;
    }

    private Account resolveSettlementAccount(String clientId, Integer preferredAccountId) {
        if (preferredAccountId != null) {
            Account preferredAccount = accountRepository.findById(preferredAccountId)
                    .orElseThrow(() -> new ResourceNotFoundException("Repayment account not found"));

            if (!clientId.equals(preferredAccount.getClientId())) {
                throw new BusinessException("The selected repayment account does not belong to the client");
            }

            if (preferredAccount.getStatus() != Account.AccountStatus.ACTIVE) {
                throw new BusinessException("The selected repayment account is not active");
            }

            if (preferredAccount.getType() == Account.AccountType.LOAN) {
                throw new BusinessException("Loan accounts cannot be used as repayment accounts");
            }

            return preferredAccount;
        }

        return resolveDefaultSettlementAccount(clientId);
    }

    private Account resolveDefaultSettlementAccount(String clientId) {
        return accountRepository.findByClientIdAndStatus(clientId, Account.AccountStatus.ACTIVE)
                .stream()
                .filter(account -> account.getType() != Account.AccountType.LOAN)
                .findFirst()
                .orElse(null);
    }

    private boolean hasOpenInstallments(Loan loan) {
        return repaymentRepository.findByLoanOrderByMonthNumberAsc(loan)
                .stream()
                .anyMatch(item -> item.getStatus() != Repayment.RepaymentStatus.PAID);
    }

    private BigDecimal calculateOutstandingBeforeMonth(Loan loan, List<Repayment> schedule, int targetMonthNumber) {
        BigDecimal outstanding = repaymentPlanService.normalizeCurrency(loan.getInitialAmount());

        for (Repayment repayment : schedule) {
            if (repayment.getMonthNumber() >= targetMonthNumber) {
                break;
            }

            BigDecimal principalPortion = repayment.getStatus() == Repayment.RepaymentStatus.PAID
                    ? repayment.getActualPrincipalAmount()
                    : repayment.getExpectedPrincipalAmount();
            outstanding = repaymentPlanService.normalizeCurrency(outstanding.subtract(principalPortion));
        }

        return outstanding;
    }

    private void rebuildScheduleAfterPayment(
            Loan loan,
            List<Repayment> schedule,
            Repayment paidInstallment,
            BigDecimal remainingPrincipal,
            InstallmentPaymentDTO.OverpaymentStrategy strategy
    ) {
        List<Repayment> futureInstallments = schedule.stream()
                .filter(item -> item.getStatus() != Repayment.RepaymentStatus.PAID)
                .filter(item -> item.getMonthNumber() > paidInstallment.getMonthNumber())
                .toList();

        if (!futureInstallments.isEmpty()) {
            repaymentRepository.deleteAll(futureInstallments);
        }

        if (remainingPrincipal.compareTo(BigDecimal.ZERO) <= 0) {
            loan.setTermMonths(paidInstallment.getMonthNumber());
            return;
        }

        int remainingMonths = futureInstallments.size();
        List<Repayment> regenerated;

        if (strategy == InstallmentPaymentDTO.OverpaymentStrategy.REDUCE_TERM) {
            regenerated = buildReducedTermSchedule(
                    loan,
                    paidInstallment.getMonthNumber() + 1,
                    paidInstallment.getDueDate().plusMonths(1),
                    remainingPrincipal,
                    loan.getMonthlyPayment(),
                    remainingMonths
            );
        } else {
            regenerated = buildInstallmentCostReductionSchedule(
                    loan,
                    paidInstallment.getMonthNumber() + 1,
                    paidInstallment.getDueDate().plusMonths(1),
                    remainingPrincipal,
                    remainingMonths
            );

            if (!regenerated.isEmpty()) {
                loan.setMonthlyPayment(regenerated.get(0).getExpectedPaymentAmount());
            }
        }

        if (!regenerated.isEmpty()) {
            repaymentRepository.saveAll(regenerated);
            loan.setTermMonths(regenerated.get(regenerated.size() - 1).getMonthNumber());
        } else {
            loan.setTermMonths(paidInstallment.getMonthNumber());
        }
    }

    private List<Repayment> buildInstallmentCostReductionSchedule(
            Loan loan,
            int startMonthNumber,
            LocalDate firstDueDate,
            BigDecimal principal,
            int remainingMonths
    ) {
        if (remainingMonths <= 0 || principal.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }

        BigDecimal paymentAmount = repaymentPlanService.calculateMonthlyPayment(
                principal,
                remainingMonths,
                loan.getLoanType().getInterestRate()
        );

        return buildScheduleWithFixedRemainingMonths(
                loan,
                startMonthNumber,
                firstDueDate,
                principal,
                remainingMonths,
                paymentAmount
        );
    }

    private List<Repayment> buildReducedTermSchedule(
            Loan loan,
            int startMonthNumber,
            LocalDate firstDueDate,
            BigDecimal principal,
            BigDecimal fixedPaymentAmount,
            int maxMonths
    ) {
        List<Repayment> schedule = new ArrayList<>();
        BigDecimal remainingPrincipal = repaymentPlanService.normalizeCurrency(principal);
        BigDecimal monthlyRate = repaymentPlanService.getMonthlyRate(loan);
        BigDecimal fixedPayment = repaymentPlanService.normalizeCurrency(fixedPaymentAmount);

        for (int index = 0; index < maxMonths && remainingPrincipal.compareTo(BigDecimal.ZERO) > 0; index++) {
            BigDecimal interestAmount = repaymentPlanService.normalizeCurrency(remainingPrincipal.multiply(monthlyRate));
            BigDecimal principalAmount = repaymentPlanService.normalizeCurrency(fixedPayment.subtract(interestAmount));

            if (principalAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("The current monthly payment is too low to reduce the loan term");
            }

            BigDecimal paymentAmount = fixedPayment;
            if (principalAmount.compareTo(remainingPrincipal) >= 0) {
                principalAmount = remainingPrincipal;
                paymentAmount = repaymentPlanService.normalizeCurrency(principalAmount.add(interestAmount));
            }

            remainingPrincipal = repaymentPlanService.normalizeCurrency(remainingPrincipal.subtract(principalAmount));

            schedule.add(repaymentPlanService.buildPendingRepayment(
                    loan,
                    startMonthNumber + index,
                    firstDueDate.plusMonths(index),
                    paymentAmount,
                    interestAmount,
                    principalAmount,
                    remainingPrincipal
            ));
        }

        return schedule;
    }

    private List<Repayment> buildScheduleWithFixedRemainingMonths(
            Loan loan,
            int startMonthNumber,
            LocalDate firstDueDate,
            BigDecimal principal,
            int remainingMonths,
            BigDecimal paymentAmount
    ) {
        List<Repayment> schedule = new ArrayList<>();
        BigDecimal remainingPrincipal = repaymentPlanService.normalizeCurrency(principal);
        BigDecimal monthlyRate = repaymentPlanService.getMonthlyRate(loan);
        BigDecimal monthlyPayment = repaymentPlanService.normalizeCurrency(paymentAmount);

        for (int index = 0; index < remainingMonths && remainingPrincipal.compareTo(BigDecimal.ZERO) > 0; index++) {
            BigDecimal interestAmount = repaymentPlanService.normalizeCurrency(remainingPrincipal.multiply(monthlyRate));
            BigDecimal principalAmount = repaymentPlanService.normalizeCurrency(monthlyPayment.subtract(interestAmount));
            BigDecimal installmentPayment = monthlyPayment;

            if (index == remainingMonths - 1 || principalAmount.compareTo(remainingPrincipal) >= 0) {
                principalAmount = remainingPrincipal;
                installmentPayment = repaymentPlanService.normalizeCurrency(principalAmount.add(interestAmount));
            }

            remainingPrincipal = repaymentPlanService.normalizeCurrency(remainingPrincipal.subtract(principalAmount));

            schedule.add(repaymentPlanService.buildPendingRepayment(
                    loan,
                    startMonthNumber + index,
                    firstDueDate.plusMonths(index),
                    installmentPayment,
                    interestAmount,
                    principalAmount,
                    remainingPrincipal
            ));
        }

        return schedule;
    }

}
