package com.rupeek.hotelbooking.domain.property;

import com.rupeek.hotelbooking.domain.common.Guard;

public record StarRating(int stars) {

    public StarRating {
        Guard.require(stars >= 1 && stars <= 5, "star rating must be between 1 and 5");
    }

    public boolean isAtLeast(int minimum) {
        return stars >= minimum;
    }
}
