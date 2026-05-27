package com.banksystem.service;

import com.banksystem.dto.AccountRequestDTO;
import com.banksystem.exception.BusinessException;
import com.banksystem.model.Account;
import com.banksystem.model.Client;
import com.banksystem.repository.AccountRepository;
import com.banksystem.repository.ClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final ClientRepository clientRepository;

    @Autowired
    private ClientRepository clientRepository;

    public AccountService(AccountRepository accountRepository, ClientRepository clientRepository) {
        this.accountRepository = accountRepository;
        this.clientRepository = clientRepository;
    }

    @Transactional
    public Account openAccount(AccountRequestDTO request) {
        // Validate client exists
        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new BusinessException("Client not found with id: " + request.getClientId()));

        // Check for duplicate IBAN
        if (accountRepository.existsByIban(request.getIban())) {
            throw new BusinessException("Account with IBAN " + request.getIban() + " already exists");
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
                request.getIban(),
                request.getInitialDeposit(),
                accountType
        );

        return accountRepository.save(account);
    }

    public Account getAccountById(Integer id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Account not found with id: " + id));
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

}