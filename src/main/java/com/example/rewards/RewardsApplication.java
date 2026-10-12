package com.example.rewards;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/** Starts the SQL-backed customer rewards REST application. */
@SpringBootApplication
public class RewardsApplication {

  /** Launches Spring Boot with the supplied command-line arguments. */
  public static void main(String[] args) {
    SpringApplication.run(RewardsApplication.class, args);
  }

  /** Supplies an injectable UTC clock for deterministic reporting-period defaults. */
  @Bean
  public Clock reportingClock() {
    return Clock.systemUTC();
  }
}
