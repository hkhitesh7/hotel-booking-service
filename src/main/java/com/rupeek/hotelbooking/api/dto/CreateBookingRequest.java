package com.rupeek.hotelbooking.api.dto;

import com.rupeek.hotelbooking.application.CreateBookingCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record CreateBookingRequest(@NotBlank String guestId, @NotBlank String propertyId,
                                   @NotBlank String roomTypeId, @NotNull LocalDate checkIn,
                                   @NotNull LocalDate checkOut, @Positive int guests, @Positive Integer rooms) {

    public CreateBookingCommand toCommand() {
        return new CreateBookingCommand(guestId, propertyId, roomTypeId, checkIn, checkOut, guests, rooms);
    }
}
