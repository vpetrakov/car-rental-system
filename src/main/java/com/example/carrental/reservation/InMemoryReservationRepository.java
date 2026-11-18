package com.example.carrental.reservation;

import com.example.carrental.car.Car;
import com.example.carrental.car.CarType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Thread-safe, single-JVM allocation for callers sharing this repository instance.
 * Synchronization covers the entire availability check and insertion.
 */
public final class InMemoryReservationRepository implements ReservationRepository {
    private final List<Car> cars;
    private final List<Reservation> reservations = new ArrayList<>();

    public InMemoryReservationRepository(Collection<Car> cars) {
        Objects.requireNonNull(cars, "cars");
        this.cars = List.copyOf(cars);
        if (this.cars.stream().map(Car::id).distinct().count() != this.cars.size()) {
            throw new IllegalArgumentException("Car ids must be unique");
        }
    }

    @Override
    public synchronized Reservation reserveAvailable(CarType type, RentalPeriod period) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(period, "period");

        Car available = cars.stream()
                .filter(car -> car.type() == type)
                .filter(car -> reservations.stream().noneMatch(existing ->
                        existing.carId().equals(car.id()) && existing.period().overlaps(period)))
                .findFirst()
                .orElseThrow(NoCarAvailableException::new);

        Reservation reservation = new Reservation(UUID.randomUUID(), available.id(), period);
        reservations.add(reservation);
        return reservation;
    }

    @Override
    public synchronized List<Reservation> findAll() {
        return List.copyOf(reservations);
    }
}
