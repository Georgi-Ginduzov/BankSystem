package com.banksystem.service;

import com.banksystem.dto.AccountRequestDTO;
import com.banksystem.dto.AccountUpdateRequestDTO;
import com.banksystem.exception.BusinessException;
import com.banksystem.model.Account;
import com.banksystem.model.Client;
import com.banksystem.repository.AccountRepository;
import com.banksystem.repository.ClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final ClientRepository clientRepository;
    private static final ThreadLocalRandom RNG = ThreadLocalRandom.current();

    public AccountService(AccountRepository accountRepository, ClientRepository clientRepository) {
        this.accountRepository = accountRepository;
        this.clientRepository = clientRepository;
    }

    private String generateUniqueIban() {
        String iban;
        int attempts = 0;

        do {
            if (attempts++ > 10) {
                throw new IllegalStateException("Failed to generate unique IBAN");
            }

            iban = "BG"
                    + String.format("%02d", RNG.nextInt(100))
                    + "BANK"
                    + String.format("%018d", RNG.nextLong(0, 1_000_000_000_000_000_000L));
        } while (accountRepository.existsByIban(iban));

        return iban;
    }

    @Transactional
    public Account openAccount(AccountRequestDTO request) {
        // Validate client exists
        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new BusinessException("Client not found with id: " + request.getClientId()));

        String iban = request.getIban() == null || request.getIban().trim().isEmpty()
                ? generateUniqueIban()
                : request.getIban().trim();

        // Check for duplicate IBAN
        if (accountRepository.existsByIban(iban)) {
            throw new BusinessException("Account with IBAN " + iban + " already exists");
        }

        // Validate initial deposit is not negative
        if (request.getInitialDeposit().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Initial deposit cannot be negative");
        }

        // Parse account type
        Account.AccountType accountType;
        try {
            accountType = Account.AccountType.valueOf(request.getAccountType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid account type. Must be CHECKING, SAVINGS, or BUSINESS");
        }

        // Create new account
        Account account = new Account(
                request.getClientId(),
                iban,
                request.getInitialDeposit(),
                accountType
        );

        return accountRepository.save(account);
    }

    public Account getAccountById(Integer id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Account not found with id: " + id));
    }

    public java.util.List<Account> getAccountsByClientId(String clientId) {
        clientRepository.findById(clientId)
                .orElseThrow(() -> new BusinessException("Client not found with id: " + clientId));

        return accountRepository.findByClientId(clientId);
    }

    public java.util.List<Account> getAccounts(String clientId) {
        if (clientId == null || clientId.trim().isEmpty()) {
            return accountRepository.findAll();
        }

        return getAccountsByClientId(clientId.trim());
    }

    @Transactional
    public void closeAccount(Integer accountId, String clientId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException("Account not found with id: " + accountId));

        if (!account.getClientId().equals(clientId)) {
            throw new BusinessException("Account does not belong to the specified client");
        }

        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException("Cannot close account with non-zero balance");
        }

        account.setStatus(Account.AccountStatus.CLOSED);
        accountRepository.save(account);
    }

    public void validateAccountIsActive(Integer accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException("Account not found with id: " + accountId));

        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new BusinessException("Account is closed. Cannot perform operation on closed account.");
        }
    }

    @Transactional
    public Account updateAccount(Integer accountId, AccountUpdateRequestDTO request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException("Account not found with id: " + accountId));

        if (request.getBalance() == null || request.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Balance cannot be negative");
        }

        account.setBalance(request.getBalance());
        return accountRepository.save(account);
    }

    @Transactional
    public Account depositToAccount(Integer accountId, BigDecimal amount) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException("Account not found with id: " + accountId));

        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new BusinessException("Only active accounts can receive deposits");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Deposit amount must be positive");
        }

        account.setBalance(account.getBalance().add(amount));
        return accountRepository.save(account);
    }

}
