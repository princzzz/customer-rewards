package com.example.rewards.exception;

/** Identifies an incomplete or reversed reporting window supplied by an API caller. */
public class InvalidDateRangeException extends RuntimeException {

  /** Creates a validation failure with a client-readable description. */
  public InvalidDateRangeException(String message) {
    super(message);
  }
}
