package com.banksystem.controller;

import com.banksystem.dto.UpdateLoanDTO;
import com.banksystem.dto.LoanApplicationFrontendDTO;
import com.banksystem.dto.LoanOpenDTO;
import com.banksystem.dto.LoanSummaryDto;
import com.banksystem.dto.response.ErrorResponseDTO;
import com.banksystem.exception.LoanTypeCriteriaMismatchException;
import com.banksystem.exception.ResourceNotFoundException;
import com.banksystem.model.Loan;
import com.banksystem.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.banksystem.dto.InstallmentPaymentDTO;
import com.banksystem.exception.BusinessException;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/loans")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class LoansController
{
    private final LoanService loanService;

    public LoansController(LoanService loanService)
    {
        this.loanService = loanService;
    }

    // ------------------------------------- Loan applications ------------------------------------------------
    @PostMapping("/apply")
    public ResponseEntity<Object> applyForLoan(@Valid @RequestBody LoanApplicationFrontendDTO request) {
        try {
            loanService.applyForLoan(request);
            return ResponseEntity.ok().build();
        } catch (ResourceNotFoundException | LoanTypeCriteriaMismatchException | BusinessException e) {
            return ResponseEntity.status(400).body(new ErrorResponseDTO(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponseDTO("Unexpected exception: " + e.getMessage()));
        }
    }

    // ------------------------------------- Loans Read/Update ------------------------------------------------
    @GetMapping("{id}")
    public ResponseEntity<LoanSummaryDto> GetLoanById(@PathVariable int id)
    {
        var loan = loanService.getLoanById(id);

        return ResponseEntity.ok().body(loan);
    }

    @PatchMapping("{id}/approve")
    public ResponseEntity<Object> approveLoan(@PathVariable int id,
                                              @RequestParam(required = false) Integer employeeId) {
        try {
            loanService.approveLoan(id, employeeId);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(new ErrorResponseDTO(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(new ErrorResponseDTO(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponseDTO("Unexpected error: " + e.getMessage()));
        }
        return ResponseEntity.ok().build();
    }

    // ------------------------------------- Loans Mark Installment as Paid ------------------------------------------------
    @PostMapping("/payments")
    public ResponseEntity<Object> markInstallmentPaid(@Valid @RequestBody InstallmentPaymentDTO request) {
        try {
            loanService.markInstallmentAsPaid(request);
            return ResponseEntity.ok().build();
        } catch (ResourceNotFoundException | BusinessException e) {
            return ResponseEntity.status(400).body(new ErrorResponseDTO(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponseDTO("Unexpected error: " + e.getMessage()));
        }
    }

    @PostMapping("/open")
    public ResponseEntity<Object> openLoanContract(@RequestBody LoanOpenDTO request) {
        try {
            Loan loan = loanService.openLoanContract(request);
            return ResponseEntity.ok(loan);
        } catch (ResourceNotFoundException | BusinessException e) {
            return ResponseEntity.status(400).body(new ErrorResponseDTO(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponseDTO("Unexpected error: " + e.getMessage()));
        }
    }


    @PatchMapping("/{id}")
    public ResponseEntity<Object> updateLoan(@PathVariable Integer id, @RequestBody UpdateLoanDTO request) {
        try {
            loanService.updateLoan(id, request);
            return ResponseEntity.ok().build();
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(new ErrorResponseDTO(e.getMessage()));
        } catch (BusinessException e) {
            return ResponseEntity.status(400).body(new ErrorResponseDTO(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponseDTO("Unexpected error: " + e.getMessage()));
        }
    }

}