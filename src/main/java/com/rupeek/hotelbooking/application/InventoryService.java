package com.rupeek.hotelbooking.application;

import com.rupeek.hotelbooking.domain.booking.Booking;
import com.rupeek.hotelbooking.domain.common.DateRange;
import com.rupeek.hotelbooking.domain.common.NotFoundException;
import com.rupeek.hotelbooking.domain.inventory.InventoryKey;
import com.rupeek.hotelbooking.domain.inventory.InventoryRepository;
import com.rupeek.hotelbooking.domain.inventory.RoomInventory;
import com.rupeek.hotelbooking.domain.property.Property;
import com.rupeek.hotelbooking.domain.property.RoomType;
import com.rupeek.hotelbooking.support.KeyedLocks;
import org.springframework.stereotype.Service;

/**
 * Single entry point for touching shared room inventory. Check-and-reserve is atomic per
 * {@link InventoryKey}, which is what makes concurrent bookings for the last room safe.
 */
@Service
public class InventoryService {

    private static final int LOCK_STRIPES = 128;

    private final InventoryRepository inventoryRepository;
    private final KeyedLocks locks = new KeyedLocks(LOCK_STRIPES);

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public void registerProperty(Property property) {
        for (RoomType roomType : property.roomTypes()) {
            InventoryKey key = new InventoryKey(property.id(), roomType.id());
            inventoryRepository.save(new RoomInventory(key, roomType.totalRooms()));
        }
    }

    /** Lock-free read; may be momentarily stale, which is fine for discovery. Booking re-checks under lock. */
    public int availableRooms(InventoryKey key, DateRange stay) {
        return load(key).availableFor(stay);
    }

    public void reserve(InventoryKey key, DateRange stay, int rooms) {
        locks.withLock(key.asString(), () -> {
            RoomInventory inventory = load(key);
            inventory.reserve(stay, rooms);
            inventoryRepository.save(inventory);
        });
    }

    public void release(Booking booking) {
        InventoryKey key = booking.inventoryKey();
        locks.withLock(key.asString(), () -> {
            RoomInventory inventory = load(key);
            inventory.release(booking.stay(), booking.rooms());
            inventoryRepository.save(inventory);
        });
    }

    private RoomInventory load(InventoryKey key) {
        return inventoryRepository.findByKey(key)
                .orElseThrow(() -> new NotFoundException("Inventory", key.asString()));
    }
}
