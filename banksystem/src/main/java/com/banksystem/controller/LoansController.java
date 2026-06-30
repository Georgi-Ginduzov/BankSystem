package com.banksystem.controller;

import com.banksystem.dto.LoanApplicationFrontendDTO;
import com.banksystem.dto.LoanReviewRequestDTO;
import com.banksystem.dto.LoanSummaryDto;
import com.banksystem.dto.RepaymentUpdateRequestDTO;
import com.banksystem.dto.LoanUpdateRequestDTO;
import com.banksystem.dto.response.ErrorResponseDTO;
import com.banksystem.exception.LoanTypeCriteriaMismatchException;
import com.banksystem.exception.ResourceNotFoundException;
import com.banksystem.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.banksystem.dto.InstallmentPaymentDTO;
import com.banksystem.exception.BusinessException;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
public class LoansController
{
    private final LoanService loanService;

    public LoansController(LoanService loanService)
    {
        this.loanService = loanService;
    }

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

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LOAN_OFFICER', 'MANAGER')")
    public ResponseEntity<List<LoanSummaryDto>> getAllLoans(@RequestParam(defaultValue = "") String status) {
        return ResponseEntity.ok(loanService.getAllLoans(status));
    }

    @GetMapping("{id}")
    public ResponseEntity<LoanSummaryDto> GetLoanById(@PathVariable int id)
    {
        var loan = loanService.getLoanById(id);

        return ResponseEntity.ok().body(loan);
    }

    @PatchMapping("{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'LOAN_OFFICER', 'MANAGER')")
    public ResponseEntity<Object> reviewLoan(
            @PathVariable int id,
            @Valid @RequestBody LoanReviewRequestDTO request,
            Authentication authentication
    ) {
        try {
            return ResponseEntity.ok(loanService.reviewLoan(id, authentication.getName(), request));
        } catch (ResourceNotFoundException | BusinessException e) {
            return ResponseEntity.status(400).body(new ErrorResponseDTO(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponseDTO("Unexpected error: " + e.getMessage()));
        }
    }

    @PatchMapping("{id}/approve")
    public ResponseEntity<Object> approveLoan(@PathVariable int id, @RequestParam Integer employeeId)
    {
        try
        {
            loanService.approveLoan(id, employeeId);
        }
        catch (ResourceNotFoundException e)
        {
            return ResponseEntity
                    .status(404)
                    .body(new ErrorResponseDTO(e.getMessage()));
        }
        catch (IllegalStateException e)
        {
            return ResponseEntity
                    .status(409)
                    .body(new ErrorResponseDTO(e.getMessage()));
        }

        return ResponseEntity.ok().build();
    }

    @PatchMapping("{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LOAN_OFFICER', 'MANAGER')")
    public ResponseEntity<Object> updateLoan(@PathVariable int id, @RequestBody LoanUpdateRequestDTO request) {
        try {
            return ResponseEntity.ok(loanService.updateLoan(id, request));
        } catch (ResourceNotFoundException | BusinessException e) {
            return ResponseEntity.status(400).body(new ErrorResponseDTO(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponseDTO("Unexpected error: " + e.getMessage()));
        }
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

    @PatchMapping("{loanId}/installments/{monthNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LOAN_OFFICER', 'MANAGER')")
    public ResponseEntity<Object> updateInstallment(
            @PathVariable Integer loanId,
            @PathVariable Integer monthNumber,
            @Valid @RequestBody RepaymentUpdateRequestDTO request
    ) {
        try {
            loanService.updateInstallment(loanId, monthNumber, request);
            return ResponseEntity.ok().build();
        } catch (ResourceNotFoundException | BusinessException e) {
            return ResponseEntity.status(400).body(new ErrorResponseDTO(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponseDTO("Unexpected error: " + e.getMessage()));
        }
    }


}
