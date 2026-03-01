# To-Do Service

REST API for managing to-do items with three states: **NOT_DONE**, **DONE**, and **PAST_DUE**.

## Assumptions

- In-memory H2 database — data is lost on restart
- Items with null or empty description are not allowed
- New items may be created without a due date or with any due date in the future or in the past. The past-due status is automatically set.
- The due-date value is expected in ISO-8601 format, including timezone offset. All timestamps are stored and returned in UTC (ISO-8601 format)
- No authentication or authorization

## Tech Stack

- **Java 21**
- **Spring Boot 3.5** (Web, Data JPA, Validation)
- **H2** — in-memory database
- **SpringDoc OpenAPI** — auto-generated API docs at `/swagger-ui.html`
- **Maven 3.9** (via Maven Wrapper, no local install needed)
- **JUnit 5 + Mockito + AssertJ** — tests

## Build

```bash
./mvnw clean package
```

The executable JAR is created in `target/`.

## Run Tests

```bash
./mvnw test
```

## Run Locally

```bash
./mvnw spring-boot:run
```

The service starts on [http://localhost:8080](http://localhost:8080).

- API docs: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- H2 console: [http://localhost:8080/h2-console](http://localhost:8080/h2-console) (JDBC URL: `jdbc:h2:mem:testdb`)
