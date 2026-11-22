package com.rupeek.hotelbooking.application;

import com.rupeek.hotelbooking.domain.inventory.InventoryKey;
import com.rupeek.hotelbooking.domain.pricing.PriceCalculator;
import com.rupeek.hotelbooking.domain.property.Property;
import com.rupeek.hotelbooking.domain.property.PropertyRepository;
import com.rupeek.hotelbooking.domain.property.RoomType;
import com.rupeek.hotelbooking.domain.search.PropertyAvailability;
import com.rupeek.hotelbooking.domain.search.RoomOffer;
import com.rupeek.hotelbooking.domain.search.SearchCriteria;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Discovery pipeline: location -> availability for the dates/party -> pluggable filters -> sort.
 * Only room types with enough free rooms on every night are offered.
 */
@Service
public class PropertySearchService {

    private final PropertyRepository propertyRepository;
    private final InventoryService inventoryService;
    private final PriceCalculator priceCalculator;

    public PropertySearchService(PropertyRepository propertyRepository, InventoryService inventoryService,
                                 PriceCalculator priceCalculator) {
        this.propertyRepository = propertyRepository;
        this.inventoryService = inventoryService;
        this.priceCalculator = priceCalculator;
    }

    public List<PropertyAvailability> search(SearchCriteria criteria) {
        return propertyRepository.findByLocation(criteria.city(), criteria.locality()).stream()
                .map(property -> availabilityOf(property, criteria))
                .filter(PropertyAvailability::hasOffers)
                .filter(candidate -> criteria.filters().stream().allMatch(filter -> filter.matches(candidate)))
                .sorted(Comparator.comparing((PropertyAvailability c) -> c.cheapestTotal().orElseThrow()))
                .toList();
    }

    private PropertyAvailability availabilityOf(Property property, SearchCriteria criteria) {
        List<RoomOffer> offers = property.roomTypes().stream()
                .map(roomType -> offerFor(property, roomType, criteria))
                .flatMap(Optional::stream)
                .toList();
        return new PropertyAvailability(property, offers);
    }

    private Optional<RoomOffer> offerFor(Property property, RoomType roomType, SearchCriteria criteria) {
        int roomsRequired = roomType.roomsNeededFor(criteria.guests());
        int available = inventoryService.availableRooms(new InventoryKey(property.id(), roomType.id()), criteria.stay());
        if (available < roomsRequired) {
            return Optional.empty();
        }
        return Optional.of(new RoomOffer(roomType, roomsRequired, available,
                priceCalculator.quote(property, roomType, criteria.stay(), roomsRequired)));
    }
}
