package com.banksystem.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class AccountRequestDTO {

    @NotBlank(message = "Client ID is required")
    private String clientId;

    @Pattern(regexp = "^$|^[A-Z]{2}[0-9A-Z]{20,30}$", message = "Invalid IBAN format")
    private String iban;

    @NotNull(message = "Initial deposit is required")
    @PositiveOrZero(message = "Initial deposit cannot be negative")
    private BigDecimal initialDeposit;

    @NotBlank(message = "Account type is required")
    private String accountType; // CHECKING, SAVINGS, BUSINESS

    // Getters and Setters
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }

    public BigDecimal getInitialDeposit() { return initialDeposit; }
    public void setInitialDeposit(BigDecimal initialDeposit) { this.initialDeposit = initialDeposit; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
}
