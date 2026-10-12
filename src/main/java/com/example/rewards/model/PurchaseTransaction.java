package com.example.rewards.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Stores one dated purchase; rewards are calculated separately for every transaction. */
@Entity
@Table(name = "purchase_transactions")
public class PurchaseTransaction {

  @Id private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_id", nullable = false)
  private Customer customer;

  @Column(name = "purchased_on", nullable = false)
  private LocalDate purchasedOn;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal amount;

  /** Allows JPA to instantiate a persisted purchase. */
  protected PurchaseTransaction() {}

  /** Creates a purchase from its identifier, owner, business date, and dollar amount. */
  public PurchaseTransaction(Long id, Customer customer, LocalDate purchasedOn, BigDecimal amount) {
    this.id = id;
    this.customer = customer;
    this.purchasedOn = purchasedOn;
    this.amount = amount;
  }

  /** Returns the stable transaction identifier. */
  public Long getId() {
    return id;
  }

  /** Returns the customer who made the purchase. */
  public Customer getCustomer() {
    return customer;
  }

  /** Returns the date used to assign this purchase to a reporting month. */
  public LocalDate getPurchasedOn() {
    return purchasedOn;
  }

  /** Returns the purchase amount without floating-point conversion. */
  public BigDecimal getAmount() {
    return amount;
  }
}
