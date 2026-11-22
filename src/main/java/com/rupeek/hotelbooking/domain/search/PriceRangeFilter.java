package com.rupeek.hotelbooking.domain.search;

import com.rupeek.hotelbooking.domain.common.Guard;
import com.rupeek.hotelbooking.domain.common.Money;

/** Keeps properties with at least one offer whose average nightly rate falls in [min, max]. Either bound optional. */
public final class PriceRangeFilter implements SearchFilter {

    private final Money min;
    private final Money max;

    public PriceRangeFilter(Money min, Money max) {
        Guard.require(min != null || max != null, "price filter needs a min or a max");
        Guard.require(min == null || max == null || !min.isGreaterThan(max), "minPrice must not exceed maxPrice");
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean matches(PropertyAvailability candidate) {
        return candidate.offers().stream()
                .map(offer -> offer.price().averageNightlyRate())
                .anyMatch(this::inRange);
    }

    private boolean inRange(Money rate) {
        return (min == null || !min.isGreaterThan(rate)) && (max == null || !rate.isGreaterThan(max));
    }
}
