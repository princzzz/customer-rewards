# Customer Rewards API

This Spring Boot project calculates reward points for each customer by month and shows the total
for the selected period. Purchase data comes from SQL scripts.

## Tools Used

- Java 17 and Spring Boot 3.5.7
- Maven 3.9 or newer
- Spring Data JPA and H2 database
- JUnit 5 and Mockito for tests
- Spotless for Java formatting

## Run the Project

Install Java and Maven. Set `JAVA_HOME` to your JDK and make sure Maven is on `PATH`.
Run these commands from the folder containing `pom.xml`:

```bash
mvn clean verify
mvn spring-boot:run
```

The API runs at `http://localhost:8080/api/rewards`.
If port 8080 is busy, use:

```bash
mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```

H2 runs in memory, so the sample data is loaded again each time the application starts.

## Reward Rules

- Up to $50: no points.
- Above $50 through $100: 1 point per whole dollar above $50.
- Above $100: 50 points, plus 2 points per whole dollar above $100.

A $120 purchase earns `50 + (2 * 20) = 90` points.
Points are calculated for each purchase before adding them together.
Only whole dollars count, so $120.99 also earns 90 points. Amounts use `BigDecimal` to avoid
rounding errors from floating-point numbers. Negative amounts are not allowed.

## API

**GET /api/rewards** returns each customer's monthly points and total.
Without dates, it uses the current month and the previous two months.

For a specific period, provide both dates in `yyyy-MM-dd` format:

```bash
curl "http://localhost:8080/api/rewards?startDate=2025-12-01&endDate=2026-02-28"
```

Both dates are included. Other date ranges are also supported.
Months use `yyyy-MM` and are sorted by date, so reporting works across years.
Customers and months without purchases are left out. A month with zero-point purchases is kept.
If no purchases match, the API returns an empty customer list.

Providing only one date, invalid dates, or reversed dates returns HTTP 400. Unexpected failures
return HTTP 500 without showing database details or stack traces.

## Sample Data

`schema.sql` creates the tables and checks that amounts are nonnegative and customers exist.
`data.sql` adds three customers and 18 purchases. Two purchases fall outside the default period.
The sample dates follow the current month; months are not hard-coded.

| Customer | Two months ago | Previous month | Current month | Total |
| --- | ---: | ---: | ---: | ---: |
| Alice Johnson | 115 | 250 | 140 | 505 |
| Bob Smith | 52 | 160 | 390 | 602 |
| Carol Davis | 0 | 50 | 26 | 76 |

## Project Structure

```text
customer-rewards/
|-- pom.xml
|-- README.md
`-- src/
    |-- main/
    |   |-- java/com/example/rewards/
    |   |   |-- RewardsApplication.java
    |   |   |-- controller/
    |   |   |-- dto/
    |   |   |-- exception/
    |   |   |-- model/
    |   |   |-- repository/
    |   |   `-- service/
    |   `-- resources/
    |       |-- application.properties
    |       |-- schema.sql
    |       `-- data.sql
    `-- test/
        |-- java/com/example/rewards/
        `-- resources/integration-data.sql
```

The controller handles requests. The service calculates and groups points. The repository reads
purchases from the database. DTOs define the response, and the exception handler returns errors.

## Tests

Run all tests with `mvn test`. Run `mvn clean verify` to also build the project and check formatting.

- Calculator tests cover purchase limits, cents, large amounts, nulls, and negative amounts.
- Service tests cover multiple customers, multiple purchases, year changes, leap years, and dates.
- Integration tests use a real HTTP server and H2 to check totals, date filtering, and SQL constraints.
- Controller tests check the response when the database is unavailable.

The test SQL has five customers and 20 purchases. Fixed dates and a fixed clock keep the tests
repeatable. Test reports are saved in `target/surefire-reports`.

Use `mvn spotless:apply` to format Java code and `mvn javadoc:javadoc` to generate documentation.

## Submission

Upload `src/`, `pom.xml`, and `README.md`. Do not upload `target/`, `bin/`, logs, IDE files, or a ZIP.
This is a read-only demo API. Authentication and a permanent database would be needed for production.
