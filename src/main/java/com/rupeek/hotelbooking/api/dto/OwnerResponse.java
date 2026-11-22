package com.rupeek.hotelbooking.api.dto;

import com.rupeek.hotelbooking.domain.property.OwnerType;
import com.rupeek.hotelbooking.domain.property.PropertyOwner;

import java.util.Set;

public record OwnerResponse(String id, String name, OwnerType type, Set<String> propertyIds) {

    public static OwnerResponse from(PropertyOwner owner) {
        return new OwnerResponse(owner.id(), owner.name(), owner.type(), owner.propertyIds());
    }
}
