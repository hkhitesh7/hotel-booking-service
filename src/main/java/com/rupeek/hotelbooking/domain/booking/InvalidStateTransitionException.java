package com.rupeek.hotelbooking.domain.booking;

import com.rupeek.hotelbooking.domain.common.ConflictException;

public class InvalidStateTransitionException extends ConflictException {

    public InvalidStateTransitionException(String bookingId, BookingStatus from, BookingStatus to) {
        super("Booking " + bookingId + " cannot move from " + from + " to " + to);
    }
}
