package com.rupeek.hotelbooking.domain.pricing;

import com.rupeek.hotelbooking.domain.common.Guard;
import com.rupeek.hotelbooking.domain.common.Money;
import com.rupeek.hotelbooking.domain.property.Property;
import com.rupeek.hotelbooking.domain.property.RoomType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/** Decorator: adds a percentage on Friday and Saturday nights. */
public final class WeekendSurchargePricingStrategy implements PricingStrategy {

    private static final Set<DayOfWeek> WEEKEND_NIGHTS = Set.of(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY);

    private final PricingStrategy delegate;
    private final int surchargePercent;

    public WeekendSurchargePricingStrategy(PricingStrategy delegate, int surchargePercent) {
        this.delegate = Guard.requireNonNull(delegate, "delegate");
        Guard.require(surchargePercent >= 0, "surchargePercent must not be negative");
        this.surchargePercent = surchargePercent;
    }

    @Override
    public Money nightlyRate(Property property, RoomType roomType, LocalDate night) {
        Money base = delegate.nightlyRate(property, roomType, night);
        return WEEKEND_NIGHTS.contains(night.getDayOfWeek()) ? base.plus(base.percentage(surchargePercent)) : base;
    }
}
