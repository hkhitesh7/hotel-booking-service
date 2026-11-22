package com.rupeek.hotelbooking.api.dto;

import com.rupeek.hotelbooking.domain.property.Amenity;
import com.rupeek.hotelbooking.domain.property.Location;
import com.rupeek.hotelbooking.domain.property.Property;
import com.rupeek.hotelbooking.domain.property.RoomType;

import java.util.List;
import java.util.Set;

public record PropertyResponse(String id, String ownerId, String name, Location location, int stars,
                               Set<Amenity> amenities, List<RoomTypeResponse> roomTypes, String cancellationPolicy) {

    public static PropertyResponse from(Property property) {
        return new PropertyResponse(property.id(), property.ownerId(), property.name(), property.location(),
                property.starRating().stars(), property.amenities(),
                property.roomTypes().stream().map(RoomTypeResponse::from).toList(),
                property.cancellationPolicyCode());
    }

    public record RoomTypeResponse(String id, String name, int maxOccupancy, int totalRooms, MoneyDto baseNightlyRate) {

        static RoomTypeResponse from(RoomType roomType) {
            return new RoomTypeResponse(roomType.id(), roomType.name(), roomType.maxOccupancy(),
                    roomType.totalRooms(), MoneyDto.from(roomType.baseNightlyRate()));
        }
    }
}
