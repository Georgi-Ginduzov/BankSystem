package com.banksystem.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class AccountFrontendDTO {
    private String customerId;
    private String accountType; // CHECKING, SAVINGS, BUSINESS
    private String currency; // BGN, EUR, USD
    private BigDecimal initialDeposit;
    private String branch;
    private Boolean overdraftEnabled;
}