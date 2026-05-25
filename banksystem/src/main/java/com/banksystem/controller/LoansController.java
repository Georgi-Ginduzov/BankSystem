package com.banksystem.controller;

import com.banksystem.dto.LoanApplicationDTO;
import com.banksystem.dto.LoanSummaryDto;
import com.banksystem.dto.response.ErrorResponseDTO;
import com.banksystem.exception.LoanTypeCriteriaMismatchException;
import com.banksystem.exception.ResourceNotFoundException;
import com.banksystem.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/loans")
@CrossOrigin(origins = "http://localhost:3000")
public class LoansController
{
    private final LoanService loanService;

    public LoansController(LoanService loanService)
    {
        this.loanService = loanService;
    }

    // ------------------------------------- Loan applications ------------------------------------------------
    @PostMapping("/apply")
    public ResponseEntity<Object> applyForLoan(@Valid @RequestBody LoanApplicationDTO request)
    {
        if(request.getStartDate().isBefore(LocalDate.now()))
        {
            return ResponseEntity
                    .status(400)
                    .body(new ErrorResponseDTO("Loan start date cannot be before current date"));
        }

        try
        {
            loanService.applyForLoan(request);
        }
        catch(ResourceNotFoundException | LoanTypeCriteriaMismatchException e)
        {
            return ResponseEntity
                    .status(400)
                    .body(new ErrorResponseDTO(e.getMessage()));
        }
        catch (Exception e)
        {
            // Could log full exception
            return ResponseEntity
                    .status(500)
                    .body(new ErrorResponseDTO("Unexpected exception: " + e.getMessage()));
        }

        return ResponseEntity.ok().build();
    }

    // ------------------------------------- Loans Read/Update ------------------------------------------------
    @GetMapping("{id}")
    public ResponseEntity<LoanSummaryDto> GetLoanById(@PathVariable int id)
    {
        var loan = loanService.getLoanById(id);

        return ResponseEntity.ok().body(loan);
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

}