package com.rupeek.hotelbooking.api.dto;

import com.rupeek.hotelbooking.domain.booking.Booking;
import com.rupeek.hotelbooking.domain.booking.BookingStatus;

import java.time.Instant;
import java.time.LocalDate;

public record BookingResponse(String id, String guestId, String propertyId, String roomTypeId, LocalDate checkIn,
                              LocalDate checkOut, int guests, int rooms, MoneyDto totalAmount, BookingStatus status,
                              Instant holdExpiresAt, String paymentId, MoneyDto refundedAmount) {

    public static BookingResponse from(Booking booking) {
        return new BookingResponse(booking.id(), booking.guestId(), booking.propertyId(),
                booking.inventoryKey().roomTypeId(), booking.stay().checkIn(), booking.stay().checkOut(),
                booking.guests(), booking.rooms(), MoneyDto.from(booking.totalAmount()), booking.status(),
                booking.holdExpiresAt(), booking.paymentId().orElse(null),
                booking.refundedAmount().map(MoneyDto::from).orElse(null));
    }
}
