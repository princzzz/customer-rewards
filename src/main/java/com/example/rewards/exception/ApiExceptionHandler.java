package com.example.rewards.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Maps validation and unexpected failures to consistent RFC 9457 problem responses. */
@RestControllerAdvice
public class ApiExceptionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

  /** Returns a descriptive HTTP 400 response for incomplete or reversed reporting dates. */
  @ExceptionHandler(InvalidDateRangeException.class)
  public ProblemDetail handleInvalidRange(InvalidDateRangeException exception) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
  }

  /** Returns HTTP 400 when a query parameter cannot be parsed as an ISO calendar date. */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ProblemDetail handleInvalidDate(MethodArgumentTypeMismatchException exception) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST,
        exception.getName() + " must be a valid date in yyyy-MM-dd format.");
  }

  /** Logs unexpected failures while keeping implementation details out of HTTP 500 responses. */
  @ExceptionHandler(Exception.class)
  public ProblemDetail handleUnexpectedFailure(Exception exception) {
    LOGGER.error("Unable to calculate customer rewards", exception);
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR, "Unable to calculate rewards. Please try again later.");
  }
}
