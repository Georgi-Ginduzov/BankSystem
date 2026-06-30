package com.banksystem.dto;

import com.banksystem.model.Loan;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class LoanUpdateRequestDTO {
    private BigDecimal initialAmount;
    private BigDecimal remainingAmount;
    private Integer termMonths;
    private Loan.LoanStatus status;
}
