# Customer Rewards API

A Spring Boot REST API that calculates reward points for each customer, grouped by actual calendar
month, with a total over the reporting period. Purchases are loaded from SQL, not Java constants.

## Technology

- Java 17, Spring Boot 3.5.7, Maven
- Spring Web and Spring Data JPA
- H2 in-memory SQL database for a self-contained demonstration
- JUnit 5, Mockito, MockMvc, and real-HTTP integration tests
- Spotless with Google Java Format; formatting is enforced during `verify`

## Run

Install a **JDK 17 or newer** and set `JAVA_HOME` to that JDK, not a Java 8 installation or a JRE.
The Maven wrapper pins Maven 3.9.11, so installing Maven separately is unnecessary. The first build
needs internet access to download Maven and dependencies.

Windows PowerShell:

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

Linux/macOS:

```bash
./mvnw clean verify
./mvnw spring-boot:run
```

The default URL is `http://localhost:8080/api/rewards`. If port 8080 is occupied:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```

Alternatively, after building, run `java -jar target/customer-rewards-1.0.0.jar`.
H2 is initialized from `schema.sql` and `data.sql` at startup. Data is intentionally ephemeral and
resets when the process restarts; no external database installation is required.

## REST Endpoint

### GET /api/rewards

Without parameters, reports the **current calendar month and the previous two calendar months**.
The effective window starts on the first day of the earliest month and ends on the last day of the
current month. Both the application clock and demo SQL session use UTC. The response includes these
effective dates so the period is unambiguous. No month names, month numbers, or years are fixed in
the application logic or the demonstration seed data.

To report a specific three-month period, provide both inclusive ISO dates:

```bash
curl "http://localhost:8080/api/rewards?startDate=2025-12-01&endDate=2026-02-28"
```

```powershell
Invoke-RestMethod 'http://localhost:8080/api/rewards'
Invoke-RestMethod 'http://localhost:8080/api/rewards?startDate=2025-12-01&endDate=2026-02-28'
```

| Parameter | Format | Behavior |
| --- | --- | --- |
| `startDate` | `yyyy-MM-dd` | Inclusive first purchase date; supply with `endDate` |
| `endDate` | `yyyy-MM-dd` | Inclusive last purchase date; must not precede `startDate` |

Explicit ranges may also be shorter or longer than three months, including a single day. The
three-month default is a convenience, not an arbitrary restriction on historical reports.
Purchases outside the exact requested dates are excluded, even if they share a reporting month.

Example response when the current month is October 2026 (month values change automatically):

```json
{
  "startDate": "2026-08-01",
  "endDate": "2026-10-31",
  "customers": [
    {
      "customerId": 1,
      "customerName": "Alice Johnson",
      "monthlyRewards": [
        { "month": "2026-08", "points": 115 },
        { "month": "2026-09", "points": 250 },
        { "month": "2026-10", "points": 140 }
      ],
      "totalPoints": 505
    },
    {
      "customerId": 2,
      "customerName": "Bob Smith",
      "monthlyRewards": [
        { "month": "2026-08", "points": 52 },
        { "month": "2026-09", "points": 160 },
        { "month": "2026-10", "points": 390 }
      ],
      "totalPoints": 602
    },
    {
      "customerId": 3,
      "customerName": "Carol Davis",
      "monthlyRewards": [
        { "month": "2026-08", "points": 0 },
        { "month": "2026-09", "points": 50 },
        { "month": "2026-10", "points": 26 }
      ],
      "totalPoints": 76
    }
  ]
}
```

Customers are ordered by identifier; months are chronological ISO year-months so different years
cannot be accidentally merged. Only customers and months with matching transactions are returned.
A month with transactions earning zero points is retained. Months without transactions are omitted.
A period without purchases returns HTTP 200 with `"customers": []`.

### Errors

Incomplete ranges, reversed ranges, malformed dates, and impossible dates return HTTP 400 using
RFC 9457 `application/problem+json`:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "startDate must be on or before endDate.",
  "instance": "/api/rewards"
}
```

Unexpected infrastructure failures return HTTP 500 with a generic detail. The server logs the
failure, but the HTTP response does not disclose connection information or stack traces.

## Reward Policy

Points are calculated **per transaction**, then summed by customer and year-month:

- Up to and including $50: zero points.
- Above $50 through $100: one point for each whole dollar above $50.
- Above $100: 50 points for the middle tier plus two points for each whole dollar above $100.

For $120, the result is `50 + 2 * 20 = 90` points. Two $60 transactions earn `10 + 10 = 20`,
not 90; purchases must not be combined before applying the tiers.

**Fractional-dollar assumption:** only complete dollars earn points, rounded down independently
for each transaction. For example, $50.99 earns 0, $100.99 earns 50, and $120.99 earns 90.
`BigDecimal` and SQL `DECIMAL(12,2)` avoid binary floating-point errors. Point values use `long`,
with checked arithmetic during aggregation. Negative or missing purchase amounts are invalid;
the calculator rejects them and SQL constraints prevent storing them. Refunds are outside this
purchase-only model and are not silently treated as zero-point purchases.

## SQL Demonstration Data

`src/main/resources/data.sql` inserts three customers and 18 purchases. Sixteen purchases fall
in the default three-month window, and two are outside it to demonstrate date filtering.
Dates use SQL `CURRENT_DATE`, `DATE_TRUNC`, and `DATEADD`, so the example remains usable as time
passes. Current-month purchases occur on its first day, rather than becoming future-dated when
the app is run early in a month.

| Customer | Two months ago | Previous month | Current month | Total |
| --- | ---: | ---: | ---: | ---: |
| Alice Johnson | 115 | 250 | 140 | 505 |
| Bob Smith | 52 | 160 | 390 | 602 |
| Carol Davis | 0 | 50 | 26 | 76 |

The schema supplies primary keys, a customer foreign key, non-null constraints, a nonnegative
amount check, and a date/customer index. Hibernate validates the SQL schema instead of creating it.
The endpoint is read-only; additional purchase records can be supplied in SQL scripts.

## Project Structure

```text
customer-rewards/
|-- pom.xml                              Dependencies, build, formatting gate
|-- mvnw / mvnw.cmd / .mvn/               Pinned Maven wrapper
|-- .github/workflows/build.yml           GitHub Actions build and tests
|-- src/main/java/com/example/rewards/
|   |-- RewardsApplication.java          Bootstrap and injectable UTC clock
|   |-- controller/                      REST request binding
|   |-- dto/                             Immutable JSON response records
|   |-- exception/                       Validation errors and problem responses
|   |-- model/                           Customer and purchase JPA entities
|   |-- repository/                      Inclusive date query with customer fetching
|   `-- service/                         Per-purchase policy and monthly aggregation
|-- src/main/resources/
|   |-- application.properties           H2, SQL initialization, JPA validation
|   |-- schema.sql                       Tables, constraints, and index
|   `-- data.sql                         Date-relative demonstration purchases
|-- src/test/java/com/example/rewards/    Unit, MVC, and real-HTTP integration tests
`-- src/test/resources/
    `-- integration-data.sql             Deterministic multi-customer test fixtures
```

The controller delegates to a read-only transactional service. The repository fetches customer
details alongside purchases using an entity graph, avoiding an N+1 query pattern. The calculator
owns the reward tiers; the service groups its results with `YearMonth`. DTO records keep JPA entities
out of API serialization. Classes and declared methods have purpose-oriented Javadocs; Java source
uses explicit imports and Google Java formatting.

## Tests and Quality Checks

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd "-Dtest=RewardPointsCalculatorTest" test
.\mvnw.cmd "-Dtest=RewardsServiceTest" test
.\mvnw.cmd "-Dtest=RewardsIntegrationTest" test
.\mvnw.cmd spotless:apply
.\mvnw.cmd javadoc:javadoc
```

Use `./mvnw` for the same commands on Linux/macOS.

- Calculator unit tests: tier boundaries, the $120 example, cents, zero, large values, null, negative.
- Service unit tests: multiple customers (including duplicate names), multiple purchases, cross-year
  chronological grouping, per-transaction tier application, zero-point months, defaults, leap years,
  one-day windows, and date validation before database access.
- Real-HTTP integration tests: embedded server, actual repository and H2 schema, full expected
  customer/month totals, default and explicit windows, both inclusive boundaries, empty reports,
  invalid query parameters, and SQL amount/customer constraints.
- MVC failure test: a simulated database outage returns a sanitized HTTP 500 problem response.

The integration SQL fixture deliberately uses fixed dates from December 2025 through February 2026
and an injected clock. It contains five customers and 20 purchases, including a customer with no
purchases, repeated $60 purchases, and purchases immediately outside the reporting window. Fixed
test fixtures are separate from the date-relative production demonstration data.

Surefire reports are generated under `target/surefire-reports`. Generated Javadocs are under
`target/reports/apidocs`. GitHub Actions runs `clean verify` on pushes and pull requests.

## Submission and Scope

Submit the GitHub source repository, not an archive. `.gitignore` excludes `target/`, `bin/`, local
tools, logs, and IDE settings. The Maven wrapper is script-only; no wrapper JAR or ZIP is committed.

This is a focused assignment demonstration, not an authenticated production customer-data service.
A production deployment would require access control, a persistent database with migrations,
operational monitoring, and a pagination or batch strategy for large purchase histories. The H2
seed date functions are database-specific and should be adapted when changing database engines.