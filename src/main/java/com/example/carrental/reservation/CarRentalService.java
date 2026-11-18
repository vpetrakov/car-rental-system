package com.example.carrental.reservation;

import com.example.carrental.car.CarType;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Coordinates reservation requests; atomic allocation belongs to the repository.
 */
public final class CarRentalService {
    private final ReservationRepository repository;

    public CarRentalService(ReservationRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    /**
     * Reserves one physical car for a half-open rental period.
     * The repository guarantees atomic check-and-insert within its shared instance.
     *
     * @throws NoCarAvailableException  if no vehicle is available for the entire period
     * @throws IllegalArgumentException if days is not positive
     */
    public Reservation reserve(CarType type, LocalDateTime start, int days) {
        Objects.requireNonNull(type, "type");
        return repository.reserveAvailable(type, RentalPeriod.forDays(start, days));
    }
}
