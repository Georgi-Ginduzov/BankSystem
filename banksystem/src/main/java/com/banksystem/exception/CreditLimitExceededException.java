package com.banksystem.exception;

import java.math.BigDecimal;

public class CreditLimitExceededException extends LoanValidationException {

  public CreditLimitExceededException(BigDecimal requestedAmount, BigDecimal maxAmount) {
    super(String.format("Requested amount %.2f exceeds maximum allowed amount of %.2f",
            requestedAmount, maxAmount));
  }

  public CreditLimitExceededException(String message) {
    super(message);
  }
}