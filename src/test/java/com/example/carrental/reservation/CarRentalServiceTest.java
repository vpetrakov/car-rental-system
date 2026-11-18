package com.example.carrental.reservation;

import com.example.carrental.car.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class CarRentalServiceTest {
    private static final LocalDateTime START = LocalDateTime.of(2031, 1, 10, 10, 0);

    private static InMemoryReservationRepository inventory(Car... cars) {
        return new InMemoryReservationRepository(List.of(cars));
    }

    @Test
    void supportsAllThreeTypesAndLimitedInventory() {
        var repo = inventory(new Car("s1", CarType.SEDAN), new Car("u1", CarType.SUV), new Car("v1", CarType.VAN));
        var service = new CarRentalService(repo);
        assertEquals("s1", service.reserve(CarType.SEDAN, START, 1).carId());
        assertEquals("u1", service.reserve(CarType.SUV, START, 1).carId());
        assertEquals("v1", service.reserve(CarType.VAN, START, 1).carId());
        assertThrows(NoCarAvailableException.class, () -> service.reserve(CarType.SUV, START, 1));
    }

    @Test
    void rejectsOverlappingPeriodAndAllowsCheckoutBoundary() {
        var service = new CarRentalService(inventory(new Car("u1", CarType.SUV)));
        service.reserve(CarType.SUV, START, 2);
        assertThrows(NoCarAvailableException.class, () -> service.reserve(CarType.SUV, START.plusDays(1), 1));
        assertDoesNotThrow(() -> service.reserve(CarType.SUV, START.plusDays(2), 1));
    }

    @Test
    void canUseTwoCarsOfSameTypeBeforeExhaustion() {
        var service = new CarRentalService(inventory(new Car("a", CarType.SEDAN), new Car("b", CarType.SEDAN)));
        assertNotEquals(service.reserve(CarType.SEDAN, START, 1).carId(), service.reserve(CarType.SEDAN, START, 1).carId());
        assertThrows(NoCarAvailableException.class, () -> service.reserve(CarType.SEDAN, START, 1));
    }

    @Test
    void rejectsInvalidInputsAndDuplicateCarIds() {
        var service = new CarRentalService(inventory(new Car("u1", CarType.SUV)));
        assertThrows(IllegalArgumentException.class, () -> service.reserve(CarType.SUV, START, 0));
        assertThrows(IllegalArgumentException.class, () -> service.reserve(CarType.SUV, START, -1));
        assertThrows(NullPointerException.class, () -> service.reserve(null, START, 1));
        assertThrows(NullPointerException.class, () -> service.reserve(CarType.SUV, null, 1));
        assertThrows(IllegalArgumentException.class, () -> inventory(new Car("x", CarType.SUV), new Car("x", CarType.VAN)));
    }

    @Test
    void defensiveSnapshotCannotBeModified() {
        var repo = inventory(new Car("u1", CarType.SUV));
        new CarRentalService(repo).reserve(CarType.SUV, START, 1);
        assertThrows(UnsupportedOperationException.class, () -> repo.findAll().clear());
    }

    @Test
    void concurrentBookingOfLastCarHasExactlyOneWinner() throws Exception {
        var repo = inventory(new Car("v1", CarType.VAN));
        var service = new CarRentalService(repo);
        var gate = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> book = () -> {
                gate.await();
                try {
                    service.reserve(CarType.VAN, START, 1);
                    return true;
                } catch (NoCarAvailableException expected) {
                    return false;
                }
            };
            var a = pool.submit(book);
            var b = pool.submit(book);
            gate.countDown();
            int successes = (a.get(10, TimeUnit.SECONDS) ? 1 : 0) + (b.get(10, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, successes);
            assertEquals(1, repo.findAll().size());
        }
    }

    @Test
    void missingCarTypeDoesNotConsumeOtherTypes() {
        var service = new CarRentalService(new InMemoryReservationRepository(
                List.of(new Car("sedan-1", CarType.SEDAN))));
        assertThrows(NoCarAvailableException.class, () -> service.reserve(CarType.SUV, START, 1));
        assertEquals("sedan-1", service.reserve(CarType.SEDAN, START, 1).carId());
    }

    @Test
    void reservationsForNonOverlappingDatesReuseSameCar() {
        var service = new CarRentalService(new InMemoryReservationRepository(
                List.of(new Car("van-1", CarType.VAN))));
        assertEquals("van-1", service.reserve(CarType.VAN, START, 1).carId());
        assertEquals("van-1", service.reserve(CarType.VAN, START.plusDays(4), 1).carId());
    }

    @Test
    void failedBookingDoesNotModifyStoredReservations() {
        var repository = new InMemoryReservationRepository(List.of(new Car("suv-1", CarType.SUV)));
        var service = new CarRentalService(repository);
        service.reserve(CarType.SUV, START, 2);
        assertThrows(NoCarAvailableException.class, () -> service.reserve(CarType.SUV, START.plusDays(1), 1));
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void reservationsHaveDistinctIdentifiers() {
        var service = new CarRentalService(new InMemoryReservationRepository(
                List.of(new Car("s1", CarType.SEDAN))));
        var first = service.reserve(CarType.SEDAN, START, 1);
        var second = service.reserve(CarType.SEDAN, START.plusDays(1), 1);
        assertNotEquals(first.id(), second.id());
    }

    @Test
    void emptyInventoryRejectsReservation() {
        var service = new CarRentalService(inventory());
        assertThrows(NoCarAvailableException.class,
                () -> service.reserve(CarType.SEDAN, START, 1));
    }

    @Test
    void concurrentServicesSharingRepositoryCannotDoubleBook() throws Exception {
        var repository = inventory(new Car("sedan-1", CarType.SEDAN));
        var firstService = new CarRentalService(repository);
        var secondService = new CarRentalService(repository);
        var gate = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> firstBooking = () -> {
                gate.await();
                try {
                    firstService.reserve(CarType.SEDAN, START, 2);
                    return true;
                } catch (NoCarAvailableException expected) {
                    return false;
                }
            };
            Callable<Boolean> secondBooking = () -> {
                gate.await();
                try {
                    secondService.reserve(CarType.SEDAN, START, 2);
                    return true;
                } catch (NoCarAvailableException expected) {
                    return false;
                }
            };
            var first = executor.submit(firstBooking);
            var second = executor.submit(secondBooking);
            gate.countDown();

            int successes = (first.get(10, TimeUnit.SECONDS) ? 1 : 0)
                    + (second.get(10, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, successes);
            assertEquals(1, repository.findAll().size());
        }
    }
}
