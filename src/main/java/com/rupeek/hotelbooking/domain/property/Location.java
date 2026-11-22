package com.rupeek.hotelbooking.domain.property;

import com.rupeek.hotelbooking.domain.common.Guard;

public record Location(String city, String locality, String addressLine) {

    public Location {
        city = Guard.requireText(city, "city");
        locality = Guard.requireText(locality, "locality");
        addressLine = addressLine == null ? "" : addressLine.trim();
    }

    public boolean isIn(String queryCity, String queryLocality) {
        boolean cityMatches = city.equalsIgnoreCase(queryCity.trim());
        boolean localityMatches = queryLocality == null || queryLocality.isBlank()
                || locality.equalsIgnoreCase(queryLocality.trim());
        return cityMatches && localityMatches;
    }
}
