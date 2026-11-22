package com.rupeek.hotelbooking.domain.property;

import com.rupeek.hotelbooking.domain.common.Guard;
import com.rupeek.hotelbooking.domain.common.Money;

/**
 * A category of identical rooms within a property (e.g. "Deluxe King"). It is the unit of
 * inventory: {@code totalRooms} interchangeable rooms, each sleeping up to {@code maxOccupancy}.
 */
public record RoomType(String id, String name, int maxOccupancy, int totalRooms, Money baseNightlyRate) {

    public RoomType {
        id = Guard.requireText(id, "roomType.id");
        name = Guard.requireText(name, "roomType.name");
        Guard.require(maxOccupancy > 0, "maxOccupancy must be positive");
        Guard.require(totalRooms > 0, "totalRooms must be positive");
        Guard.requireNonNull(baseNightlyRate, "baseNightlyRate");
        Guard.require(!baseNightlyRate.isZero(), "baseNightlyRate must be positive");
    }

    /** Minimum number of rooms of this type needed to sleep {@code guests} people. */
    public int roomsNeededFor(int guests) {
        Guard.require(guests > 0, "guests must be positive");
        return (guests + maxOccupancy - 1) / maxOccupancy;
    }

    public boolean canAccommodate(int guests, int rooms) {
        return rooms > 0 && guests > 0 && guests <= (long) rooms * maxOccupancy;
    }
}
