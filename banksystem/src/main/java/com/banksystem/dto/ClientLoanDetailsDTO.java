package com.banksystem.dto;

import com.banksystem.model.Loan;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class ClientLoanDetailsDTO {
    private Integer id;
    private String loanTypeName;
    private Loan.LoanStatus status;
    private BigDecimal initialAmount;
    private BigDecimal remainingAmount;
    private BigDecimal monthlyPayment;
    private Integer termMonths;
    private Integer paidInstallments;
    private LocalDate startDate;
    private Integer settlementAccountId;
    private String settlementAccountIban;
    private List<RepaymentPlanItemDTO> repaymentPlan;
}
