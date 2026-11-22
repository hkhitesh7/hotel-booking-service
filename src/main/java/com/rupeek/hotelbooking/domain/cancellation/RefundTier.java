package com.rupeek.hotelbooking.domain.cancellation;

import com.rupeek.hotelbooking.domain.common.Guard;

/** "Cancel at least {@code minDaysBeforeCheckIn} days ahead and get {@code refundPercent}% back." */
public record RefundTier(int minDaysBeforeCheckIn, int refundPercent) {

    public RefundTier {
        Guard.require(minDaysBeforeCheckIn >= 0, "minDaysBeforeCheckIn must not be negative");
        Guard.require(refundPercent >= 0 && refundPercent <= 100, "refundPercent must be within 0..100");
    }
}
