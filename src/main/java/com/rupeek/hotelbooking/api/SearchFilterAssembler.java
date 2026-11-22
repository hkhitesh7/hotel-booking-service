package com.rupeek.hotelbooking.api;

import com.rupeek.hotelbooking.domain.common.Money;
import com.rupeek.hotelbooking.domain.property.Amenity;
import com.rupeek.hotelbooking.domain.property.StarRating;
import com.rupeek.hotelbooking.domain.search.AmenitiesFilter;
import com.rupeek.hotelbooking.domain.search.MinimumStarRatingFilter;
import com.rupeek.hotelbooking.domain.search.PriceRangeFilter;
import com.rupeek.hotelbooking.domain.search.SearchFilter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Translates optional query parameters into domain filters. Adding a filter = a new
 * {@link SearchFilter} plus one line here; the search service itself is untouched.
 */
final class SearchFilterAssembler {

    private SearchFilterAssembler() {
    }

    static List<SearchFilter> assemble(BigDecimal minPrice, BigDecimal maxPrice, Set<Amenity> amenities,
                                       Integer minStars) {
        List<SearchFilter> filters = new ArrayList<>();
        if (minPrice != null || maxPrice != null) {
            filters.add(new PriceRangeFilter(inr(minPrice), inr(maxPrice)));
        }
        if (amenities != null && !amenities.isEmpty()) {
            filters.add(new AmenitiesFilter(amenities));
        }
        if (minStars != null) {
            filters.add(new MinimumStarRatingFilter(new StarRating(minStars)));
        }
        return filters;
    }

    private static Money inr(BigDecimal amount) {
        return amount == null ? null : Money.of(amount, Money.INR);
    }
}
