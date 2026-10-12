package com.example.rewards.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Verifies reward tiers, fractional-dollar handling, and invalid calculator inputs. */
class RewardPointsCalculatorTest {

  private final RewardPointsCalculator calculator = new RewardPointsCalculator();

  /** Checks both thresholds and representative purchases, including the assignment example. */
  @ParameterizedTest
  @CsvSource({
    "0, 0",
    "25, 0",
    "49.99, 0",
    "50, 0",
    "50.99, 0",
    "51, 1",
    "75, 25",
    "99.99, 49",
    "100, 50",
    "100.99, 50",
    "101, 52",
    "120, 90",
    "120.99, 90",
    "200, 250",
    "9999999999.99, 19999999848"
  })
  void calculatesPointsPerTransaction(String amount, long expectedPoints) {
    assertEquals(expectedPoints, calculator.calculate(new BigDecimal(amount)));
  }

  /** Rejects negative amounts rather than silently producing incorrect rewards. */
  @Test
  void rejectsNegativeAmounts() {
    assertThrows(
        IllegalArgumentException.class, () -> calculator.calculate(new BigDecimal("-0.01")));
  }

  /** Rejects missing amounts explicitly. */
  @Test
  void rejectsNullAmounts() {
    assertThrows(IllegalArgumentException.class, () -> calculator.calculate(null));
  }
}
