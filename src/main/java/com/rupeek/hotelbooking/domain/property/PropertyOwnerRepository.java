package com.rupeek.hotelbooking.domain.property;

import java.util.Optional;

public interface PropertyOwnerRepository {

    PropertyOwner save(PropertyOwner owner);

    Optional<PropertyOwner> findById(String id);
}
