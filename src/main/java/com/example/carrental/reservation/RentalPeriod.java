package com.example.carrental.reservation;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Half-open interval [start, end): back-to-back bookings do not overlap.
 */
public record RentalPeriod(LocalDateTime start, LocalDateTime end) {
    public RentalPeriod {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Start must precede end");
        }
    }

    /**
     * Each rental day is 24 hours in local-time arithmetic; no timezone is modeled.
     */
    public static RentalPeriod forDays(LocalDateTime start, int days) {
        Objects.requireNonNull(start, "start");
        if (days <= 0 || days > 365) {
            throw new IllegalArgumentException(
                    "Days must be between 1 and 365"
            );
        }
        return new RentalPeriod(start, start.plus(Duration.ofDays(days)));
    }

    public boolean overlaps(RentalPeriod other) {
        Objects.requireNonNull(other, "other");
        return start.isBefore(other.end()) && other.start().isBefore(end);
    }
}
