package com.rupeek.hotelbooking.domain.cancellation;

import com.rupeek.hotelbooking.domain.booking.Booking;

import java.time.LocalDate;

/** Decides how much of a confirmed booking's amount is refunded when it is cancelled on {@code today}. */
public interface CancellationPolicy {

    /** Stable code a property refers to, e.g. "FLEXIBLE". */
    String code();

    RefundDecision evaluate(Booking booking, LocalDate today);
}
