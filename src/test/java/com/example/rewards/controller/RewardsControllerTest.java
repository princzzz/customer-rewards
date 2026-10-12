package com.example.rewards.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.rewards.service.RewardsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Verifies error serialization when the reporting service encounters an infrastructure failure. */
@WebMvcTest(RewardsController.class)
class RewardsControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private RewardsService service;

  /** Returns a sanitized problem response without leaking database connection details. */
  @Test
  void returnsSafeServerErrorWhenDatabaseIsUnavailable() throws Exception {
    when(service.getRewards(any(), any()))
        .thenThrow(new DataAccessResourceFailureException("Sensitive database connection details"));

    mockMvc
        .perform(
            get("/api/rewards").param("startDate", "2025-12-01").param("endDate", "2026-02-28"))
        .andExpect(status().isInternalServerError())
        .andExpect(content().contentType("application/problem+json"))
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(
            jsonPath("$.detail").value("Unable to calculate rewards. Please try again later."))
        .andExpect(jsonPath("$.stackTrace").doesNotExist());
  }
}
