package com.example.rewards.dto;

import java.util.List;

/**
 * Reports a customer's monthly rewards and their total over the requested period.
 *
 * @param customerId stable customer identifier
 * @param customerName customer's display name
 * @param monthlyRewards chronological months containing purchases, including zero-point purchases
 * @param totalPoints sum of the reported monthly points
 */
public record CustomerRewards(
    long customerId, String customerName, List<MonthlyRewards> monthlyRewards, long totalPoints) {}
