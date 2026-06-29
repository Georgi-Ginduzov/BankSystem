package com.banksystem.controller;

import com.banksystem.dto.ClientFrontendDTO;
import com.banksystem.dto.ClientRequestDTO;
import com.banksystem.dto.LoanSummaryDto;
import com.banksystem.dto.response.ErrorResponseDTO;
import com.banksystem.exception.BusinessException;
import com.banksystem.exception.ResourceNotFoundException;
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
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class ClientController {

    @Autowired
    private ClientService clientService;

    // ---- FRONTEND ENDPOINTS ----

    @PostMapping
    public ResponseEntity<Object> addClient(@RequestBody ClientFrontendDTO request) {
        try {
            Client client = clientService.addClientFromFrontend(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(client);
        } catch (BusinessException e) {
            return ResponseEntity.status(400).body(new ErrorResponseDTO(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponseDTO("Unexpected error: " + e.getMessage()));
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Object> updateClient(@PathVariable String id, @RequestBody ClientFrontendDTO request) {
        try {
            Client client = clientService.updateClientFromFrontend(id, request);
            return ResponseEntity.ok(client);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(new ErrorResponseDTO(e.getMessage()));
        } catch (BusinessException e) {
            return ResponseEntity.status(400).body(new ErrorResponseDTO(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponseDTO("Unexpected error: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientFrontendDTO> getClient(@PathVariable String id) {
        ClientFrontendDTO dto = clientService.getClientFrontendById(id);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}/loans")
    public ResponseEntity<List<LoanSummaryDto>> getLoans(@PathVariable @NotBlank @NotNull String id) {
        return ResponseEntity.ok(clientService.getLoansByClientId(id));
    }

    // ---- INTERNAL / LEGACY  ----

    @PostMapping("/internal")
    public ResponseEntity<Client> addClientInternal(@Valid @RequestBody ClientRequestDTO request) {
        Client client = clientService.addClient(request);
        return new ResponseEntity<>(client, HttpStatus.CREATED);
    }
}