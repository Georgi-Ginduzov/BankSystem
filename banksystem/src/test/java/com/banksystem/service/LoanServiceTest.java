package com.banksystem.service;

import com.banksystem.dto.LoanApplicationDTO;
import com.banksystem.exception.LoanTypeCriteriaMismatchException;
import com.banksystem.exception.ResourceNotFoundException;
import com.banksystem.model.*;
import com.banksystem.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock private LoanRepository loanRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private LoanTypeRepository loanTypeRepository;
    @Mock private ClientRepository clientRepository;

    @InjectMocks
    private LoanService loanService;

    private Customer defaultClient;
    private LoanType defaultLoanType;
    private LoanApplicationDTO defaultRequest;

    @BeforeEach
    void setUp() {
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
                    .thenReturn(Optional.of(defaultLoanType)); // 6%, max 50000, 60м
            var loanCaptor = ArgumentCaptor.forClass(Loan.class);

            loanService.applyForLoan(defaultRequest);

            verify(loanRepository).save(loanCaptor.capture());
            // PMT(0.5%, 36, 15000) = 456.33
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
            // 12000 / 12 = 1000.00
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
                    new BigDecimal("99999.00"), // надвишава max 50000
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
                    120, // надвишава max 60 месеца
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
                    new BigDecimal("50000.00"), // точно на максимума
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
                    60, // точно на максимума
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
                    .thenReturn(true); // винаги зает

            assertThatThrownBy(() -> loanService.applyForLoan(defaultRequest))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Failed to generate unique IBAN");
        }
    }
}
