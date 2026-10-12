package com.example.rewards.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/** Calculates whole-dollar reward points for one purchase without combining transactions. */
@Component
public class RewardPointsCalculator {

  private static final BigDecimal LOWER_THRESHOLD = new BigDecimal("50");
  private static final BigDecimal UPPER_THRESHOLD = new BigDecimal("100");

  /**
   * Awards one point per whole dollar above 50 through 100 and two above 100.
   *
   * @param amount nonnegative purchase amount in dollars
   * @return points earned by this transaction; fractional dollars earn no points
   * @throws IllegalArgumentException if the amount is null or negative
   * @throws ArithmeticException if the result cannot be represented as a long
   */
  public long calculate(BigDecimal amount) {
    if (amount == null || amount.signum() < 0) {
      throw new IllegalArgumentException("Purchase amount must be nonnegative.");
    }
    if (amount.compareTo(LOWER_THRESHOLD) <= 0) {
      return 0;
    }
    if (amount.compareTo(UPPER_THRESHOLD) <= 0) {
      return wholeDollars(amount.subtract(LOWER_THRESHOLD));
    }
    return Math.addExact(50, Math.multiplyExact(2, wholeDollars(amount.subtract(UPPER_THRESHOLD))));
  }

  /** Converts a nonnegative dollar amount to its whole-dollar component without rounding up. */
  private long wholeDollars(BigDecimal amount) {
    return amount.setScale(0, RoundingMode.DOWN).longValueExact();
  }
}
