package com.banksystem.exception;

public class TermLimitExceededException extends LoanValidationException {

  public TermLimitExceededException(int requestedTerm, int maxTerm) {
    super(String.format("Requested term of %d months exceeds maximum allowed term of %d months",
            requestedTerm, maxTerm));
  }

  public TermLimitExceededException(String message) {
    super(message);
  }
}