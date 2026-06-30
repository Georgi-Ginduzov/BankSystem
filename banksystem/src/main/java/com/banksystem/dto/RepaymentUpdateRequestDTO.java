package com.banksystem.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RepaymentUpdateRequestDTO {

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    @NotNull(message = "Expected payment amount is required")
    @Positive(message = "Expected payment amount must be positive")
    private BigDecimal expectedPaymentAmount;

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public BigDecimal getExpectedPaymentAmount() {
        return expectedPaymentAmount;
    }

    public void setExpectedPaymentAmount(BigDecimal expectedPaymentAmount) {
        this.expectedPaymentAmount = expectedPaymentAmount;
    }
}
