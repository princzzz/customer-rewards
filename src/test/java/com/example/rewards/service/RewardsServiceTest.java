package com.example.rewards.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.rewards.dto.CustomerRewards;
import com.example.rewards.dto.MonthlyRewards;
import com.example.rewards.dto.RewardsReport;
import com.example.rewards.exception.InvalidDateRangeException;
import com.example.rewards.model.Customer;
import com.example.rewards.model.PurchaseTransaction;
import com.example.rewards.repository.PurchaseTransactionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Isolates aggregation, date validation, and clock-dependent defaults from the database. */
@ExtendWith(MockitoExtension.class)
class RewardsServiceTest {

  @Mock private PurchaseTransactionRepository repository;

  private RewardsService service;

  /** Uses a fixed February clock so the default window crosses a calendar-year boundary. */
  @BeforeEach
  void setUp() {
    service =
        new RewardsService(
            repository,
            new RewardPointsCalculator(),
            Clock.fixed(Instant.parse("2026-02-15T12:00:00Z"), ZoneOffset.UTC));
  }

  /** Checks customer isolation, multiple purchases, chronological months, and exact totals. */
  @Test
  void aggregatesByCustomerAndActualYearMonth() {
    LocalDate start = LocalDate.of(2025, 12, 1);
    LocalDate end = LocalDate.of(2026, 2, 28);
    Customer alice = new Customer(1L, "Shared name");
    Customer bob = new Customer(2L, "Shared name");
    when(repository.findByPurchasedOnBetweenOrderByCustomerIdAscPurchasedOnAscIdAsc(start, end))
        .thenReturn(
            List.of(
                purchase(1, alice, "2026-02-28", "120.99"),
                purchase(2, alice, "2025-12-01", "120"),
                purchase(3, alice, "2026-01-08", "200"),
                purchase(4, alice, "2025-12-31", "75"),
                purchase(5, alice, "2026-01-20", "50"),
                purchase(6, alice, "2026-02-01", "100"),
                purchase(7, bob, "2025-12-15", "60"),
                purchase(8, bob, "2025-12-16", "60"),
                purchase(9, bob, "2026-01-01", "25")));

    assertEquals(
        new RewardsReport(
            start,
            end,
            List.of(
                new CustomerRewards(
                    1,
                    "Shared name",
                    List.of(
                        new MonthlyRewards("2025-12", 115),
                        new MonthlyRewards("2026-01", 250),
                        new MonthlyRewards("2026-02", 140)),
                    505),
                new CustomerRewards(
                    2,
                    "Shared name",
                    List.of(new MonthlyRewards("2025-12", 20), new MonthlyRewards("2026-01", 0)),
                    20))),
        service.getRewards(start, end));
  }

  /**
   * Resolves the default window dynamically and returns an empty report when no purchases match.
   */
  @Test
  void defaultsToCurrentAndPreviousTwoCalendarMonths() {
    LocalDate start = LocalDate.of(2025, 12, 1);
    LocalDate end = LocalDate.of(2026, 2, 28);
    when(repository.findByPurchasedOnBetweenOrderByCustomerIdAscPurchasedOnAscIdAsc(start, end))
        .thenReturn(List.of());

    assertEquals(new RewardsReport(start, end, List.of()), service.getRewards(null, null));
    verify(repository).findByPurchasedOnBetweenOrderByCustomerIdAscPurchasedOnAscIdAsc(start, end);
  }

  /** Demonstrates that default month lengths also work correctly during a leap year. */
  @Test
  void usesLeapYearMonthEnd() {
    service =
        new RewardsService(
            repository,
            new RewardPointsCalculator(),
            Clock.fixed(Instant.parse("2024-02-15T12:00:00Z"), ZoneOffset.UTC));
    LocalDate start = LocalDate.of(2023, 12, 1);
    LocalDate end = LocalDate.of(2024, 2, 29);
    when(repository.findByPurchasedOnBetweenOrderByCustomerIdAscPurchasedOnAscIdAsc(start, end))
        .thenReturn(List.of());

    assertEquals(new RewardsReport(start, end, List.of()), service.getRewards(null, null));
  }

  /** Allows an inclusive one-day window without expanding it to complete months. */
  @Test
  void acceptsSingleDayRange() {
    LocalDate date = LocalDate.of(2026, 1, 8);
    when(repository.findByPurchasedOnBetweenOrderByCustomerIdAscPurchasedOnAscIdAsc(date, date))
        .thenReturn(List.of(purchase(1, new Customer(1L, "Alice"), "2026-01-08", "200")));

    assertEquals(250, service.getRewards(date, date).customers().get(0).totalPoints());
  }

  /** Rejects reversed and incomplete ranges before any database access. */
  @ParameterizedTest
  @CsvSource(
      value = {"2026-03-01,2026-02-28", "NULL,2026-02-28", "2025-12-01,NULL"},
      nullValues = "NULL")
  void rejectsInvalidDateRanges(LocalDate start, LocalDate end) {
    assertThrows(InvalidDateRangeException.class, () -> service.getRewards(start, end));
    verifyNoInteractions(repository);
  }

  /** Creates a typed purchase for aggregation tests without hard-coding data in production. */
  private PurchaseTransaction purchase(long id, Customer customer, String date, String amount) {
    return new PurchaseTransaction(id, customer, LocalDate.parse(date), new BigDecimal(amount));
  }
}
