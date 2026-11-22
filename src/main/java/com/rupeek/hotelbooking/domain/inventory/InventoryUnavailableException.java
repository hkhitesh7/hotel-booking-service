package com.rupeek.hotelbooking.domain.inventory;

import com.rupeek.hotelbooking.domain.common.ConflictException;

public class InventoryUnavailableException extends ConflictException {

    public InventoryUnavailableException(InventoryKey key, int requested, int available) {
        super("Not enough rooms for " + key.asString() + ": requested " + requested + ", available " + available);
    }
}
