package com.example.carrental.reservation;

import java.util.Objects;
import java.util.UUID;

/**
 * A confirmed allocation of one physical car for a rental period.
 */
public record Reservation(UUID id, String carId, RentalPeriod period) {
    public Reservation {
        Objects.requireNonNull(id, "id");
        if (carId == null || carId.isBlank()) {
            throw new IllegalArgumentException("Car id is required");
        }
        Objects.requireNonNull(period, "period");
    }
}
