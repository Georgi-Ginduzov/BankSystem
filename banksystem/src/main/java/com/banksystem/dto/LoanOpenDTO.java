package com.banksystem.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
public class LoanOpenDTO {
    private String applicationId;
    private String customerId;
    private String loanType;
    private BigDecimal principal;
    private Integer periodMonths;
    private BigDecimal interestRate;
    private LocalDate startDate;
}