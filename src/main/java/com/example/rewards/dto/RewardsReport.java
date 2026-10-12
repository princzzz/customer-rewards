package com.example.rewards.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Defines the effective reporting window and rewards for customers with matching purchases.
 *
 * @param startDate inclusive first purchase date
 * @param endDate inclusive last purchase date
 * @param customers customer reports ordered by identifier
 */
public record RewardsReport(
    LocalDate startDate, LocalDate endDate, List<CustomerRewards> customers) {}
