package com.rupeek.hotelbooking.domain.pricing;

import com.rupeek.hotelbooking.domain.common.Money;
import com.rupeek.hotelbooking.domain.property.Property;
import com.rupeek.hotelbooking.domain.property.RoomType;

import java.time.LocalDate;

public final class BaseRatePricingStrategy implements PricingStrategy {

    @Override
    public Money nightlyRate(Property property, RoomType roomType, LocalDate night) {
        return roomType.baseNightlyRate();
    }
}
