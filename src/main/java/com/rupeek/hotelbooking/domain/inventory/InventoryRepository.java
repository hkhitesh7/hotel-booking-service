package com.rupeek.hotelbooking.domain.inventory;

import java.util.Optional;

public interface InventoryRepository {

    RoomInventory save(RoomInventory inventory);

    Optional<RoomInventory> findByKey(InventoryKey key);
}
