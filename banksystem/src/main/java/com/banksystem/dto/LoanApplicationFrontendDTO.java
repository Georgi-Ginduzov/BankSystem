package com.banksystem.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class LoanApplicationFrontendDTO {

    private String clientId;

    private String customerName;

    private String email;

    @NotBlank(message = "Phone is required")
    private String phone;

    @NotBlank(message = "Loan type is required")
    private String loanType;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    private Integer repaymentAccountId;

    @NotNull(message = "Period months is required")
    @Positive(message = "Period months must be positive")
    private Integer periodMonths;

    @NotNull(message = "Monthly income is required")
    @Positive(message = "Monthly income must be positive")
    private BigDecimal monthlyIncome;

    @NotBlank(message = "Purpose is required")
    private String purpose;
}
