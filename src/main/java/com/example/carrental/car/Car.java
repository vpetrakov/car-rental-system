package com.example.carrental.car;

import java.util.Objects;

/**
 * A uniquely identified physical vehicle.
 */
public record Car(String id, CarType type) {
    public Car {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Car id is required");
        }
        Objects.requireNonNull(type, "type");
    }
}
