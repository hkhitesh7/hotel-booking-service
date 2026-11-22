package com.rupeek.hotelbooking.application;

import com.rupeek.hotelbooking.domain.booking.Booking;
import com.rupeek.hotelbooking.domain.booking.BookingStatus;
import com.rupeek.hotelbooking.domain.common.ConflictException;
import com.rupeek.hotelbooking.domain.common.Money;
import com.rupeek.hotelbooking.domain.payment.PaymentStatus;
import com.rupeek.hotelbooking.domain.property.Property;
import com.rupeek.hotelbooking.testsupport.Fixture;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CancellationServiceTest {

    private final Fixture f = new Fixture();
    private final Property flexibleHotel = f.simpleHotel(1);

    @Test
    void earlyCancellationRefundsInFullAndReleasesRooms() {
        Booking booking = f.bookAndPay(flexibleHotel, 5, 2);

        CancellationResult result = f.cancellationService.cancel(booking.id());

        assertThat(result.booking().status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(result.refund().amount()).isEqualTo(Money.inr("2000"));
        assertThat(f.payments.findById(booking.paymentId().orElseThrow()).orElseThrow().status())
                .isEqualTo(PaymentStatus.REFUNDED);
        assertThat(f.book(flexibleHotel, 5, 2).status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
    }

    @Test
    void lateCancellationGetsPartialRefund() {
        Booking booking = f.bookAndPay(flexibleHotel, 1, 2);

        CancellationResult result = f.cancellationService.cancel(booking.id());

        assertThat(result.refund().amount()).isEqualTo(Money.inr("1000"));
        assertThat(f.payments.findById(booking.paymentId().orElseThrow()).orElseThrow().status())
                .isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
    }

    @Test
    void policyIsChosenPerProperty() {
        Property strict = f.hotel("Strict Inn", "Whitefield", 3, Set.of(), 1, "NON_REFUNDABLE");
        Booking booking = f.bookAndPay(strict, 10, 2);

        CancellationResult result = f.cancellationService.cancel(booking.id());

        assertThat(result.refund().isRefundDue()).isFalse();
        assertThat(result.booking().status()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void cancellingUnpaidHoldRefundsNothingButFreesRooms() {
        Booking booking = f.book(flexibleHotel, 5, 2);

        CancellationResult result = f.cancellationService.cancel(booking.id());

        assertThat(result.refund().isRefundDue()).isFalse();
        assertThat(f.book(flexibleHotel, 5, 2).status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
    }

    @Test
    void cannotCancelTwiceOrAfterExpiry() {
        Booking booking = f.bookAndPay(flexibleHotel, 5, 2);
        f.cancellationService.cancel(booking.id());
        assertThatThrownBy(() -> f.cancellationService.cancel(booking.id())).isInstanceOf(ConflictException.class);

        Booking unpaid = f.book(flexibleHotel, 10, 1);
        f.clock.advance(Fixture.HOLD.plus(Duration.ofSeconds(1)));
        f.bookingService.expireLapsedHolds();
        assertThatThrownBy(() -> f.cancellationService.cancel(unpaid.id())).isInstanceOf(ConflictException.class);
    }

    @Test
    void cannotCancelAfterStayHasStarted() {
        Booking booking = f.bookAndPay(flexibleHotel, 1, 3);
        f.clock.advance(Duration.ofDays(2));
        assertThatThrownBy(() -> f.cancellationService.cancel(booking.id())).isInstanceOf(ConflictException.class);
        assertThat(f.bookingService.getBooking(booking.id()).status()).isEqualTo(BookingStatus.CONFIRMED);
    }
}
