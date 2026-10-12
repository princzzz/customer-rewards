package com.example.rewards.service;

import com.example.rewards.dto.CustomerRewards;
import com.example.rewards.dto.MonthlyRewards;
import com.example.rewards.dto.RewardsReport;
import com.example.rewards.exception.InvalidDateRangeException;
import com.example.rewards.model.Customer;
import com.example.rewards.model.PurchaseTransaction;
import com.example.rewards.repository.PurchaseTransactionRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Builds customer reward reports using purchase dates rather than hard-coded month names. */
@Service
public class RewardsService {

  private final PurchaseTransactionRepository repository;
  private final RewardPointsCalculator calculator;
  private final Clock clock;

  /** Injects persistence, the reward policy, and the clock used for default dates. */
  public RewardsService(
      PurchaseTransactionRepository repository, RewardPointsCalculator calculator, Clock clock) {
    this.repository = repository;
    this.calculator = calculator;
    this.clock = clock;
  }

  /**
   * Calculates rewards for an explicit window or the current and two preceding calendar months.
   *
   * @param startDate first included date, or null when both dates are omitted
   * @param endDate last included date, or null when both dates are omitted
   * @return effective dates and the monthly and total rewards for matching customers
   * @throws InvalidDateRangeException if only one date is supplied or the dates are reversed
   */
  @Transactional(readOnly = true)
  public RewardsReport getRewards(LocalDate startDate, LocalDate endDate) {
    if ((startDate == null) != (endDate == null)) {
      throw new InvalidDateRangeException("Provide both startDate and endDate, or omit both.");
    }
    if (startDate == null) {
      YearMonth currentMonth = YearMonth.now(clock);
      startDate = currentMonth.minusMonths(2).atDay(1);
      endDate = currentMonth.atEndOfMonth();
    }
    if (startDate.isAfter(endDate)) {
      throw new InvalidDateRangeException("startDate must be on or before endDate.");
    }

    Map<Long, CustomerAccumulator> customers = new LinkedHashMap<>();
    for (PurchaseTransaction purchase :
        repository.findByPurchasedOnBetweenOrderByCustomerIdAscPurchasedOnAscIdAsc(
            startDate, endDate)) {
      Customer customer = purchase.getCustomer();
      customers
          .computeIfAbsent(customer.getId(), ignored -> new CustomerAccumulator(customer))
          .add(
              YearMonth.from(purchase.getPurchasedOn()),
              calculator.calculate(purchase.getAmount()));
    }
    List<CustomerRewards> reports =
        customers.values().stream().map(CustomerAccumulator::toReport).toList();
    return new RewardsReport(startDate, endDate, reports);
  }

  /** Keeps each customer's chronological point totals isolated during report construction. */
  private static class CustomerAccumulator {

    private final Customer customer;
    private final Map<YearMonth, Long> monthlyPoints = new TreeMap<>();

    /** Associates the accumulator with exactly one customer. */
    private CustomerAccumulator(Customer customer) {
      this.customer = customer;
    }

    /** Adds one transaction's points to its actual year-month, retaining zero-point months. */
    private void add(YearMonth month, long points) {
      monthlyPoints.merge(month, points, Math::addExact);
    }

    /** Produces an immutable response and calculates the total from its monthly values. */
    private CustomerRewards toReport() {
      List<MonthlyRewards> months =
          monthlyPoints.entrySet().stream()
              .map(entry -> new MonthlyRewards(entry.getKey().toString(), entry.getValue()))
              .toList();
      long total = monthlyPoints.values().stream().reduce(0L, Math::addExact);
      return new CustomerRewards(customer.getId(), customer.getName(), months, total);
    }
  }
}
