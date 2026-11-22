package com.rupeek.hotelbooking.application;

import java.time.LocalDate;

/** {@code rooms} is optional; when absent the minimum number of rooms for the party is used. */
public record CreateBookingCommand(String guestId, String propertyId, String roomTypeId, LocalDate checkIn,
                                   LocalDate checkOut, int guests, Integer rooms) {
}
