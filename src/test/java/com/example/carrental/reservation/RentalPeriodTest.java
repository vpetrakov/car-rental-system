package com.example.carrental.reservation;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class RentalPeriodTest {
    private static final LocalDateTime START = LocalDateTime.of(2031, 1, 10, 10, 0);

    @Test
    void createsPeriodWithRequestedDuration() {
        var period = RentalPeriod.forDays(START, 3);
        assertEquals(START, period.start());
        assertEquals(START.plusDays(3), period.end());
    }

    @Test
    void rejectsZeroOrNegativeDuration() {
        assertThrows(IllegalArgumentException.class, () -> RentalPeriod.forDays(START, 0));
        assertThrows(IllegalArgumentException.class, () -> RentalPeriod.forDays(START, -2));
    }

    @Test
    void rejectsInvalidPeriodBoundaries() {
        assertThrows(IllegalArgumentException.class, () -> new RentalPeriod(START, START));
        assertThrows(IllegalArgumentException.class, () -> new RentalPeriod(START, START.minusHours(1)));
    }

    @Test
    void rejectsNullPeriodInputs() {
        assertThrows(NullPointerException.class, () -> RentalPeriod.forDays(null, 1));
        assertThrows(NullPointerException.class, () -> new RentalPeriod(null, START));
        assertThrows(NullPointerException.class, () -> new RentalPeriod(START, null));
    }

    @Test
    void overlappingPeriodsAreSymmetric() {
        var first = RentalPeriod.forDays(START, 2);
        var second = RentalPeriod.forDays(START.plusDays(1), 2);
        assertTrue(first.overlaps(second));
        assertTrue(second.overlaps(first));
    }

    @Test
    void adjacentPeriodsDoNotOverlap() {
        var first = RentalPeriod.forDays(START, 2);
        var second = RentalPeriod.forDays(START.plusDays(2), 1);
        assertFalse(first.overlaps(second));
        assertFalse(second.overlaps(first));
    }

    @Test
    void disjointPeriodsDoNotOverlap() {
        var first = RentalPeriod.forDays(START, 1);
        var second = RentalPeriod.forDays(START.plusDays(5), 1);
        assertFalse(first.overlaps(second));
    }

    @Test
    void fullyContainedPeriodOverlaps() {
        var outer = RentalPeriod.forDays(START, 5);
        var inner = RentalPeriod.forDays(START.plusDays(1), 1);
        assertTrue(outer.overlaps(inner));
        assertTrue(inner.overlaps(outer));
    }
}
