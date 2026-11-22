package com.rupeek.hotelbooking.domain.payment;

import com.rupeek.hotelbooking.domain.common.Guard;
import com.rupeek.hotelbooking.domain.common.Money;

import java.time.Instant;
import java.util.Optional;

/** Record of one payment attempt against a booking (successful or not) and any refunds on it. */
public final class Payment {

    private final String id;
    private final String bookingId;
    private final PaymentMethod method;
    private final Money amount;
    private final String idempotencyKey;
    private final String transactionId;
    private final String instrumentSummary;
    private final String failureReason;
    private final Instant createdAt;

    private PaymentStatus status;
    private Money refundedAmount;

    private Payment(String id, String bookingId, PaymentMethod method, Money amount, String idempotencyKey,
                    ChargeOutcome outcome, Instant createdAt) {
        this.id = Guard.requireText(id, "payment.id");
        this.bookingId = Guard.requireText(bookingId, "bookingId");
        this.method = Guard.requireNonNull(method, "method");
        this.amount = Guard.requireNonNull(amount, "amount");
        this.idempotencyKey = Guard.requireText(idempotencyKey, "idempotencyKey");
        this.transactionId = outcome.transactionId();
        this.instrumentSummary = outcome.instrumentSummary();
        this.failureReason = outcome.failureReason();
        this.createdAt = Guard.requireNonNull(createdAt, "createdAt");
        this.status = outcome.succeeded() ? PaymentStatus.SUCCEEDED : PaymentStatus.FAILED;
        this.refundedAmount = Money.zero(amount.currency());
    }

    public static Payment fromOutcome(String id, ChargeRequest request, PaymentMethod method,
                                      ChargeOutcome outcome, Instant now) {
        return new Payment(id, request.bookingId(), method, request.amount(), request.idempotencyKey(),
                outcome, now);
    }

    public synchronized void recordRefund(Money refund) {
        Guard.require(status == PaymentStatus.SUCCEEDED || status == PaymentStatus.PARTIALLY_REFUNDED,
                "only a successful payment can be refunded");
        Money total = refundedAmount.plus(refund);
        Guard.require(!total.isGreaterThan(amount), "refunds cannot exceed the captured amount");
        refundedAmount = total;
        status = total.equals(amount) ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED;
    }

    public boolean isSuccessful() {
        return status != PaymentStatus.FAILED;
    }

    public boolean belongsTo(String otherBookingId) {
        return bookingId.equals(otherBookingId);
    }

    public String id() {
        return id;
    }

    public String bookingId() {
        return bookingId;
    }

    public PaymentMethod method() {
        return method;
    }

    public Money amount() {
        return amount;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public Optional<String> transactionId() {
        return Optional.ofNullable(transactionId);
    }

    public String instrumentSummary() {
        return instrumentSummary;
    }

    public Optional<String> failureReason() {
        return Optional.ofNullable(failureReason);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public synchronized PaymentStatus status() {
        return status;
    }

    public synchronized Money refundedAmount() {
        return refundedAmount;
    }
}
