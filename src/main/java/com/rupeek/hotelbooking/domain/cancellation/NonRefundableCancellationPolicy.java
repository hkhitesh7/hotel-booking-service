package com.rupeek.hotelbooking.domain.cancellation;

import com.rupeek.hotelbooking.domain.booking.Booking;

import java.time.LocalDate;

public final class NonRefundableCancellationPolicy implements CancellationPolicy {

    public static final String CODE = "NON_REFUNDABLE";

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public RefundDecision evaluate(Booking booking, LocalDate today) {
        return RefundDecision.none(booking.totalAmount().currency(), "Non-refundable rate");
    }
}
