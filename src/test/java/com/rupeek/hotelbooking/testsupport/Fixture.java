package com.rupeek.hotelbooking.testsupport;

import com.rupeek.hotelbooking.application.BookingService;
import com.rupeek.hotelbooking.application.CancellationPolicyRegistry;
import com.rupeek.hotelbooking.application.CancellationService;
import com.rupeek.hotelbooking.application.CreateBookingCommand;
import com.rupeek.hotelbooking.application.InventoryService;
import com.rupeek.hotelbooking.application.OnboardingCommands.NewProperty;
import com.rupeek.hotelbooking.application.OnboardingCommands.NewRoomType;
import com.rupeek.hotelbooking.application.OnboardingCommands.OnboardOwner;
import com.rupeek.hotelbooking.application.PayCommand;
import com.rupeek.hotelbooking.application.PaymentProcessorRegistry;
import com.rupeek.hotelbooking.application.PaymentResult;
import com.rupeek.hotelbooking.application.PaymentService;
import com.rupeek.hotelbooking.application.PropertyOnboardingService;
import com.rupeek.hotelbooking.application.PropertySearchService;
import com.rupeek.hotelbooking.domain.booking.Booking;
import com.rupeek.hotelbooking.domain.cancellation.NonRefundableCancellationPolicy;
import com.rupeek.hotelbooking.domain.cancellation.RefundTier;
import com.rupeek.hotelbooking.domain.cancellation.TieredCancellationPolicy;
import com.rupeek.hotelbooking.domain.common.Money;
import com.rupeek.hotelbooking.domain.payment.PaymentMethod;
import com.rupeek.hotelbooking.domain.pricing.BaseRatePricingStrategy;
import com.rupeek.hotelbooking.domain.pricing.PriceCalculator;
import com.rupeek.hotelbooking.domain.property.Amenity;
import com.rupeek.hotelbooking.domain.property.Location;
import com.rupeek.hotelbooking.domain.property.OwnerType;
import com.rupeek.hotelbooking.domain.property.Property;
import com.rupeek.hotelbooking.domain.property.PropertyOwner;
import com.rupeek.hotelbooking.infrastructure.payment.CardPaymentProcessor;
import com.rupeek.hotelbooking.infrastructure.payment.MockPaymentGateway;
import com.rupeek.hotelbooking.infrastructure.payment.UpiPaymentProcessor;
import com.rupeek.hotelbooking.infrastructure.payment.WalletPaymentProcessor;
import com.rupeek.hotelbooking.infrastructure.persistence.InMemoryBookingRepository;
import com.rupeek.hotelbooking.infrastructure.persistence.InMemoryInventoryRepository;
import com.rupeek.hotelbooking.infrastructure.persistence.InMemoryPaymentRepository;
import com.rupeek.hotelbooking.infrastructure.persistence.InMemoryPropertyOwnerRepository;
import com.rupeek.hotelbooking.infrastructure.persistence.InMemoryPropertyRepository;
import com.rupeek.hotelbooking.support.KeyedLocks;
import com.rupeek.hotelbooking.support.UuidIdGenerator;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Real services over in-memory adapters, wired by hand (no Spring) so service tests are fast and
 * exercise genuine collaborators. Pricing is the flat base rate to keep expected totals obvious.
 */
public final class Fixture {

    public static final Duration HOLD = Duration.ofMinutes(15);
    public static final String VALID_CARD = "4111111111111111";
    public static final String DECLINED_CARD = "4000000000000002";

    public final MutableClock clock = new MutableClock(Instant.parse("2026-12-01T06:00:00Z"));
    public final InMemoryPropertyRepository properties = new InMemoryPropertyRepository();
    public final InMemoryBookingRepository bookings = new InMemoryBookingRepository();
    public final InMemoryPaymentRepository payments = new InMemoryPaymentRepository();
    public final InventoryService inventory = new InventoryService(new InMemoryInventoryRepository());
    public final PropertyOnboardingService onboarding;
    public final PropertySearchService search;
    public final BookingService bookingService;
    public final PaymentService paymentService;
    public final CancellationService cancellationService;

    public Fixture() {
        var ids = new UuidIdGenerator();
        var locks = new KeyedLocks(16);
        var priceCalculator = new PriceCalculator(new BaseRatePricingStrategy());
        var policies = new CancellationPolicyRegistry(List.of(
                new TieredCancellationPolicy("FLEXIBLE", List.of(new RefundTier(2, 100), new RefundTier(1, 50))),
                new NonRefundableCancellationPolicy()));
        var gateway = new MockPaymentGateway();
        var processors = new PaymentProcessorRegistry(List.of(new CardPaymentProcessor(gateway),
                new UpiPaymentProcessor(gateway), new WalletPaymentProcessor(gateway)));

        onboarding = new PropertyOnboardingService(new InMemoryPropertyOwnerRepository(), properties, inventory,
                policies, ids);
        search = new PropertySearchService(properties, inventory, priceCalculator);
        bookingService = new BookingService(properties, bookings, inventory, priceCalculator, locks, ids, clock, HOLD);
        paymentService = new PaymentService(bookingService, bookings, payments, processors, locks, ids, clock);
        cancellationService = new CancellationService(bookingService, bookings, properties, payments, processors,
                policies, inventory, locks, clock);
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /** One property in Bangalore with a "Standard" room type (2 guests, INR 1000/night). */
    public Property hotel(String name, String locality, int stars, Set<Amenity> amenities, int rooms,
                          String policy) {
        return hotel(name, locality, stars, amenities, policy, List.of(
                new NewRoomType("Standard", 2, rooms, Money.inr("1000"))));
    }

    public Property hotel(String name, String locality, int stars, Set<Amenity> amenities, String policy,
                          List<NewRoomType> roomTypes) {
        PropertyOwner owner = onboarding.onboardOwner(new OnboardOwner(name + " Owner", OwnerType.INDIVIDUAL,
                List.of(new NewProperty(name, new Location("Bangalore", locality, null), stars, amenities,
                        roomTypes, policy))));
        return properties.findById(owner.propertyIds().iterator().next()).orElseThrow();
    }

    public Property simpleHotel(int rooms) {
        return hotel("Test Inn", "Koramangala", 3, Set.of(Amenity.WIFI), rooms, "FLEXIBLE");
    }

    public CreateBookingCommand bookingFor(Property property, int checkInOffsetDays, int nights, int guests) {
        LocalDate checkIn = today().plusDays(checkInOffsetDays);
        return new CreateBookingCommand("guest-1", property.id(), property.roomTypes().get(0).id(), checkIn,
                checkIn.plusDays(nights), guests, null);
    }

    public Booking book(Property property, int checkInOffsetDays, int nights) {
        return bookingService.createBooking(bookingFor(property, checkInOffsetDays, nights, 2));
    }

    public PaymentResult payByCard(Booking booking, String cardNumber) {
        return paymentService.pay(new PayCommand(booking.id(), PaymentMethod.CARD, Map.of("cardNumber", cardNumber),
                UUID.randomUUID().toString()));
    }

    public Booking bookAndPay(Property property, int checkInOffsetDays, int nights) {
        Booking booking = book(property, checkInOffsetDays, nights);
        payByCard(booking, VALID_CARD);
        return booking;
    }
}
