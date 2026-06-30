package com.banksystem.controller;

import com.banksystem.dto.AccountRequestDTO;
import com.banksystem.dto.AccountDepositRequestDTO;
import com.banksystem.dto.AccountUpdateRequestDTO;
import com.banksystem.dto.response.ErrorResponseDTO;
import com.banksystem.exception.BusinessException;
import com.banksystem.model.Account;
import com.banksystem.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @PostMapping
    public ResponseEntity<Account> openAccount(@Valid @RequestBody AccountRequestDTO request) {
        Account account = accountService.openAccount(request);
        return new ResponseEntity<>(account, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccount(@PathVariable Integer id) {
        Account account = accountService.getAccountById(id);
        return ResponseEntity.ok(account);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LOAN_OFFICER', 'MANAGER')")
    public ResponseEntity<Object> getAccounts(@RequestParam(required = false) String clientId) {
        try {
            return ResponseEntity.ok(accountService.getAccounts(clientId));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(new ErrorResponseDTO(e.getMessage()));
        }
    }

    @DeleteMapping("/{accountId}/clients/{clientId}")
    public ResponseEntity<Void> closeAccount(@PathVariable Integer accountId, @PathVariable String clientId) {
        accountService.closeAccount(accountId, clientId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{accountId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LOAN_OFFICER', 'MANAGER')")
    public ResponseEntity<Object> updateAccount(
            @PathVariable Integer accountId,
            @Valid @RequestBody AccountUpdateRequestDTO request
    ) {
        try {
            return ResponseEntity.ok(accountService.updateAccount(accountId, request));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(new ErrorResponseDTO(e.getMessage()));
        }
    }

    @PostMapping("/{accountId}/deposit")
    @PreAuthorize("hasAnyRole('ADMIN', 'LOAN_OFFICER', 'MANAGER')")
    public ResponseEntity<Object> depositToAccount(
            @PathVariable Integer accountId,
            @Valid @RequestBody AccountDepositRequestDTO request
    ) {
        try {
            return ResponseEntity.ok(accountService.depositToAccount(accountId, request.getAmount()));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(new ErrorResponseDTO(e.getMessage()));
        }
    }
}
