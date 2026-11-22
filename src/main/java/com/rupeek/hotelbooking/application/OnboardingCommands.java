package com.rupeek.hotelbooking.application;

import com.rupeek.hotelbooking.domain.common.Money;
import com.rupeek.hotelbooking.domain.property.Amenity;
import com.rupeek.hotelbooking.domain.property.Location;
import com.rupeek.hotelbooking.domain.property.OwnerType;

import java.util.List;
import java.util.Set;

/** Input models for onboarding, independent of the REST representation. */
public final class OnboardingCommands {

    private OnboardingCommands() {
    }

    public record OnboardOwner(String name, OwnerType type, List<NewProperty> properties) {

        public OnboardOwner {
            properties = properties == null ? List.of() : List.copyOf(properties);
        }
    }

    public record NewProperty(String name, Location location, int stars, Set<Amenity> amenities,
                              List<NewRoomType> roomTypes, String cancellationPolicy) {
    }

    public record NewRoomType(String name, int maxOccupancy, int totalRooms, Money baseNightlyRate) {
    }
}
