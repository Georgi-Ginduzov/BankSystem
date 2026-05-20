package com.banksystem.exception;

public class LoanValidationException extends RuntimeException {

  public LoanValidationException(String message) {
    super(message);
  }

  public LoanValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}