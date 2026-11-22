package com.rupeek.hotelbooking.domain.search;

import com.rupeek.hotelbooking.domain.property.Amenity;

import java.util.Set;

/** Keeps properties offering every requested amenity. */
public final class AmenitiesFilter implements SearchFilter {

    private final Set<Amenity> required;

    public AmenitiesFilter(Set<Amenity> required) {
        this.required = Set.copyOf(required);
    }

    @Override
    public boolean matches(PropertyAvailability candidate) {
        return candidate.property().hasAllAmenities(required);
    }
}
