package com.rupeek.hotelbooking.domain.booking;

import java.util.EnumSet;
import java.util.Set;

/**
 * Booking lifecycle.
 * <pre>
 *   PENDING_PAYMENT --pay ok--> CONFIRMED --cancel--> CANCELLED
 *        |  \--cancel-------------------------------> CANCELLED
 *        \--hold lapses--> EXPIRED
 * </pre>
 * A failed payment leaves the booking in PENDING_PAYMENT so the guest can retry while the hold lasts.
 */
public enum BookingStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    CANCELLED,
    EXPIRED;

    public Set<BookingStatus> allowedTransitions() {
        return switch (this) {
            case PENDING_PAYMENT -> EnumSet.of(CONFIRMED, CANCELLED, EXPIRED);
            case CONFIRMED -> EnumSet.of(CANCELLED);
            case CANCELLED, EXPIRED -> EnumSet.noneOf(BookingStatus.class);
        };
    }

    public boolean canTransitionTo(BookingStatus target) {
        return allowedTransitions().contains(target);
    }

    /** Whether a booking in this state is still holding inventory. */
    public boolean holdsInventory() {
        return this == PENDING_PAYMENT || this == CONFIRMED;
    }
}
