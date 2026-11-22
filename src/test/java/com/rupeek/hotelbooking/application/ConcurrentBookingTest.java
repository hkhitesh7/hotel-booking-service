package com.rupeek.hotelbooking.application;

import com.rupeek.hotelbooking.domain.booking.Booking;
import com.rupeek.hotelbooking.domain.booking.BookingStatus;
import com.rupeek.hotelbooking.domain.inventory.InventoryUnavailableException;
import com.rupeek.hotelbooking.domain.payment.PaymentMethod;
import com.rupeek.hotelbooking.domain.property.Property;
import com.rupeek.hotelbooking.testsupport.Fixture;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class ConcurrentBookingTest {

    private static final int THREADS = 32;

    private final Fixture f = new Fixture();

    /** Runs all tasks released at the same instant and returns their outcomes (value or thrown cause). */
    private static <T> List<Object> raceAll(List<Callable<T>> tasks) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (Callable<T> task : tasks) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();
            List<Object> outcomes = new ArrayList<>();
            for (Future<T> future : futures) {
                try {
                    outcomes.add(future.get(10, TimeUnit.SECONDS));
                } catch (ExecutionException e) {
                    outcomes.add(e.getCause());
                } catch (TimeoutException e) {
                    throw new AssertionError("task timed out", e);
                }
            }
            return outcomes;
        } finally {
            pool.shutdownNow();
        }
    }

    @RepeatedTest(5)
    void simultaneousBookingsNeverOversellInventory() throws Exception {
        int rooms = 3;
        Property hotel = f.simpleHotel(rooms);
        List<Callable<Booking>> attempts = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            attempts.add(() -> f.book(hotel, 5, 2));
        }

        List<Object> outcomes = raceAll(attempts);

        assertThat(outcomes).filteredOn(Booking.class::isInstance).hasSize(rooms);
        assertThat(outcomes).filteredOn(InventoryUnavailableException.class::isInstance).hasSize(THREADS - rooms);
        Booking winner = (Booking) outcomes.stream().filter(Booking.class::isInstance).findFirst().orElseThrow();
        assertThat(f.inventory.availableRooms(winner.inventoryKey(), winner.stay())).isZero();
    }

    @Test
    void concurrentRetriesWithSameIdempotencyKeyChargeOnce() throws Exception {
        Booking booking = f.book(f.simpleHotel(1), 5, 2);
        PayCommand command = new PayCommand(booking.id(), PaymentMethod.UPI, Map.of("vpa", "guest@okbank"), "same");
        List<Callable<PaymentResult>> attempts = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            attempts.add(() -> f.paymentService.pay(command));
        }

        List<Object> outcomes = raceAll(attempts);

        assertThat(outcomes).allMatch(PaymentResult.class::isInstance);
        assertThat(outcomes).extracting(o -> ((PaymentResult) o).payment().id()).containsOnly(
                ((PaymentResult) outcomes.get(0)).payment().id());
        assertThat(outcomes).filteredOn(o -> !((PaymentResult) o).replayed()).hasSize(1);
        assertThat(f.bookingService.getBooking(booking.id()).status()).isEqualTo(BookingStatus.CONFIRMED);
    }
}
