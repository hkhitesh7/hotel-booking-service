package com.rupeek.hotelbooking.infrastructure.persistence;

import com.rupeek.hotelbooking.domain.inventory.InventoryKey;
import com.rupeek.hotelbooking.domain.inventory.InventoryRepository;
import com.rupeek.hotelbooking.domain.inventory.RoomInventory;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryInventoryRepository implements InventoryRepository {

    private final Map<InventoryKey, RoomInventory> inventories = new ConcurrentHashMap<>();

    @Override
    public RoomInventory save(RoomInventory inventory) {
        inventories.put(inventory.key(), inventory);
        return inventory;
    }

    @Override
    public Optional<RoomInventory> findByKey(InventoryKey key) {
        return Optional.ofNullable(inventories.get(key));
    }
}
