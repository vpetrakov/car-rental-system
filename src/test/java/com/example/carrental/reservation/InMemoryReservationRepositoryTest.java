package com.example.carrental.reservation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryReservationRepositoryTest {
    @Test
    void rejectsNullInventory() {
        assertThrows(NullPointerException.class,
                () -> new InMemoryReservationRepository(null));
    }
}
