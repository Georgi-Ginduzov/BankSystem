package com.banksystem.service;

import com.banksystem.dto.AccountRequestDTO;
import com.banksystem.exception.BusinessException;
import com.banksystem.model.Account;
import com.banksystem.model.Client;
import com.banksystem.model.Customer;
import com.banksystem.repository.AccountRepository;
import com.banksystem.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private AccountService accountService;

    private AccountRequestDTO validRequest;
    private Client client;

    @BeforeEach
    void setUp() {
        validRequest = new AccountRequestDTO();
        validRequest.setClientId("1234567890");
        validRequest.setIban("BG80BNBG96611020345678");
        validRequest.setInitialDeposit(new BigDecimal("1000.00"));
        validRequest.setAccountType("CHECKING");

        client = new Customer("1234567890", "Ivan", "Ivanov");
    }

    @Test
    void openAccount_success() {
        when(clientRepository.findById("1234567890")).thenReturn(Optional.of(client));
        when(accountRepository.existsByIban(validRequest.getIban())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> {
            Account acc = inv.getArgument(0);
            acc.setId(1);
            return acc;
        });

        Account result = accountService.openAccount(validRequest);

        assertThat(result).isNotNull();
        assertThat(result.getIban()).isEqualTo(validRequest.getIban());
        assertThat(result.getBalance()).isEqualByComparingTo("1000.00");
        assertThat(result.getStatus()).isEqualTo(Account.AccountStatus.ACTIVE);
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void openAccount_clientNotFound_throwsException() {
        when(clientRepository.findById("1234567890")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.openAccount(validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Client not found with id: 1234567890");
        verify(accountRepository, never()).save(any());
    }

    @Test
    void openAccount_duplicateIban_throwsException() {
        when(clientRepository.findById("1234567890")).thenReturn(Optional.of(client));
        when(accountRepository.existsByIban(validRequest.getIban())).thenReturn(true);

        assertThatThrownBy(() -> accountService.openAccount(validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Account with IBAN BG80BNBG96611020345678 already exists");
    }

    @Test
    void openAccount_negativeDeposit_throwsException() {
        validRequest.setInitialDeposit(new BigDecimal("-100.00"));
        when(clientRepository.findById("1234567890")).thenReturn(Optional.of(client));
        when(accountRepository.existsByIban(anyString())).thenReturn(false);

        assertThatThrownBy(() -> accountService.openAccount(validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Initial deposit cannot be negative");
    }

    @Test
    void openAccount_invalidType_throwsException() {
        validRequest.setAccountType("INVALID");
        when(clientRepository.findById("1234567890")).thenReturn(Optional.of(client));
        when(accountRepository.existsByIban(anyString())).thenReturn(false);

        assertThatThrownBy(() -> accountService.openAccount(validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid account type");
    }

    @Test
    void getAccountById_found_returnsAccount() {
        Account account = new Account("1234567890", "BG80BNBG96611020345678", new BigDecimal("1000.00"), Account.AccountType.CHECKING);
        account.setId(1);
        when(accountRepository.findById(1)).thenReturn(Optional.of(account));

        Account result = accountService.getAccountById(1);
        assertThat(result).isEqualTo(account);
    }

    @Test
    void getAccountById_notFound_throwsException() {
        when(accountRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccountById(999))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Account not found with id: 999");
    }

    @Test
    void closeAccount_success() {
        Account account = new Account("1234567890", "BG80BNBG96611020345678", BigDecimal.ZERO, Account.AccountType.CHECKING);
        account.setId(1);
        account.setStatus(Account.AccountStatus.ACTIVE);
        when(accountRepository.findById(1)).thenReturn(Optional.of(account));

        accountService.closeAccount(1, "1234567890");

        assertThat(account.getStatus()).isEqualTo(Account.AccountStatus.CLOSED);
        verify(accountRepository).save(account);
    }

    @Test
    void closeAccount_wrongClient_throwsException() {
        Account account = new Account("999", "BG80BNBG96611020345678", BigDecimal.ZERO, Account.AccountType.CHECKING);
        account.setId(1);
        when(accountRepository.findById(1)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.closeAccount(1, "1234567890"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Account does not belong to the specified client");
        verify(accountRepository, never()).save(any());
    }

    @Test
    void closeAccount_nonZeroBalance_throwsException() {
        Account account = new Account("1234567890", "BG80BNBG96611020345678", new BigDecimal("500.00"), Account.AccountType.CHECKING);
        account.setId(1);
        when(accountRepository.findById(1)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.closeAccount(1, "1234567890"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot close account with non-zero balance");
    }
}