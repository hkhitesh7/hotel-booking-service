package com.rupeek.hotelbooking.application;

import com.rupeek.hotelbooking.domain.booking.Booking;
import com.rupeek.hotelbooking.domain.booking.BookingStatus;
import com.rupeek.hotelbooking.domain.common.InvalidRequestException;
import com.rupeek.hotelbooking.domain.common.Money;
import com.rupeek.hotelbooking.domain.common.NotFoundException;
import com.rupeek.hotelbooking.domain.inventory.InventoryUnavailableException;
import com.rupeek.hotelbooking.domain.property.Property;
import com.rupeek.hotelbooking.testsupport.Fixture;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingServiceTest {

    private final Fixture f = new Fixture();
    private final Property hotel = f.simpleHotel(1);

    @Test
    void createsPricedPendingHold() {
        Booking booking = f.book(hotel, 5, 3);

        assertThat(booking.status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(booking.totalAmount()).isEqualTo(Money.inr("3000"));
        assertThat(booking.holdExpiresAt()).isEqualTo(f.clock.instant().plus(Fixture.HOLD));
    }

    @Test
    void secondBookingForOverlappingDatesIsRejected() {
        f.book(hotel, 5, 3);
        assertThatThrownBy(() -> f.book(hotel, 6, 1)).isInstanceOf(InventoryUnavailableException.class);
    }

    @Test
    void nonOverlappingBookingsOfTheSameRoomSucceed() {
        f.book(hotel, 5, 3);
        assertThat(f.book(hotel, 8, 2).status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
    }

    @Test
    void derivesRoomCountFromPartySize() {
        Property bigger = f.simpleHotel(3);
        Booking booking = f.bookingService.createBooking(f.bookingFor(bigger, 5, 1, 5));
        assertThat(booking.rooms()).isEqualTo(3);
        assertThat(booking.totalAmount()).isEqualTo(Money.inr("3000"));
    }

    @Test
    void rejectsTooFewRoomsForTheParty() {
        var cmd = f.bookingFor(hotel, 5, 1, 3);
        var tooFew = new CreateBookingCommand(cmd.guestId(), cmd.propertyId(), cmd.roomTypeId(), cmd.checkIn(),
                cmd.checkOut(), 3, 1);
        assertThatThrownBy(() -> f.bookingService.createBooking(tooFew)).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void rejectsPastCheckInAndUnknownRoomType() {
        assertThatThrownBy(() -> f.book(hotel, -1, 2)).isInstanceOf(InvalidRequestException.class);

        var cmd = f.bookingFor(hotel, 5, 1, 2);
        var unknown = new CreateBookingCommand(cmd.guestId(), cmd.propertyId(), "nope", cmd.checkIn(),
                cmd.checkOut(), 2, null);
        assertThatThrownBy(() -> f.bookingService.createBooking(unknown)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void lapsedHoldsExpireAndReleaseInventory() {
        Booking booking = f.book(hotel, 5, 3);
        f.clock.advance(Fixture.HOLD.plus(Duration.ofSeconds(1)));

        assertThat(f.bookingService.expireLapsedHolds()).isEqualTo(1);

        assertThat(f.bookingService.getBooking(booking.id()).status()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(f.book(hotel, 5, 3).status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
    }

    @Test
    void activeHoldsAreNotExpired() {
        f.book(hotel, 5, 3);
        f.clock.advance(Duration.ofMinutes(5));
        assertThat(f.bookingService.expireLapsedHolds()).isZero();
    }
}
