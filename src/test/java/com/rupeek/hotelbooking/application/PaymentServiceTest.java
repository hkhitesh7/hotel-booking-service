package com.rupeek.hotelbooking.application;

import com.rupeek.hotelbooking.domain.booking.Booking;
import com.rupeek.hotelbooking.domain.booking.BookingStatus;
import com.rupeek.hotelbooking.domain.common.ConflictException;
import com.rupeek.hotelbooking.domain.common.InvalidRequestException;
import com.rupeek.hotelbooking.domain.payment.PaymentMethod;
import com.rupeek.hotelbooking.domain.payment.PaymentStatus;
import com.rupeek.hotelbooking.domain.property.Property;
import com.rupeek.hotelbooking.testsupport.Fixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class PaymentServiceTest {

    private final Fixture f = new Fixture();
    private final Property hotel = f.simpleHotel(2);

    static Stream<org.junit.jupiter.params.provider.Arguments> validInstruments() {
        return Stream.of(
                arguments(PaymentMethod.CARD, Map.of("cardNumber", Fixture.VALID_CARD)),
                arguments(PaymentMethod.UPI, Map.of("vpa", "guest@okbank")),
                arguments(PaymentMethod.WALLET, Map.of("provider", "paytm", "walletId", "9876543210")));
    }

    @ParameterizedTest
    @MethodSource("validInstruments")
    void successfulPaymentConfirmsBooking(PaymentMethod method, Map<String, String> details) {
        Booking booking = f.book(hotel, 5, 2);

        PaymentResult result = f.paymentService.pay(new PayCommand(booking.id(), method, details, "key-1"));

        assertThat(result.payment().status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(result.booking().status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(result.booking().paymentId()).contains(result.payment().id());
    }

    @Test
    void declinedPaymentKeepsBookingPendingAndAllowsRetry() {
        Booking booking = f.book(hotel, 5, 2);

        PaymentResult declined = f.payByCard(booking, Fixture.DECLINED_CARD);
        assertThat(declined.payment().status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(declined.booking().status()).isEqualTo(BookingStatus.PENDING_PAYMENT);

        PaymentResult retry = f.payByCard(booking, Fixture.VALID_CARD);
        assertThat(retry.booking().status()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void sameIdempotencyKeyReturnsOriginalPaymentWithoutRecharging() {
        Booking booking = f.book(hotel, 5, 2);
        PayCommand command = new PayCommand(booking.id(), PaymentMethod.UPI, Map.of("vpa", "guest@okbank"), "k-42");

        PaymentResult first = f.paymentService.pay(command);
        PaymentResult replay = f.paymentService.pay(command);

        assertThat(replay.replayed()).isTrue();
        assertThat(replay.payment().id()).isEqualTo(first.payment().id());
        assertThat(replay.booking().status()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void idempotencyKeyCannotBeReusedForAnotherBooking() {
        Booking first = f.book(hotel, 5, 2);
        Booking second = f.book(hotel, 5, 2);
        Map<String, String> upi = Map.of("vpa", "guest@okbank");
        f.paymentService.pay(new PayCommand(first.id(), PaymentMethod.UPI, upi, "shared"));

        assertThatThrownBy(() -> f.paymentService.pay(new PayCommand(second.id(), PaymentMethod.UPI, upi, "shared")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void cannotPayAnAlreadyConfirmedBookingWithANewKey() {
        Booking booking = f.bookAndPay(hotel, 5, 2);
        assertThatThrownBy(() -> f.payByCard(booking, Fixture.VALID_CARD)).isInstanceOf(ConflictException.class);
    }

    @Test
    void paymentAfterHoldLapsedExpiresBookingAndFails() {
        Booking booking = f.book(hotel, 5, 2);
        f.clock.advance(Fixture.HOLD.plus(Duration.ofMinutes(1)));

        assertThatThrownBy(() -> f.payByCard(booking, Fixture.VALID_CARD)).isInstanceOf(ConflictException.class);
        assertThat(f.bookingService.getBooking(booking.id()).status()).isEqualTo(BookingStatus.EXPIRED);
    }

    @Test
    void rejectsInvalidInstrumentDetailsAndUnknownMethods() {
        Booking booking = f.book(hotel, 5, 2);
        assertThatThrownBy(() -> f.payByCard(booking, "4111111111111112")).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> f.paymentService.pay(new PayCommand(booking.id(), new PaymentMethod("CRYPTO"),
                Map.of(), "k"))).isInstanceOf(InvalidRequestException.class);
        assertThat(f.bookingService.getBooking(booking.id()).status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
    }
}
