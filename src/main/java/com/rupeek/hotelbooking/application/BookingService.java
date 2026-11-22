package com.rupeek.hotelbooking.application;

import com.rupeek.hotelbooking.domain.booking.Booking;
import com.rupeek.hotelbooking.domain.booking.BookingRepository;
import com.rupeek.hotelbooking.domain.common.DateRange;
import com.rupeek.hotelbooking.domain.common.Guard;
import com.rupeek.hotelbooking.domain.common.NotFoundException;
import com.rupeek.hotelbooking.domain.inventory.InventoryKey;
import com.rupeek.hotelbooking.domain.pricing.PriceQuote;
import com.rupeek.hotelbooking.domain.pricing.PriceCalculator;
import com.rupeek.hotelbooking.domain.property.Property;
import com.rupeek.hotelbooking.domain.property.PropertyRepository;
import com.rupeek.hotelbooking.domain.property.RoomType;
import com.rupeek.hotelbooking.support.IdGenerator;
import com.rupeek.hotelbooking.support.KeyedLocks;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Creates bookings as time-boxed holds on inventory and expires holds that are never paid.
 * Booking-level state changes across services are serialised through the shared {@code bookingLocks}.
 */
@Service
public class BookingService {

    private final PropertyRepository propertyRepository;
    private final BookingRepository bookingRepository;
    private final InventoryService inventoryService;
    private final PriceCalculator priceCalculator;
    private final KeyedLocks bookingLocks;
    private final IdGenerator ids;
    private final Clock clock;
    private final Duration holdDuration;

    public BookingService(PropertyRepository propertyRepository, BookingRepository bookingRepository,
                          InventoryService inventoryService, PriceCalculator priceCalculator,
                          KeyedLocks bookingLocks, IdGenerator ids, Clock clock,
                          @Value("${booking.hold-duration:PT15M}") Duration holdDuration) {
        this.propertyRepository = propertyRepository;
        this.bookingRepository = bookingRepository;
        this.inventoryService = inventoryService;
        this.priceCalculator = priceCalculator;
        this.bookingLocks = bookingLocks;
        this.ids = ids;
        this.clock = clock;
        this.holdDuration = holdDuration;
    }

    public Booking createBooking(CreateBookingCommand command) {
        Guard.requireText(command.guestId(), "guestId");
        Property property = propertyRepository.findById(command.propertyId())
                .orElseThrow(() -> new NotFoundException("Property", command.propertyId()));
        RoomType roomType = property.roomType(command.roomTypeId());
        DateRange stay = futureStay(command.checkIn(), command.checkOut());
        int rooms = roomsFor(roomType, command.guests(), command.rooms());
        PriceQuote quote = priceCalculator.quote(property, roomType, stay, rooms);

        InventoryKey key = new InventoryKey(property.id(), roomType.id());
        Instant now = clock.instant();
        Booking booking = Booking.pendingPayment(ids.next("bk"), command.guestId(), key, stay, command.guests(),
                rooms, quote.total(), now, now.plus(holdDuration));

        inventoryService.reserve(key, stay, rooms);
        try {
            return bookingRepository.save(booking);
        } catch (RuntimeException e) {
            inventoryService.release(booking);
            throw e;
        }
    }

    public Booking getBooking(String bookingId) {
        return bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking", bookingId));
    }

    /** Expires every unpaid hold past its deadline and returns its rooms to the pool. */
    public int expireLapsedHolds() {
        Instant now = clock.instant();
        List<Booking> lapsed = bookingRepository.findLapsedHolds(now);
        lapsed.forEach(booking -> expireIfLapsed(booking.id()));
        return lapsed.size();
    }

    /** @return true if the booking was (or already had to be) expired. */
    public boolean expireIfLapsed(String bookingId) {
        return bookingLocks.withLock(bookingId, () -> {
            Booking booking = getBooking(bookingId);
            Instant now = clock.instant();
            if (!booking.isHoldLapsed(now)) {
                return false;
            }
            booking.expire(now);
            bookingRepository.save(booking);
            inventoryService.release(booking);
            return true;
        });
    }

    private DateRange futureStay(LocalDate checkIn, LocalDate checkOut) {
        DateRange stay = new DateRange(checkIn, checkOut);
        Guard.require(!stay.checkIn().isBefore(LocalDate.now(clock)), "checkIn must not be in the past");
        return stay;
    }

    private static int roomsFor(RoomType roomType, int guests, Integer requestedRooms) {
        int rooms = requestedRooms != null ? requestedRooms : roomType.roomsNeededFor(guests);
        Guard.require(roomType.canAccommodate(guests, rooms),
                rooms + " x '" + roomType.name() + "' cannot accommodate " + guests + " guest(s)");
        return rooms;
    }
}
