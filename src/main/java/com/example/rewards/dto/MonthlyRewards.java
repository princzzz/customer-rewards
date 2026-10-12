package com.example.rewards.dto;

/**
 * Exposes earned points for an actual calendar month, including its year.
 *
 * @param month ISO year-month such as 2026-01
 * @param points sum of independently calculated transaction rewards in that month
 */
public record MonthlyRewards(String month, long points) {}
