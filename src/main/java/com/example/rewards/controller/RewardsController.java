package com.example.rewards.controller;

import com.example.rewards.dto.RewardsReport;
import com.example.rewards.service.RewardsService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Exposes read-only reward reports as a REST resource. */
@RestController
@RequestMapping("/api/rewards")
public class RewardsController {

  private final RewardsService service;

  /** Connects the HTTP resource to the reward reporting service. */
  public RewardsController(RewardsService service) {
    this.service = service;
  }

  /**
   * Returns customer rewards for an inclusive ISO-date window or the default three-month period.
   *
   * @param startDate optional ISO first date; must be paired with endDate
   * @param endDate optional ISO last date; must be paired with startDate
   * @return reporting dates, customer identities, chronological monthly points, and totals
   */
  @GetMapping
  public RewardsReport getRewards(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate endDate) {
    return service.getRewards(startDate, endDate);
  }
}
