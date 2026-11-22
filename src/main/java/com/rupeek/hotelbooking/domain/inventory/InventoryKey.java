package com.rupeek.hotelbooking.domain.inventory;

import com.rupeek.hotelbooking.domain.common.Guard;

/** Identifies one pool of interchangeable rooms: a room type within a property. */
public record InventoryKey(String propertyId, String roomTypeId) {

    public InventoryKey {
        Guard.requireText(propertyId, "propertyId");
        Guard.requireText(roomTypeId, "roomTypeId");
    }

    public String asString() {
        return propertyId + "/" + roomTypeId;
    }
}
