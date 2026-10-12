package com.example.rewards.repository;

import com.example.rewards.model.PurchaseTransaction;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Retrieves purchases and their customers for reward reporting. */
public interface PurchaseTransactionRepository extends JpaRepository<PurchaseTransaction, Long> {

  /**
   * Loads purchases in an inclusive date range, fetching customers to avoid per-purchase queries.
   *
   * @param startDate first included purchase date
   * @param endDate last included purchase date
   * @return purchases ordered by customer, purchase date, and transaction identifier
   */
  @EntityGraph(attributePaths = "customer")
  List<PurchaseTransaction> findByPurchasedOnBetweenOrderByCustomerIdAscPurchasedOnAscIdAsc(
      LocalDate startDate, LocalDate endDate);
}
