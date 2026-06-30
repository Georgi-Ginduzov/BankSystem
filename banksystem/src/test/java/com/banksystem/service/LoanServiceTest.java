package com.banksystem.service;

import com.banksystem.dto.InstallmentPaymentDTO;
import com.banksystem.dto.LoanApplicationDTO;
import com.banksystem.dto.RepaymentUpdateRequestDTO;
import com.banksystem.exception.BusinessException;
import com.banksystem.exception.LoanTypeCriteriaMismatchException;
import com.banksystem.exception.ResourceNotFoundException;
import com.banksystem.model.Account;
import com.banksystem.model.Customer;
import com.banksystem.model.Employee;
import com.banksystem.model.Loan;
import com.banksystem.model.LoanType;
import com.banksystem.model.Repayment;
import com.banksystem.repository.AccountRepository;
import com.banksystem.repository.ClientRepository;
import com.banksystem.repository.EmployeeRepository;
import com.banksystem.repository.LoanRepository;
import com.banksystem.repository.LoanTypeRepository;
import com.banksystem.repository.RepaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock private LoanRepository loanRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private LoanTypeRepository loanTypeRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private RepaymentRepository repaymentRepository;
    @Mock private EmployeeRepository employeeRepository;

    private LoanService loanService;
    private RepaymentPlanService repaymentPlanService;

    private Customer defaultClient;
    private LoanType defaultLoanType;
    private LoanApplicationDTO defaultRequest;

    @BeforeEach
    void setUp() {
        repaymentPlanService = new RepaymentPlanService();
        loanService = new LoanService(
                loanRepository,
                accountRepository,
                loanTypeRepository,
                clientRepository,
                repaymentRepository,
                employeeRepository,
                repaymentPlanService
        );

        defaultClient = new Customer("9501011234", "Ivan", "Ivanov");

        defaultLoanType = LoanType.builder()
                .id(1)
                .name("CONSUMER")
                .interestRate(new BigDecimal("6.00"))
                .maxAmount(new BigDecimal("50000.00"))
                .maxTermMonths(60)
                .build();

        defaultRequest = new LoanApplicationDTO(
                "CONSUMER",
                "9501011234",
                new BigDecimal("15000.00"),
                36,
                LocalDate.of(2026, 6, 1)
        );
    }

    @Nested
    class SuccessScenarios {

        @BeforeEach
        void stubDefaults() {
            when(clientRepository.findById("9501011234"))
                    .thenReturn(Optional.of(defaultClient));
            when(loanTypeRepository.findByName("CONSUMER"))
                    .thenReturn(Optional.of(defaultLoanType));
            when(accountRepository.existsByIban(anyString()))
                    .thenReturn(false);
            when(accountRepository.save(any(Account.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(loanRepository.save(any(Loan.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
        }

        @Test
        void applyForLoan_validRequest_savesLoan() throws LoanTypeCriteriaMismatchException {
            loanService.applyForLoan(defaultRequest);

            verify(loanRepository, times(1)).save(any(Loan.class));
        }

        @Test
        void applyForLoan_validRequest_loanStatusIsPending() throws LoanTypeCriteriaMismatchException {
            var loanCaptor = ArgumentCaptor.forClass(Loan.class);

            loanService.applyForLoan(defaultRequest);

            verify(loanRepository).save(loanCaptor.capture());
            assertThat(loanCaptor.getValue().getStatus())
                    .isEqualTo(Loan.LoanStatus.PENDING);
        }

        @Test
        void applyForLoan_validRequest_amountsAreCorrect() throws LoanTypeCriteriaMismatchException {
            var loanCaptor = ArgumentCaptor.forClass(Loan.class);

            loanService.applyForLoan(defaultRequest);

            verify(loanRepository).save(loanCaptor.capture());
            Loan saved = loanCaptor.getValue();
            assertThat(saved.getInitialAmount()).isEqualByComparingTo("15000.00");
            assertThat(saved.getRemainingAmount()).isEqualByComparingTo("15000.00");
        }

        @Test
        void applyForLoan_validRequest_termMonthsSaved() throws LoanTypeCriteriaMismatchException {
            var loanCaptor = ArgumentCaptor.forClass(Loan.class);

            loanService.applyForLoan(defaultRequest);

            verify(loanRepository).save(loanCaptor.capture());
            assertThat(loanCaptor.getValue().getTermMonths()).isEqualTo(36);
        }

        @Test
        void applyForLoan_validRequest_monthlyPaymentIsPositive() throws LoanTypeCriteriaMismatchException {
            var loanCaptor = ArgumentCaptor.forClass(Loan.class);

            loanService.applyForLoan(defaultRequest);

            verify(loanRepository).save(loanCaptor.capture());
            assertThat(loanCaptor.getValue().getMonthlyPayment())
                    .isGreaterThan(BigDecimal.ZERO);
        }

        @Test
        void applyForLoan_validRequest_accountIsCreated() throws LoanTypeCriteriaMismatchException {
            loanService.applyForLoan(defaultRequest);

            verify(accountRepository, times(1)).save(any(Account.class));
        }

        @Test
        void applyForLoan_ibanCollision_retriesUntilUnique() throws LoanTypeCriteriaMismatchException {
            when(accountRepository.existsByIban(anyString()))
                    .thenReturn(true, true, false);

            loanService.applyForLoan(defaultRequest);

            verify(accountRepository, atLeast(3)).existsByIban(anyString());
        }

        @Test
        void approveLoan_withoutExistingCustomerAccount_createsMainCheckingAccount() {
            Account loanAccount = new Account(
                    defaultClient.getId(),
                    "BG80BANK123456789012345678",
                    new BigDecimal("15000.00"),
                    Account.AccountType.LOAN
            );
            loanAccount.setStatus(Account.AccountStatus.CLOSED);

            Loan pendingLoan = Loan.builder()
                    .id(8)
                    .client(defaultClient)
                    .loanType(defaultLoanType)
                    .account(loanAccount)
                    .initialAmount(new BigDecimal("15000.00"))
                    .remainingAmount(new BigDecimal("15000.00"))
                    .monthlyPayment(new BigDecimal("456.33"))
                    .termMonths(36)
                    .paidInstallments(0)
                    .startDate(LocalDate.of(2026, 6, 1))
                    .status(Loan.LoanStatus.PENDING)
                    .build();

            Employee reviewer = new Employee();
            reviewer.setId(3);
            reviewer.setEmail("employee@bank.com");

            when(loanRepository.findById(8)).thenReturn(Optional.of(pendingLoan));
            when(employeeRepository.findById(3)).thenReturn(Optional.of(reviewer));
            when(accountRepository.findByClientIdAndStatus(defaultClient.getId(), Account.AccountStatus.ACTIVE))
                    .thenReturn(List.of());
            when(repaymentRepository.findByLoanOrderByMonthNumberAsc(pendingLoan)).thenReturn(List.of());
            when(repaymentRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

            ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);

            loanService.approveLoan(8, 3);

            verify(accountRepository, atLeast(2)).save(accountCaptor.capture());
            List<Account> savedAccounts = accountCaptor.getAllValues();

            Account createdChecking = savedAccounts.stream()
                    .filter(account -> account.getType() == Account.AccountType.CHECKING)
                    .findFirst()
                    .orElse(null);

            assertThat(createdChecking).isNotNull();
            assertThat(createdChecking.getClientId()).isEqualTo(defaultClient.getId());
            assertThat(createdChecking.getStatus()).isEqualTo(Account.AccountStatus.ACTIVE);
            assertThat(createdChecking.getBalance()).isEqualByComparingTo("0.00");
            assertThat(pendingLoan.getSettlementAccount()).isSameAs(createdChecking);
            assertThat(pendingLoan.getStatus()).isEqualTo(Loan.LoanStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("calculateMonthlyPayment — PMT formula")
    class MonthlyPaymentCalculation {

        @BeforeEach
        void stubDefaults() {
            when(clientRepository.findById(anyString()))
                    .thenReturn(Optional.of(defaultClient));
            when(accountRepository.existsByIban(anyString()))
                    .thenReturn(false);
            when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        }

        @Test
        void monthlyPayment_standardRate_correctPMT() throws LoanTypeCriteriaMismatchException {
            when(loanTypeRepository.findByName("CONSUMER"))
                    .thenReturn(Optional.of(defaultLoanType));
            var loanCaptor = ArgumentCaptor.forClass(Loan.class);

            loanService.applyForLoan(defaultRequest);

            verify(loanRepository).save(loanCaptor.capture());
            assertThat(loanCaptor.getValue().getMonthlyPayment())
                    .isEqualByComparingTo("456.33");
        }

        @Test
        @DisplayName("PMT at 0% interest = principal / months")
        void monthlyPayment_zeroInterestRate_equalInstallments() throws LoanTypeCriteriaMismatchException {
            LoanType zeroRateType = LoanType.builder()
                    .id(2)
                    .name("ZERO_RATE")
                    .interestRate(BigDecimal.ZERO)
                    .maxAmount(new BigDecimal("50000.00"))
                    .maxTermMonths(60)
                    .build();

            when(loanTypeRepository.findByName("ZERO_RATE"))
                    .thenReturn(Optional.of(zeroRateType));

            LoanApplicationDTO zeroRateRequest = new LoanApplicationDTO(
                    "ZERO_RATE", "9501011234",
                    new BigDecimal("12000.00"), 12,
                    LocalDate.of(2026, 6, 1)
            );
            var loanCaptor = ArgumentCaptor.forClass(Loan.class);

            loanService.applyForLoan(zeroRateRequest);

            verify(loanRepository).save(loanCaptor.capture());
            assertThat(loanCaptor.getValue().getMonthlyPayment())
                    .isEqualByComparingTo("1000.00");
        }
    }

    @Nested
    @DisplayName("applyForLoan — missing data")
    class ResourceNotFoundScenarios {

        @Test
        void applyForLoan_clientNotFound_throwsResourceNotFoundException() {
            when(clientRepository.findById("9501011234"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> loanService.applyForLoan(defaultRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Client not found");
        }

        @Test
        void applyForLoan_loanTypeNotFound_throwsResourceNotFoundException() {
            when(clientRepository.findById("9501011234"))
                    .thenReturn(Optional.of(defaultClient));
            when(loanTypeRepository.findByName("CONSUMER"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> loanService.applyForLoan(defaultRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("LoanType not found");
        }

        @Test
        void applyForLoan_clientNotFound_nothingPersisted() {
            when(clientRepository.findById(anyString()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> loanService.applyForLoan(defaultRequest));

            verify(accountRepository, never()).save(any());
            verify(loanRepository, never()).save(any());
        }
    }

    @Nested
    class LoanTypeCriteriaScenarios {

        @BeforeEach
        void stubClientAndType() {
            when(clientRepository.findById("9501011234"))
                    .thenReturn(Optional.of(defaultClient));
            when(loanTypeRepository.findByName("CONSUMER"))
                    .thenReturn(Optional.of(defaultLoanType));
        }

        @Test
        void applyForLoan_amountExceedsMax_throwsMismatchException() {
            LoanApplicationDTO overAmountRequest = new LoanApplicationDTO(
                    "CONSUMER", "9501011234",
                    new BigDecimal("99999.00"),
                    36,
                    LocalDate.of(2026, 6, 1)
            );

            assertThatThrownBy(() -> loanService.applyForLoan(overAmountRequest))
                    .isInstanceOf(LoanTypeCriteriaMismatchException.class)
                    .hasMessageContaining("exceeds max loan type amount");
        }

        @Test
        void applyForLoan_termExceedsMax_throwsMismatchException() {
            LoanApplicationDTO overTermRequest = new LoanApplicationDTO(
                    "CONSUMER", "9501011234",
                    new BigDecimal("15000.00"),
                    120,
                    LocalDate.of(2026, 6, 1)
            );

            assertThatThrownBy(() -> loanService.applyForLoan(overTermRequest))
                    .isInstanceOf(LoanTypeCriteriaMismatchException.class)
                    .hasMessageContaining("term exceeds");
        }

        @Test
        void applyForLoan_amountExceedsMax_accountNotPersisted() {
            LoanApplicationDTO overAmountRequest = new LoanApplicationDTO(
                    "CONSUMER", "9501011234",
                    new BigDecimal("99999.00"),
                    36,
                    LocalDate.of(2026, 6, 1)
            );

            assertThatThrownBy(() -> loanService.applyForLoan(overAmountRequest));

            verify(accountRepository, never()).save(any());
            verify(loanRepository, never()).save(any());
        }

        @Test
        void applyForLoan_amountExactlyAtMax_doesNotThrow() {
            when(accountRepository.existsByIban(anyString())).thenReturn(false);
            when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            LoanApplicationDTO exactMaxRequest = new LoanApplicationDTO(
                    "CONSUMER", "9501011234",
                    new BigDecimal("50000.00"),
                    36,
                    LocalDate.of(2026, 6, 1)
            );

            assertThatNoException().isThrownBy(
                    () -> loanService.applyForLoan(exactMaxRequest)
            );
        }

        @Test
        void applyForLoan_termExactlyAtMax_doesNotThrow() {
            when(accountRepository.existsByIban(anyString())).thenReturn(false);
            when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            LoanApplicationDTO exactMaxTermRequest = new LoanApplicationDTO(
                    "CONSUMER", "9501011234",
                    new BigDecimal("15000.00"),
                    60,
                    LocalDate.of(2026, 6, 1)
            );

            assertThatNoException().isThrownBy(
                    () -> loanService.applyForLoan(exactMaxTermRequest)
            );
        }
    }

    @Nested
    @DisplayName("generateUniqueIban")
    class IbanGenerationScenarios {

        @Test
        @DisplayName("Throws IllegalStateException after 10 failed IBAN generations")
        void generateUniqueIban_alwaysCollides_throwsIllegalStateException() {
            when(clientRepository.findById(anyString()))
                    .thenReturn(Optional.of(defaultClient));
            when(loanTypeRepository.findByName(anyString()))
                    .thenReturn(Optional.of(defaultLoanType));
            when(accountRepository.existsByIban(anyString()))
                    .thenReturn(true);

            assertThatThrownBy(() -> loanService.applyForLoan(defaultRequest))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Failed to generate unique IBAN");
        }
    }

    @Nested
    @DisplayName("repayment plan payment logic")
    class RepaymentPaymentScenarios {

        private Loan activeLoan;
        private Account loanAccount;
        private Account settlementAccount;
        private List<Repayment> repaymentPlan;

        @BeforeEach
        void setUpLoan() {
            loanAccount = new Account(
                    defaultClient.getId(),
                    "BG80BANK123456789012345678",
                    new BigDecimal("5000.00"),
                    Account.AccountType.LOAN
            );
            loanAccount.setStatus(Account.AccountStatus.ACTIVE);

            settlementAccount = new Account(
                    defaultClient.getId(),
                    "BG80BANK999999999999999999",
                    new BigDecimal("5000.00"),
                    Account.AccountType.CHECKING
            );
            settlementAccount.setId(42);
            settlementAccount.setStatus(Account.AccountStatus.ACTIVE);

            activeLoan = Loan.builder()
                    .id(7)
                    .client(defaultClient)
                    .loanType(defaultLoanType)
                    .account(loanAccount)
                    .settlementAccount(settlementAccount)
                    .initialAmount(new BigDecimal("1200.00"))
                    .remainingAmount(new BigDecimal("1200.00"))
                    .monthlyPayment(repaymentPlanService.calculateMonthlyPayment(
                            new BigDecimal("1200.00"),
                            4,
                            defaultLoanType.getInterestRate()
                    ))
                    .termMonths(4)
                    .paidInstallments(0)
                    .startDate(LocalDate.now())
                    .status(Loan.LoanStatus.ACTIVE)
                    .build();

            repaymentPlan = repaymentPlanService.generateRepaymentSchedule(activeLoan);

            when(loanRepository.findById(activeLoan.getId())).thenReturn(Optional.of(activeLoan));
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
            when(repaymentRepository.save(any(Repayment.class))).thenAnswer(inv -> inv.getArgument(0));
            when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));
            when(repaymentRepository.findByLoanOrderByMonthNumberAsc(activeLoan)).thenReturn(repaymentPlan);
            when(accountRepository.findByClientIdAndStatus(defaultClient.getId(), Account.AccountStatus.ACTIVE))
                    .thenReturn(List.of(settlementAccount));
        }

        @Test
        void markInstallmentAsPaid_exactPayment_marksInstallmentAndReducesBalance() {
            Repayment firstInstallment = repaymentPlan.get(0);
            InstallmentPaymentDTO request = new InstallmentPaymentDTO();
            request.setLoanId(activeLoan.getId());
            request.setClientId(defaultClient.getId());
            request.setMonthNumber(firstInstallment.getMonthNumber());
            request.setPaymentAmount(firstInstallment.getExpectedPaymentAmount());

            loanService.markInstallmentAsPaid(request);

            assertThat(firstInstallment.getStatus()).isEqualTo(Repayment.RepaymentStatus.PAID);
            assertThat(firstInstallment.getActualPaymentAmount())
                    .isEqualByComparingTo(firstInstallment.getExpectedPaymentAmount());
            assertThat(activeLoan.getPaidInstallments()).isEqualTo(1);
            assertThat(activeLoan.getRemainingAmount())
                    .isEqualByComparingTo(firstInstallment.getExpectedRemainingToPay());
            assertThat(settlementAccount.getBalance()).isLessThan(new BigDecimal("5000.00"));
            assertThat(loanAccount.getBalance()).isEqualByComparingTo(activeLoan.getRemainingAmount());
            verify(accountRepository).save(loanAccount);
            verify(repaymentRepository).save(firstInstallment);
            verify(loanRepository).save(activeLoan);
        }

        @Test
        void markInstallmentAsPaid_overpaymentReduceInstallment_rebuildsFuturePlanWithLowerNextPayment() {
            Repayment firstInstallment = repaymentPlan.get(0);
            BigDecimal originalNextPayment = repaymentPlan.get(1).getExpectedPaymentAmount();
            InstallmentPaymentDTO request = new InstallmentPaymentDTO();
            request.setLoanId(activeLoan.getId());
            request.setClientId(defaultClient.getId());
            request.setMonthNumber(firstInstallment.getMonthNumber());
            request.setPaymentAmount(firstInstallment.getExpectedPaymentAmount().add(new BigDecimal("100.00")));
            request.setOverpaymentStrategy(InstallmentPaymentDTO.OverpaymentStrategy.REDUCE_INSTALLMENT);

            ArgumentCaptor<List<Repayment>> futureScheduleCaptor = ArgumentCaptor.forClass(List.class);

            loanService.markInstallmentAsPaid(request);

            verify(repaymentRepository).saveAll(futureScheduleCaptor.capture());
            List<Repayment> regenerated = futureScheduleCaptor.getValue();

            assertThat(regenerated).hasSize(3);
            assertThat(regenerated.get(0).getExpectedPaymentAmount())
                    .isLessThan(originalNextPayment);
            assertThat(activeLoan.getMonthlyPayment())
                    .isEqualByComparingTo(regenerated.get(0).getExpectedPaymentAmount());
            assertThat(regenerated.get(0).getMonthNumber()).isEqualTo(2);
        }

        @Test
        void markInstallmentAsPaid_overpaymentReduceTerm_shortensRepaymentPlan() {
            activeLoan.setInitialAmount(new BigDecimal("3000.00"));
            activeLoan.setRemainingAmount(new BigDecimal("3000.00"));
            activeLoan.setMonthlyPayment(repaymentPlanService.calculateMonthlyPayment(
                    new BigDecimal("3000.00"),
                    6,
                    defaultLoanType.getInterestRate()
            ));
            activeLoan.setTermMonths(6);
            repaymentPlan = repaymentPlanService.generateRepaymentSchedule(activeLoan);
            when(repaymentRepository.findByLoanOrderByMonthNumberAsc(activeLoan)).thenReturn(repaymentPlan);

            Repayment firstInstallment = repaymentPlan.get(0);
            InstallmentPaymentDTO request = new InstallmentPaymentDTO();
            request.setLoanId(activeLoan.getId());
            request.setClientId(defaultClient.getId());
            request.setMonthNumber(firstInstallment.getMonthNumber());
            request.setPaymentAmount(firstInstallment.getExpectedPaymentAmount().add(new BigDecimal("1000.00")));
            request.setOverpaymentStrategy(InstallmentPaymentDTO.OverpaymentStrategy.REDUCE_TERM);

            ArgumentCaptor<List<Repayment>> futureScheduleCaptor = ArgumentCaptor.forClass(List.class);

            loanService.markInstallmentAsPaid(request);

            verify(repaymentRepository).saveAll(futureScheduleCaptor.capture());
            List<Repayment> regenerated = futureScheduleCaptor.getValue();

            assertThat(regenerated.size()).isLessThan(5);
            assertThat(regenerated.get(0).getExpectedPaymentAmount())
                    .isEqualByComparingTo(activeLoan.getMonthlyPayment());
            assertThat(activeLoan.getTermMonths())
                    .isEqualTo(regenerated.get(regenerated.size() - 1).getMonthNumber());
        }

        @Test
        void updateInstallment_previousInstallment_throwsBusinessException() {
            Repayment overdueInstallment = repaymentPlan.get(0);
            overdueInstallment.setDueDate(LocalDate.now().minusDays(3));

            RepaymentUpdateRequestDTO request = new RepaymentUpdateRequestDTO();
            request.setDueDate(LocalDate.now().plusDays(10));
            request.setExpectedPaymentAmount(new BigDecimal("350.00"));

            assertThatThrownBy(() -> loanService.updateInstallment(activeLoan.getId(), 1, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Previous installments cannot be modified");

            verify(repaymentRepository, never()).deleteAll(any());
        }
    }
}
