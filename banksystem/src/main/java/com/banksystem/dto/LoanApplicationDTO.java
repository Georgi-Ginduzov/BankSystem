package com.banksystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class LoanApplicationDTO
{
    @NotNull(message = "loanType is a required field")
    @NotBlank(message = "loanType could not be blank")
    private String loanType;

    @NotNull(message = "clientId is a required field")
    @NotBlank(message = "clientId could not be blank")
    private String clientId;

    @NotNull(message = "amount is required")
    @Positive(message = "Desired loan amount could not be equal or bellow 0")
    private BigDecimal amount;

    @NotNull(message = "termMonths is required")
    @Positive(message = "loan's termMonths could not be equal or bellow 0")
    private Integer termMonths;

    @NotNull(message = "startDate is a required field")
    private LocalDate startDate;
}
