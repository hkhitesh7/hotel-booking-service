package com.rupeek.hotelbooking.domain.booking;

import com.rupeek.hotelbooking.domain.common.DateRange;
import com.rupeek.hotelbooking.domain.common.Guard;
import com.rupeek.hotelbooking.domain.common.Money;
import com.rupeek.hotelbooking.domain.inventory.InventoryKey;

import java.time.Instant;
import java.util.Optional;

/**
 * Aggregate root for a reservation. All status changes go through {@link #transitionTo}, which
 * enforces the {@link BookingStatus} state machine; callers cannot set status directly.
 */
public final class Booking {

    private final String id;
    private final String guestId;
    private final InventoryKey inventoryKey;
    private final DateRange stay;
    private final int guests;
    private final int rooms;
    private final Money totalAmount;
    private final Instant createdAt;
    private final Instant holdExpiresAt;

    private BookingStatus status;
    private String paymentId;
    private Money refundedAmount;
    private Instant closedAt;

    private Booking(String id, String guestId, InventoryKey inventoryKey, DateRange stay, int guests, int rooms,
                    Money totalAmount, Instant createdAt, Instant holdExpiresAt) {
        this.id = Guard.requireText(id, "booking.id");
        this.guestId = Guard.requireText(guestId, "guestId");
        this.inventoryKey = Guard.requireNonNull(inventoryKey, "inventoryKey");
        this.stay = Guard.requireNonNull(stay, "stay");
        Guard.require(guests > 0, "guests must be positive");
        Guard.require(rooms > 0, "rooms must be positive");
        this.guests = guests;
        this.rooms = rooms;
        this.totalAmount = Guard.requireNonNull(totalAmount, "totalAmount");
        this.createdAt = Guard.requireNonNull(createdAt, "createdAt");
        this.holdExpiresAt = Guard.requireNonNull(holdExpiresAt, "holdExpiresAt");
        this.status = BookingStatus.PENDING_PAYMENT;
    }

    /** A new booking always starts as a time-boxed hold awaiting payment. */
    public static Booking pendingPayment(String id, String guestId, InventoryKey inventoryKey, DateRange stay,
                                         int guests, int rooms, Money totalAmount, Instant now,
                                         Instant holdExpiresAt) {
        Guard.require(holdExpiresAt.isAfter(now), "hold expiry must be in the future");
        return new Booking(id, guestId, inventoryKey, stay, guests, rooms, totalAmount, now, holdExpiresAt);
    }

    public synchronized void confirm(String paymentId) {
        Guard.requireText(paymentId, "paymentId");
        transitionTo(BookingStatus.CONFIRMED);
        this.paymentId = paymentId;
    }

    public synchronized void cancel(Money refund, Instant at) {
        Guard.require(!refund.isGreaterThan(totalAmount), "refund cannot exceed the booking amount");
        transitionTo(BookingStatus.CANCELLED);
        this.refundedAmount = refund;
        this.closedAt = at;
    }

    public synchronized void expire(Instant at) {
        transitionTo(BookingStatus.EXPIRED);
        this.closedAt = at;
    }

    /** Fails fast (before any side effect such as a refund) if the transition is not allowed. */
    public synchronized void ensureCanTransitionTo(BookingStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidStateTransitionException(id, status, target);
        }
    }

    public synchronized boolean isHoldLapsed(Instant now) {
        return status == BookingStatus.PENDING_PAYMENT && !now.isBefore(holdExpiresAt);
    }

    private void transitionTo(BookingStatus target) {
        ensureCanTransitionTo(target);
        this.status = target;
    }

    public String id() {
        return id;
    }

    public String guestId() {
        return guestId;
    }

    public InventoryKey inventoryKey() {
        return inventoryKey;
    }

    public String propertyId() {
        return inventoryKey.propertyId();
    }

    public DateRange stay() {
        return stay;
    }

    public int guests() {
        return guests;
    }

    public int rooms() {
        return rooms;
    }

    public Money totalAmount() {
        return totalAmount;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant holdExpiresAt() {
        return holdExpiresAt;
    }

    public synchronized BookingStatus status() {
        return status;
    }

    public synchronized Optional<String> paymentId() {
        return Optional.ofNullable(paymentId);
    }

    public synchronized Optional<Money> refundedAmount() {
        return Optional.ofNullable(refundedAmount);
    }

    public synchronized Optional<Instant> closedAt() {
        return Optional.ofNullable(closedAt);
    }
}
