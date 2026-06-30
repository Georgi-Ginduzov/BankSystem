package com.banksystem.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class InstallmentPaymentDTO {

    public enum OverpaymentStrategy {
        REDUCE_TERM,
        REDUCE_INSTALLMENT
    }

    @NotNull(message = "Loan ID is required")
    private Integer loanId;

    @NotBlank(message = "Client ID is required")
    private String clientId;

    @NotNull(message = "Month number is required")
    @Positive(message = "Month number must be positive")
    private Integer monthNumber;

    @NotNull(message = "Payment amount is required")
    @Positive(message = "Payment amount must be positive")
    private BigDecimal paymentAmount;

    private OverpaymentStrategy overpaymentStrategy;

    // Getters and Setters
    public Integer getLoanId() { return loanId; }
    public void setLoanId(Integer loanId) { this.loanId = loanId; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public Integer getMonthNumber() { return monthNumber; }
    public void setMonthNumber(Integer monthNumber) { this.monthNumber = monthNumber; }

    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }

    public OverpaymentStrategy getOverpaymentStrategy() { return overpaymentStrategy; }
    public void setOverpaymentStrategy(OverpaymentStrategy overpaymentStrategy) {
        this.overpaymentStrategy = overpaymentStrategy;
    }
}
