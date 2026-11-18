package com.example.carrental.reservation;

import com.example.carrental.car.CarType;

import java.util.List;

/**
 * Storage boundary for allocation. Availability check and insertion MUST be atomic
 * for callers sharing one repository instance.
 */
public interface ReservationRepository {
    Reservation reserveAvailable(CarType type, RentalPeriod period);

    List<Reservation> findAll();
}
