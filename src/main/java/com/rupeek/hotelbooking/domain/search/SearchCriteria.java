package com.rupeek.hotelbooking.domain.search;

import com.rupeek.hotelbooking.domain.common.DateRange;
import com.rupeek.hotelbooking.domain.common.Guard;

import java.util.List;

/** Mandatory criteria (where, when, how many) plus an open list of optional filters. */
public record SearchCriteria(String city, String locality, DateRange stay, int guests, List<SearchFilter> filters) {

    public SearchCriteria {
        city = Guard.requireText(city, "city");
        Guard.requireNonNull(stay, "stay");
        Guard.require(guests > 0, "guests must be positive");
        filters = filters == null ? List.of() : List.copyOf(filters);
    }
}
