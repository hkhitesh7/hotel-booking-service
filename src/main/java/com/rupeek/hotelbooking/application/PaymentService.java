package com.rupeek.hotelbooking.application;

import com.rupeek.hotelbooking.domain.booking.Booking;
import com.rupeek.hotelbooking.domain.booking.BookingRepository;
import com.rupeek.hotelbooking.domain.booking.BookingStatus;
import com.rupeek.hotelbooking.domain.common.ConflictException;
import com.rupeek.hotelbooking.domain.common.Guard;
import com.rupeek.hotelbooking.domain.payment.ChargeOutcome;
import com.rupeek.hotelbooking.domain.payment.ChargeRequest;
import com.rupeek.hotelbooking.domain.payment.Payment;
import com.rupeek.hotelbooking.domain.payment.PaymentRepository;
import com.rupeek.hotelbooking.support.IdGenerator;
import com.rupeek.hotelbooking.support.KeyedLocks;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Optional;

/**
 * Takes payment for a booking and lets the outcome drive the booking state:
 * success confirms it, a decline leaves it awaiting payment (retry allowed while the hold lasts).
 * Repeating a request with the same idempotency key returns the original result without re-charging.
 */
@Service
public class PaymentService {

    private final BookingService bookingService;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentProcessorRegistry processors;
    private final KeyedLocks bookingLocks;
    private final IdGenerator ids;
    private final Clock clock;

    public PaymentService(BookingService bookingService, BookingRepository bookingRepository,
                          PaymentRepository paymentRepository, PaymentProcessorRegistry processors,
                          KeyedLocks bookingLocks, IdGenerator ids, Clock clock) {
        this.bookingService = bookingService;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.processors = processors;
        this.bookingLocks = bookingLocks;
        this.ids = ids;
        this.clock = clock;
    }

    public PaymentResult pay(PayCommand command) {
        Guard.requireText(command.idempotencyKey(), "Idempotency-Key");
        Guard.requireNonNull(command.method(), "method");
        return bookingLocks.withLock(command.bookingId(), () -> replayIfSeen(command).orElseGet(() -> charge(command)));
    }

    private Optional<PaymentResult> replayIfSeen(PayCommand command) {
        return paymentRepository.findByIdempotencyKey(command.idempotencyKey()).map(previous -> {
            if (!previous.belongsTo(command.bookingId())) {
                throw new ConflictException("Idempotency-Key already used for a different booking");
            }
            return new PaymentResult(previous, bookingService.getBooking(command.bookingId()), true);
        });
    }

    private PaymentResult charge(PayCommand command) {
        Booking booking = bookingService.getBooking(command.bookingId());
        if (bookingService.expireIfLapsed(booking.id())) {
            throw new ConflictException("Booking " + booking.id() + " hold expired before payment");
        }
        booking.ensureCanTransitionTo(BookingStatus.CONFIRMED);

        ChargeRequest request = new ChargeRequest(booking.id(), booking.totalAmount(), command.details(),
                command.idempotencyKey());
        ChargeOutcome outcome = processors.forMethod(command.method()).charge(request);
        Payment payment = paymentRepository.save(
                Payment.fromOutcome(ids.next("pay"), request, command.method(), outcome, clock.instant()));

        if (outcome.succeeded()) {
            booking.confirm(payment.id());
            bookingRepository.save(booking);
        }
        return new PaymentResult(payment, booking, false);
    }
}
