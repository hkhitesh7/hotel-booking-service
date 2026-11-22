package com.rupeek.hotelbooking.api.dto;

import com.rupeek.hotelbooking.application.OnboardingCommands.NewProperty;
import com.rupeek.hotelbooking.application.OnboardingCommands.NewRoomType;
import com.rupeek.hotelbooking.application.OnboardingCommands.OnboardOwner;
import com.rupeek.hotelbooking.domain.common.Money;
import com.rupeek.hotelbooking.domain.property.Amenity;
import com.rupeek.hotelbooking.domain.property.Location;
import com.rupeek.hotelbooking.domain.property.OwnerType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public final class OnboardingRequests {

    private static final String DEFAULT_POLICY = "FLEXIBLE";

    private OnboardingRequests() {
    }

    /** A standalone owner simply sends a single-element {@code properties} list. */
    public record OnboardOwnerRequest(@NotBlank String name, @NotNull OwnerType type,
                                      List<@Valid PropertyRequest> properties) {

        public OnboardOwner toCommand() {
            List<NewProperty> newProperties = properties == null ? List.of()
                    : properties.stream().map(PropertyRequest::toCommand).toList();
            return new OnboardOwner(name, type, newProperties);
        }
    }

    public record PropertyRequest(@NotBlank String name, @NotNull @Valid LocationRequest location,
                                  @Min(1) @Max(5) int stars, Set<Amenity> amenities,
                                  @NotEmpty List<@Valid RoomTypeRequest> roomTypes, String cancellationPolicy) {

        public NewProperty toCommand() {
            String policy = cancellationPolicy == null || cancellationPolicy.isBlank()
                    ? DEFAULT_POLICY : cancellationPolicy;
            return new NewProperty(name, location.toDomain(), stars, amenities,
                    roomTypes.stream().map(RoomTypeRequest::toCommand).toList(), policy);
        }
    }

    public record LocationRequest(@NotBlank String city, @NotBlank String locality, String addressLine) {

        Location toDomain() {
            return new Location(city, locality, addressLine);
        }
    }

    public record RoomTypeRequest(@NotBlank String name, @Positive int maxOccupancy, @Positive int totalRooms,
                                  @NotNull @Positive BigDecimal nightlyRate) {

        NewRoomType toCommand() {
            return new NewRoomType(name, maxOccupancy, totalRooms, Money.of(nightlyRate, Money.INR));
        }
    }
}
