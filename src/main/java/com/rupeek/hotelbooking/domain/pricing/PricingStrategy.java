package com.rupeek.hotelbooking.domain.pricing;

import com.rupeek.hotelbooking.domain.common.Money;
import com.rupeek.hotelbooking.domain.property.Property;
import com.rupeek.hotelbooking.domain.property.RoomType;

import java.time.LocalDate;

/**
 * Prices a single room for a single night. Strategies compose as decorators (weekend surcharge,
 * seasonal, occupancy-based...) on top of {@link BaseRatePricingStrategy}.
 */
public interface PricingStrategy {

    Money nightlyRate(Property property, RoomType roomType, LocalDate night);
}
