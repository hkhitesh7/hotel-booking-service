package com.rupeek.hotelbooking.domain.cancellation;

import com.rupeek.hotelbooking.domain.booking.Booking;
import com.rupeek.hotelbooking.domain.common.Guard;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

/** Refund percentage chosen by how far ahead of check-in the guest cancels. Covers most real policies. */
public final class TieredCancellationPolicy implements CancellationPolicy {

    private final String code;
    private final List<RefundTier> tiersMostGenerousFirst;

    public TieredCancellationPolicy(String code, List<RefundTier> tiers) {
        this.code = Guard.requireText(code, "code");
        Guard.require(tiers != null && !tiers.isEmpty(), "at least one refund tier is required");
        this.tiersMostGenerousFirst = tiers.stream()
                .sorted(Comparator.comparingInt(RefundTier::minDaysBeforeCheckIn).reversed())
                .toList();
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public RefundDecision evaluate(Booking booking, LocalDate today) {
        long daysBefore = ChronoUnit.DAYS.between(today, booking.stay().checkIn());
        return tiersMostGenerousFirst.stream()
                .filter(tier -> daysBefore >= tier.minDaysBeforeCheckIn())
                .findFirst()
                .map(tier -> new RefundDecision(booking.totalAmount().percentage(tier.refundPercent()),
                        tier.refundPercent() + "% refund: cancelled " + daysBefore + " day(s) before check-in"))
                .orElseGet(() -> RefundDecision.none(booking.totalAmount().currency(),
                        "No refund: cancelled " + daysBefore + " day(s) before check-in"));
    }
}
