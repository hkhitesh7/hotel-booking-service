package com.rupeek.hotelbooking.domain.search;

import com.rupeek.hotelbooking.domain.property.StarRating;

public final class MinimumStarRatingFilter implements SearchFilter {

    private final StarRating minimum;

    public MinimumStarRatingFilter(StarRating minimum) {
        this.minimum = minimum;
    }

    @Override
    public boolean matches(PropertyAvailability candidate) {
        return candidate.property().starRating().isAtLeast(minimum.stars());
    }
}
