package com.banksystem.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter
public class UpdateLoanDTO {
    private BigDecimal principal;
    private Integer periodMonths;
    private BigDecimal interestRate;
    private String status; // ACTIVE, PAUSED, CLOSED, DEFAULTED
}