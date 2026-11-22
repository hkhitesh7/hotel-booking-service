package com.rupeek.hotelbooking.domain;

import com.rupeek.hotelbooking.domain.booking.Booking;
import com.rupeek.hotelbooking.domain.booking.BookingStatus;
import com.rupeek.hotelbooking.domain.booking.InvalidStateTransitionException;
import com.rupeek.hotelbooking.domain.common.DateRange;
import com.rupeek.hotelbooking.domain.common.InvalidRequestException;
import com.rupeek.hotelbooking.domain.common.Money;
import com.rupeek.hotelbooking.domain.inventory.InventoryKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingTest {

    private static final Instant NOW = Instant.parse("2026-12-01T10:00:00Z");

    private static Booking pending() {
        LocalDate checkIn = LocalDate.of(2026, 12, 10);
        return Booking.pendingPayment("bk_1", "guest", new InventoryKey("p", "rt"),
                new DateRange(checkIn, checkIn.plusDays(2)), 2, 1, Money.inr("2000"), NOW,
                NOW.plus(Duration.ofMinutes(15)));
    }

    @Test
    void startsAsPendingPaymentHold() {
        Booking booking = pending();
        assertThat(booking.status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(booking.status().holdsInventory()).isTrue();
    }

    @Test
    void confirmRecordsPayment() {
        Booking booking = pending();
        booking.confirm("pay_1");
        assertThat(booking.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.paymentId()).contains("pay_1");
    }

    @Test
    void cannotConfirmTwiceOrAfterCancellation() {
        Booking booking = pending();
        booking.confirm("pay_1");
        assertThatThrownBy(() -> booking.confirm("pay_2")).isInstanceOf(InvalidStateTransitionException.class);

        Booking cancelled = pending();
        cancelled.cancel(Money.inr("0"), NOW);
        assertThatThrownBy(() -> cancelled.confirm("pay_1")).isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void confirmedBookingCannotExpire() {
        Booking booking = pending();
        booking.confirm("pay_1");
        assertThatThrownBy(() -> booking.expire(NOW)).isInstanceOf(InvalidStateTransitionException.class);
        assertThat(booking.isHoldLapsed(NOW.plus(Duration.ofDays(1)))).isFalse();
    }

    @Test
    void holdLapsesExactlyAtExpiry() {
        Booking booking = pending();
        assertThat(booking.isHoldLapsed(NOW.plus(Duration.ofMinutes(14)))).isFalse();
        assertThat(booking.isHoldLapsed(NOW.plus(Duration.ofMinutes(15)))).isTrue();
    }

    @Test
    void refundCannotExceedAmountPaid() {
        Booking booking = pending();
        booking.confirm("pay_1");
        assertThatThrownBy(() -> booking.cancel(Money.inr("2000.01"), NOW))
                .isInstanceOf(InvalidRequestException.class);
        assertThat(booking.status()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @ParameterizedTest
    @EnumSource(value = BookingStatus.class, names = {"CANCELLED", "EXPIRED"})
    void terminalStatesAllowNoTransitions(BookingStatus terminal) {
        assertThat(terminal.allowedTransitions()).isEmpty();
        assertThat(terminal.holdsInventory()).isFalse();
    }
}
