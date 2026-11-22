package com.rupeek.hotelbooking.infrastructure.scheduling;

import com.rupeek.hotelbooking.application.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically returns rooms held by unpaid, lapsed bookings to the pool. */
@Component
public class BookingHoldExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(BookingHoldExpiryJob.class);

    private final BookingService bookingService;

    public BookingHoldExpiryJob(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Scheduled(fixedDelayString = "${booking.expiry-sweep-interval:PT30S}")
    public void expireLapsedHolds() {
        int expired = bookingService.expireLapsedHolds();
        if (expired > 0) {
            log.info("Expired {} lapsed booking hold(s)", expired);
        }
    }
}
