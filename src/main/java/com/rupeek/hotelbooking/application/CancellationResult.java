package com.rupeek.hotelbooking.application;

import com.rupeek.hotelbooking.domain.booking.Booking;
import com.rupeek.hotelbooking.domain.cancellation.RefundDecision;

public record CancellationResult(Booking booking, RefundDecision refund) {
}
