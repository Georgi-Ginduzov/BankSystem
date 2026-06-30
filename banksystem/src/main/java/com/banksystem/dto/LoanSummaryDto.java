package com.banksystem.dto;

import com.banksystem.model.*;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @Builder
public class LoanSummaryDto
{
    @Positive
    private Integer id;

    private String clientId;

    private String loanTypeName;

    @Positive
    private BigDecimal initialAmount;

    @Positive
    private Integer employeeId;

    @Positive
    private Integer termMonths;

    private LocalDate startDate;

    private Loan.LoanStatus status;

    @Positive
    private BigDecimal monthlyPayment;

    @PositiveOrZero
    private BigDecimal remainingAmount;

    @PositiveOrZero
    private Integer paidInstallments = 0;

    private Integer settlementAccountId;

    private String settlementAccountIban;
}
