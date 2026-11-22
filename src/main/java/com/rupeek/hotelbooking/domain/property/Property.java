package com.rupeek.hotelbooking.domain.property;

import com.rupeek.hotelbooking.domain.common.Guard;
import com.rupeek.hotelbooking.domain.common.NotFoundException;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** A bookable property. Immutable once onboarded; inventory state lives in the inventory module. */
public final class Property {

    private final String id;
    private final String ownerId;
    private final String name;
    private final Location location;
    private final StarRating starRating;
    private final Set<Amenity> amenities;
    private final List<RoomType> roomTypes;
    private final String cancellationPolicyCode;

    public Property(String id, String ownerId, String name, Location location, StarRating starRating,
                    Set<Amenity> amenities, List<RoomType> roomTypes, String cancellationPolicyCode) {
        this.id = Guard.requireText(id, "property.id");
        this.ownerId = Guard.requireText(ownerId, "ownerId");
        this.name = Guard.requireText(name, "property.name");
        this.location = Guard.requireNonNull(location, "location");
        this.starRating = Guard.requireNonNull(starRating, "starRating");
        this.amenities = amenities == null || amenities.isEmpty()
                ? Set.of() : Set.copyOf(EnumSet.copyOf(amenities));
        Guard.require(roomTypes != null && !roomTypes.isEmpty(), "a property needs at least one room type");
        assertUniqueRoomTypeIds(roomTypes);
        this.roomTypes = List.copyOf(roomTypes);
        this.cancellationPolicyCode = Guard.requireText(cancellationPolicyCode, "cancellationPolicy");
    }

    private static void assertUniqueRoomTypeIds(List<RoomType> roomTypes) {
        long distinct = roomTypes.stream().map(RoomType::id).distinct().count();
        Guard.require(distinct == roomTypes.size(), "room type ids must be unique within a property");
    }

    public RoomType roomType(String roomTypeId) {
        return findRoomType(roomTypeId)
                .orElseThrow(() -> new NotFoundException("RoomType", id + "/" + roomTypeId));
    }

    public Optional<RoomType> findRoomType(String roomTypeId) {
        return roomTypes.stream().filter(rt -> rt.id().equals(roomTypeId)).findFirst();
    }

    public boolean hasAllAmenities(Set<Amenity> required) {
        return amenities.containsAll(required);
    }

    public String id() {
        return id;
    }

    public String ownerId() {
        return ownerId;
    }

    public String name() {
        return name;
    }

    public Location location() {
        return location;
    }

    public StarRating starRating() {
        return starRating;
    }

    public Set<Amenity> amenities() {
        return amenities;
    }

    public List<RoomType> roomTypes() {
        return roomTypes;
    }

    public String cancellationPolicyCode() {
        return cancellationPolicyCode;
    }

    public Set<String> roomTypeIds() {
        return roomTypes.stream().map(RoomType::id).collect(Collectors.toUnmodifiableSet());
    }
}
