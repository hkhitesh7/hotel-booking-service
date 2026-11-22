package com.rupeek.hotelbooking.application;

import com.rupeek.hotelbooking.domain.booking.Booking;
import com.rupeek.hotelbooking.domain.payment.Payment;

/** {@code replayed} is true when an earlier attempt with the same idempotency key was returned. */
public record PaymentResult(Payment payment, Booking booking, boolean replayed) {
}
