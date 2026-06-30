package com.banksystem.controller;

import com.banksystem.dto.ClientRequestDTO;
import com.banksystem.dto.ClientLoanDetailsDTO;
import com.banksystem.dto.LoanSummaryDto;
import com.banksystem.model.Account;
import com.banksystem.model.Client;
import com.banksystem.service.ClientService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    @Autowired
    private ClientService clientService;

    @PostMapping
    public ResponseEntity<Client> addClient(@Valid @RequestBody ClientRequestDTO request) {
        Client client = clientService.addClient(request);
        return new ResponseEntity<>(client, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Client> getClient(@PathVariable String id) {
        Client client = clientService.getClientById(id);
        return ResponseEntity.ok(client);
    }

    @GetMapping("/{id}/loans")
    public ResponseEntity<List<LoanSummaryDto>> getLoans(@PathVariable @NotBlank @NotNull String id)
    {
        return ResponseEntity.ok(clientService.getLoansByClientId(id));
    }

    @GetMapping("/{id}/loans/{loanId}")
    public ResponseEntity<ClientLoanDetailsDTO> getLoanDetails(
            @PathVariable @NotBlank @NotNull String id,
            @PathVariable Integer loanId
    )
    {
        return ResponseEntity.ok(clientService.getLoanDetailsByClientId(id, loanId));
    }

    @GetMapping("/{id}/accounts")
    public ResponseEntity<List<Account>> getAccounts(@PathVariable @NotBlank @NotNull String id)
    {
        return ResponseEntity.ok(clientService.getAccountsByClientId(id));
    }
}
