# Car Rental System

A lightweight, object-oriented car rental reservation system implemented in **Java 21**.

The application reserves a physical vehicle of type `SEDAN`, `SUV`, or `VAN` for a requested start date/time and a positive number of rental days. Inventory is finite, and a car cannot be double-booked for overlapping periods.

## Requirements

- Reserve an available car of a specified type.
- Support Sedan, SUV, and Van, with finite inventory per type.
- Reject overlapping bookings for the same physical vehicle.
- Allow back-to-back reservations at the checkout boundary.
- Validate inputs and cover behavior with automated tests.

## Technologies

- Java 21
- Maven 3.9+
- JUnit 5
- In-memory storage

## Run

Open this folder as a Maven project in IntelliJ IDEA and select JDK 21, or run:

```bash
mvn clean test
```

## Architecture and Design Decisions

The code uses a small **package-by-feature** structure:

```text
com.example.carrental
├── car
│   ├── Car.java
│   └── CarType.java
└── reservation
    ├── Reservation.java
    ├── RentalPeriod.java
    ├── CarRentalService.java
    ├── ReservationRepository.java
    ├── InMemoryReservationRepository.java
    └── NoCarAvailableException.java
```

- **Domain records** (`Car`, `RentalPeriod`, `Reservation`) are immutable value holders.
- **CarRentalService** validates the request and delegates allocation.
- **ReservationRepository** defines an atomic allocation boundary; **InMemoryReservationRepository** provides a simple storage implementation and an immutable snapshot through `findAll()`.
- Grouping by business feature keeps related code together without introducing unnecessary `model`, `service`, or `infrastructure` subpackages.

### Reservation rules

Each car has a unique ID. A reservation is possible only if no existing booking for that car overlaps the requested period. Rental periods are **half-open**: `[start, end)`. Thus, a new booking may start exactly when another ends.

```java
start1.isBefore(end2) && start2.isBefore(end1)
```

Rental days are modeled as 24-hour increments in **local-time arithmetic**. `LocalDateTime` does not encode a timezone or guarantee real elapsed hours across daylight-saving transitions.

### Concurrency

The repository contract combines availability checking and reservation creation in one atomic operation. Its in-memory implementation synchronizes the **entire check-and-insert**, preventing double booking when multiple threads or services share the **same repository instance**.

A single repository lock is simple and correct for this exercise but limits parallelism. Separate repository instances do not share state or synchronization.

### Why plain Java?

The assessment is about object-oriented design, finite inventory, reservation rules, and tests. Spring Boot, a REST API, and a database would add infrastructure without improving the core demonstration. A production system would need persistent storage, transactions, and database-enforced booking consistency.

## Tests

The suite contains **21 JUnit test methods** across three classes:

- `CarRentalServiceTest`: 12 tests for reservation behavior, validation, inventory, failure safety, and concurrency (including two services sharing a repository).
- `RentalPeriodTest`: 8 tests for interval construction, overlap, adjacency, and invalid boundaries.
- `InMemoryReservationRepositoryTest`: 1 test for invalid repository construction.

Run:

```bash
mvn clean test
```

## Trade-offs and Limitations

- **Ephemeral storage:** reservations disappear when the process stops.
- **Single-JVM coordination:** no distributed lock or shared database.
- **Coarse-grained lock:** favors simplicity over throughput.
- **Local-time model:** pickup-location timezone and DST policies are outside scope.
- **Not implemented:** REST endpoints, authentication, pricing, cancellation, and inventory administration.

For production, I would consider Spring Boot, PostgreSQL transactions, database exclusion constraints for overlapping periods, explicit timezone rules, and integration tests.
