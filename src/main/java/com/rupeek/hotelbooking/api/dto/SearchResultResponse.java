package com.rupeek.hotelbooking.api.dto;

import com.rupeek.hotelbooking.domain.property.Amenity;
import com.rupeek.hotelbooking.domain.property.Location;
import com.rupeek.hotelbooking.domain.search.PropertyAvailability;
import com.rupeek.hotelbooking.domain.search.RoomOffer;

import java.util.List;
import java.util.Set;

public record SearchResultResponse(String propertyId, String name, Location location, int stars,
                                   Set<Amenity> amenities, List<OfferResponse> offers) {

    public static SearchResultResponse from(PropertyAvailability availability) {
        var property = availability.property();
        return new SearchResultResponse(property.id(), property.name(), property.location(),
                property.starRating().stars(), property.amenities(),
                availability.offers().stream().map(OfferResponse::from).toList());
    }

    public record OfferResponse(String roomTypeId, String roomTypeName, int maxOccupancy, int roomsRequired,
                                int roomsAvailable, MoneyDto averageNightlyRate, MoneyDto totalPrice) {

        static OfferResponse from(RoomOffer offer) {
            return new OfferResponse(offer.roomType().id(), offer.roomType().name(), offer.roomType().maxOccupancy(),
                    offer.roomsRequired(), offer.roomsAvailable(),
                    MoneyDto.from(offer.price().averageNightlyRate()), MoneyDto.from(offer.price().total()));
        }
    }
}
