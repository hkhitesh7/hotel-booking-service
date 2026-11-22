package com.rupeek.hotelbooking.domain.inventory;

import com.rupeek.hotelbooking.domain.common.DateRange;
import com.rupeek.hotelbooking.domain.common.Guard;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Night-by-night allocation ledger for one room type. A stay can be reserved only if every
 * night in it has enough free rooms; this is what prevents double-booking of overlapping dates.
 *
 * <p>Mutations ({@link #reserve}, {@link #release}) must be serialised by the caller (see
 * {@code InventoryService}); the backing map is concurrent so lock-free reads used by search
 * never see a corrupted structure, at worst a momentarily stale count.
 */
public final class RoomInventory {

    private final InventoryKey key;
    private final int totalRooms;
    private final Map<LocalDate, Integer> reservedByNight = new ConcurrentHashMap<>();

    public RoomInventory(InventoryKey key, int totalRooms) {
        this.key = Guard.requireNonNull(key, "key");
        Guard.require(totalRooms > 0, "totalRooms must be positive");
        this.totalRooms = totalRooms;
    }

    /** Rooms free on every night of the stay, i.e. the bottleneck night decides. */
    public int availableFor(DateRange stay) {
        int maxReserved = stay.nightsStream().mapToInt(this::reservedOn).max().orElse(0);
        return totalRooms - maxReserved;
    }

    public void reserve(DateRange stay, int rooms) {
        Guard.require(rooms > 0, "rooms must be positive");
        int available = availableFor(stay);
        if (available < rooms) {
            throw new InventoryUnavailableException(key, rooms, available);
        }
        stay.nightsStream().forEach(night -> reservedByNight.merge(night, rooms, Integer::sum));
    }

    public void release(DateRange stay, int rooms) {
        Guard.require(rooms > 0, "rooms must be positive");
        boolean consistent = stay.nightsStream().allMatch(night -> reservedOn(night) >= rooms);
        if (!consistent) {
            throw new IllegalStateException("Releasing more rooms than reserved for " + key.asString());
        }
        stay.nightsStream().forEach(night -> reservedByNight.computeIfPresent(night,
                (n, reserved) -> reserved == rooms ? null : reserved - rooms));
    }

    public int reservedOn(LocalDate night) {
        return reservedByNight.getOrDefault(night, 0);
    }

    public InventoryKey key() {
        return key;
    }

    public int totalRooms() {
        return totalRooms;
    }
}
