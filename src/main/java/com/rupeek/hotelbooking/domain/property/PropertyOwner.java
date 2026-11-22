package com.rupeek.hotelbooking.domain.property;

import com.rupeek.hotelbooking.domain.common.Guard;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * An account that owns and manages a portfolio of properties. A standalone property is the
 * degenerate case of a portfolio of size one; there is no separate "single property" path.
 */
public final class PropertyOwner {

    private final String id;
    private final String name;
    private final OwnerType type;
    private final Set<String> propertyIds = new LinkedHashSet<>();

    public PropertyOwner(String id, String name, OwnerType type) {
        this.id = Guard.requireText(id, "owner.id");
        this.name = Guard.requireText(name, "owner.name");
        this.type = Guard.requireNonNull(type, "owner.type");
    }

    public synchronized void addProperty(String propertyId) {
        Guard.requireText(propertyId, "propertyId");
        propertyIds.add(propertyId);
    }

    public boolean owns(String propertyId) {
        return propertyIds.contains(propertyId);
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public OwnerType type() {
        return type;
    }

    public synchronized Set<String> propertyIds() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(propertyIds));
    }
}
