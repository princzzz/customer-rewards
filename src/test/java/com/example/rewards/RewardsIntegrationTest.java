package com.example.rewards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.rewards.dto.CustomerRewards;
import com.example.rewards.dto.MonthlyRewards;
import com.example.rewards.dto.RewardsReport;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

/** Tests real HTTP requests through Spring Boot, JPA, and an SQL-initialized H2 database. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.datasource.url=jdbc:h2:mem:rewards-integration;DB_CLOSE_DELAY=-1",
      "spring.sql.init.data-locations=classpath:integration-data.sql"
    })
@Import(RewardsIntegrationTest.FixedClockConfiguration.class)
class RewardsIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;

  @Autowired private JdbcTemplate jdbcTemplate;

  /** Verifies all monthly totals, multiple customers, endpoints, and out-of-window exclusions. */
  @Test
  void returnsExactMonthlyAndTotalRewardsAcrossYearBoundary() {
    ResponseEntity<RewardsReport> response =
        restTemplate.getForEntity(
            "/api/rewards?startDate=2025-12-01&endDate=2026-02-28", RewardsReport.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(expectedReport(), response.getBody());
  }

  /** Checks the omitted-parameter path against an injected clock rather than the machine date. */
  @Test
  void returnsDynamicThreeMonthDefault() {
    ResponseEntity<RewardsReport> response =
        restTemplate.getForEntity("/api/rewards", RewardsReport.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(expectedReport(), response.getBody());
  }

  /** Ensures range boundaries are inclusive and that partial-month requests remain exact. */
  @Test
  void returnsOnlyTransactionsOnSingleRequestedDay() {
    ResponseEntity<RewardsReport> response =
        restTemplate.getForEntity(
            "/api/rewards?startDate=2026-02-28&endDate=2026-02-28", RewardsReport.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(
        new RewardsReport(
            LocalDate.of(2026, 2, 28),
            LocalDate.of(2026, 2, 28),
            List.of(
                new CustomerRewards(
                    1, "Alice Johnson", List.of(new MonthlyRewards("2026-02", 90)), 90))),
        response.getBody());
  }

  /** Returns a successful empty report rather than treating a data-free period as an error. */
  @Test
  void returnsEmptyReportWhenNoPurchasesMatch() {
    ResponseEntity<RewardsReport> response =
        restTemplate.getForEntity(
            "/api/rewards?startDate=2020-01-01&endDate=2020-03-31", RewardsReport.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(
        new RewardsReport(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 3, 31), List.of()),
        response.getBody());
  }

  /** Exercises reversed ranges, missing dates, malformed dates, and impossible calendar dates. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "?startDate=2026-03-01&endDate=2026-02-28",
        "?startDate=2025-12-01",
        "?endDate=2026-02-28",
        "?startDate=not-a-date&endDate=2026-02-28",
        "?startDate=2026-02-30&endDate=2026-03-31",
        "?startDate=2026-13-01&endDate=2026-03-31",
        "?startDate=2025-12-01&endDate=invalid",
        "?startDate=&endDate=2026-02-28"
      })
  void rejectsInvalidQueryParameters(String query) {
    ResponseEntity<JsonNode> response =
        restTemplate.getForEntity("/api/rewards" + query, JsonNode.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(400, response.getBody().get("status").asInt());
    assertFalse(response.getBody().get("detail").asText().isBlank());
    assertEquals("application/problem+json", response.getHeaders().getContentType().toString());
  }

  /** Confirms invalid purchase amounts are blocked at the persistence boundary as well. */
  @ParameterizedTest
  @CsvSource(
      value = {"-0.01", "NULL"},
      nullValues = "NULL")
  void rejectsNegativeAndMissingAmountsInDatabase(String amount) {
    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            jdbcTemplate.update(
                "INSERT INTO purchase_transactions (id, customer_id, purchased_on, amount)"
                    + " VALUES (100, 1, '2026-01-01', ?)",
                amount));
  }

  /** Confirms a purchase cannot be attached to a nonexistent customer. */
  @Test
  void rejectsUnknownCustomerInDatabase() {
    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            jdbcTemplate.update(
                "INSERT INTO purchase_transactions (id, customer_id, purchased_on, amount)"
                    + " VALUES (101, 999, '2026-01-01', 120)"));
  }

  /** Defines complete expected totals independently of the production aggregation algorithm. */
  private RewardsReport expectedReport() {
    return new RewardsReport(
        LocalDate.of(2025, 12, 1),
        LocalDate.of(2026, 2, 28),
        List.of(
            new CustomerRewards(
                1,
                "Alice Johnson",
                List.of(
                    new MonthlyRewards("2025-12", 115),
                    new MonthlyRewards("2026-01", 250),
                    new MonthlyRewards("2026-02", 140)),
                505),
            new CustomerRewards(
                2,
                "Bob Smith",
                List.of(
                    new MonthlyRewards("2025-12", 52),
                    new MonthlyRewards("2026-01", 160),
                    new MonthlyRewards("2026-02", 390)),
                602),
            new CustomerRewards(
                3,
                "Carol Davis",
                List.of(
                    new MonthlyRewards("2025-12", 0),
                    new MonthlyRewards("2026-01", 50),
                    new MonthlyRewards("2026-02", 26)),
                76),
            new CustomerRewards(5, "Daniel Lee", List.of(new MonthlyRewards("2025-12", 20)), 20)));
  }

  /** Provides a deterministic reporting clock without replacing SQL persistence or HTTP layers. */
  @TestConfiguration(proxyBeanMethods = false)
  static class FixedClockConfiguration {

    /** Supplies February 2026 as the current month for default-window integration assertions. */
    @Bean
    @Primary
    Clock fixedReportingClock() {
      return Clock.fixed(Instant.parse("2026-02-15T12:00:00Z"), ZoneOffset.UTC);
    }
  }
}
