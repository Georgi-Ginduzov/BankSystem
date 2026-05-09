package com.banksystem.controller;

import com.banksystem.dto.AccountRequestDTO;
import com.banksystem.model.Account;
import com.banksystem.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
@CrossOrigin(origins = "http://localhost:3000")
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

    @DeleteMapping("/{accountId}/clients/{clientId}")
    public ResponseEntity<Void> closeAccount(@PathVariable Integer accountId, @PathVariable String clientId) {
        accountService.closeAccount(accountId, clientId);
        return ResponseEntity.noContent().build();
    }
}