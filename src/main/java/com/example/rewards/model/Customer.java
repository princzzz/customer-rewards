package com.example.rewards.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Represents a customer whose purchases can earn reward points. */
@Entity
@Table(name = "customers")
public class Customer {

  @Id private Long id;

  @Column(nullable = false, length = 100)
  private String name;

  /** Allows JPA to instantiate a persisted customer. */
  protected Customer() {}

  /** Creates a customer with the supplied persistent identifier and display name. */
  public Customer(Long id, String name) {
    this.id = id;
    this.name = name;
  }

  /** Returns the stable customer identifier. */
  public Long getId() {
    return id;
  }

  /** Returns the customer's display name. */
  public String getName() {
    return name;
  }
}
