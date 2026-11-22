package com.rupeek.hotelbooking.domain.search;

import com.rupeek.hotelbooking.domain.common.Money;
import com.rupeek.hotelbooking.domain.property.Property;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Search candidate: a property together with the offers that are actually bookable. */
public record PropertyAvailability(Property property, List<RoomOffer> offers) {

    public PropertyAvailability {
        offers = List.copyOf(offers);
    }

    public boolean hasOffers() {
        return !offers.isEmpty();
    }

    public Optional<Money> cheapestTotal() {
        return offers.stream().map(offer -> offer.price().total()).min(Comparator.naturalOrder());
    }
}
