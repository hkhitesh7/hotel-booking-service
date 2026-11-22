package com.rupeek.hotelbooking.application;

import com.rupeek.hotelbooking.domain.booking.Booking;
import com.rupeek.hotelbooking.domain.booking.BookingRepository;
import com.rupeek.hotelbooking.domain.booking.BookingStatus;
import com.rupeek.hotelbooking.domain.cancellation.RefundDecision;
import com.rupeek.hotelbooking.domain.common.ConflictException;
import com.rupeek.hotelbooking.domain.common.NotFoundException;
import com.rupeek.hotelbooking.domain.payment.Payment;
import com.rupeek.hotelbooking.domain.payment.PaymentRepository;
import com.rupeek.hotelbooking.domain.property.Property;
import com.rupeek.hotelbooking.domain.property.PropertyRepository;
import com.rupeek.hotelbooking.support.KeyedLocks;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Cancels a booking: the property's cancellation policy decides the refund, the original payment
 * method refunds it, and the held rooms go back into the pool.
 */
@Service
public class CancellationService {

    private final BookingService bookingService;
    private final BookingRepository bookingRepository;
    private final PropertyRepository propertyRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentProcessorRegistry processors;
    private final CancellationPolicyRegistry policies;
    private final InventoryService inventoryService;
    private final KeyedLocks bookingLocks;
    private final Clock clock;

    public CancellationService(BookingService bookingService, BookingRepository bookingRepository,
                               PropertyRepository propertyRepository, PaymentRepository paymentRepository,
                               PaymentProcessorRegistry processors, CancellationPolicyRegistry policies,
                               InventoryService inventoryService, KeyedLocks bookingLocks, Clock clock) {
        this.bookingService = bookingService;
        this.bookingRepository = bookingRepository;
        this.propertyRepository = propertyRepository;
        this.paymentRepository = paymentRepository;
        this.processors = processors;
        this.policies = policies;
        this.inventoryService = inventoryService;
        this.bookingLocks = bookingLocks;
        this.clock = clock;
    }

    public CancellationResult cancel(String bookingId) {
        return bookingLocks.withLock(bookingId, () -> {
            Booking booking = bookingService.getBooking(bookingId);
            booking.ensureCanTransitionTo(BookingStatus.CANCELLED);
            LocalDate today = LocalDate.now(clock);
            if (today.isAfter(booking.stay().checkIn())) {
                throw new ConflictException("Booking " + bookingId + " cannot be cancelled after check-in");
            }

            RefundDecision refund = decideRefund(booking, today);
            if (refund.isRefundDue()) {
                issueRefund(booking, refund);
            }
            booking.cancel(refund.amount(), clock.instant());
            bookingRepository.save(booking);
            inventoryService.release(booking);
            return new CancellationResult(booking, refund);
        });
    }

    private RefundDecision decideRefund(Booking booking, LocalDate today) {
        if (booking.status() != BookingStatus.CONFIRMED) {
            return RefundDecision.none(booking.totalAmount().currency(), "Nothing was paid");
        }
        Property property = propertyRepository.findById(booking.propertyId())
                .orElseThrow(() -> new NotFoundException("Property", booking.propertyId()));
        return policies.resolve(property.cancellationPolicyCode()).evaluate(booking, today);
    }

    private void issueRefund(Booking booking, RefundDecision refund) {
        String paymentId = booking.paymentId().orElseThrow(
                () -> new IllegalStateException("Confirmed booking " + booking.id() + " has no payment"));
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment", paymentId));
        processors.forMethod(payment.method()).refund(payment, refund.amount());
        payment.recordRefund(refund.amount());
        paymentRepository.save(payment);
    }
}
