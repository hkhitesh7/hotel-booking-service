package com.rupeek.hotelbooking.domain.pricing;

import com.rupeek.hotelbooking.domain.common.DateRange;
import com.rupeek.hotelbooking.domain.common.Guard;
import com.rupeek.hotelbooking.domain.common.Money;
import com.rupeek.hotelbooking.domain.property.Property;
import com.rupeek.hotelbooking.domain.property.RoomType;

/** Turns a per-night {@link PricingStrategy} into a quote for a whole stay. */
public final class PriceCalculator {

    private final PricingStrategy strategy;

    public PriceCalculator(PricingStrategy strategy) {
        this.strategy = Guard.requireNonNull(strategy, "strategy");
    }

    public PriceQuote quote(Property property, RoomType roomType, DateRange stay, int rooms) {
        Guard.require(rooms > 0, "rooms must be positive");
        Money perRoom = stay.nightsStream()
                .map(night -> strategy.nightlyRate(property, roomType, night))
                .reduce(Money.zero(roomType.baseNightlyRate().currency()), Money::plus);
        return new PriceQuote(perRoom.times(rooms), perRoom.dividedBy(stay.nights()));
    }
}
