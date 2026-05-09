package com.banksystem.controller;

import com.banksystem.dto.InstallmentPaymentDTO;
import com.banksystem.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/loans")
@CrossOrigin(origins = "http://localhost:3000")
public class LoanController {

    @Autowired
    private LoanService loanService;

    @PostMapping("/payments")
    public ResponseEntity<Void> markInstallmentPaid(@Valid @RequestBody InstallmentPaymentDTO request) {
        loanService.markInstallmentAsPaid(request);
        return ResponseEntity.ok().build();
    }
}