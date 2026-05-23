package com.banksystem.controller;

import com.banksystem.dto.LoanApplicationDTO;
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
}