package com.rupeek.hotelbooking.application;

import com.rupeek.hotelbooking.application.OnboardingCommands.NewProperty;
import com.rupeek.hotelbooking.application.OnboardingCommands.NewRoomType;
import com.rupeek.hotelbooking.application.OnboardingCommands.OnboardOwner;
import com.rupeek.hotelbooking.domain.common.NotFoundException;
import com.rupeek.hotelbooking.domain.property.Property;
import com.rupeek.hotelbooking.domain.property.PropertyOwner;
import com.rupeek.hotelbooking.domain.property.PropertyOwnerRepository;
import com.rupeek.hotelbooking.domain.property.PropertyRepository;
import com.rupeek.hotelbooking.domain.property.RoomType;
import com.rupeek.hotelbooking.domain.property.StarRating;
import com.rupeek.hotelbooking.support.IdGenerator;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Onboards owners and their properties. A standalone hotel and a 50-property chain go through
 * exactly the same path: an owner plus a list of properties (of size one, or many).
 */
@Service
public class PropertyOnboardingService {

    private final PropertyOwnerRepository ownerRepository;
    private final PropertyRepository propertyRepository;
    private final InventoryService inventoryService;
    private final CancellationPolicyRegistry cancellationPolicies;
    private final IdGenerator ids;

    public PropertyOnboardingService(PropertyOwnerRepository ownerRepository, PropertyRepository propertyRepository,
                                     InventoryService inventoryService, CancellationPolicyRegistry cancellationPolicies,
                                     IdGenerator ids) {
        this.ownerRepository = ownerRepository;
        this.propertyRepository = propertyRepository;
        this.inventoryService = inventoryService;
        this.cancellationPolicies = cancellationPolicies;
        this.ids = ids;
    }

    public PropertyOwner onboardOwner(OnboardOwner command) {
        PropertyOwner owner = new PropertyOwner(ids.next("own"), command.name(), command.type());
        // Build (and therefore validate) every property before persisting anything.
        List<Property> properties = command.properties().stream().map(p -> buildProperty(owner.id(), p)).toList();
        ownerRepository.save(owner);
        properties.forEach(property -> attach(owner, property));
        return owner;
    }

    public Property addProperty(String ownerId, NewProperty command) {
        PropertyOwner owner = getOwner(ownerId);
        Property property = buildProperty(owner.id(), command);
        attach(owner, property);
        return property;
    }

    public PropertyOwner getOwner(String ownerId) {
        return ownerRepository.findById(ownerId).orElseThrow(() -> new NotFoundException("Owner", ownerId));
    }

    public Property getProperty(String propertyId) {
        return propertyRepository.findById(propertyId)
                .orElseThrow(() -> new NotFoundException("Property", propertyId));
    }

    private Property buildProperty(String ownerId, NewProperty command) {
        String policyCode = cancellationPolicies.resolve(command.cancellationPolicy()).code();
        List<RoomType> roomTypes = command.roomTypes() == null ? List.of()
                : command.roomTypes().stream().map(this::buildRoomType).toList();
        return new Property(ids.next("prop"), ownerId, command.name(), command.location(),
                new StarRating(command.stars()), command.amenities(), roomTypes, policyCode);
    }

    private RoomType buildRoomType(NewRoomType command) {
        return new RoomType(ids.next("rt"), command.name(), command.maxOccupancy(), command.totalRooms(),
                command.baseNightlyRate());
    }

    private void attach(PropertyOwner owner, Property property) {
        propertyRepository.save(property);
        inventoryService.registerProperty(property);
        owner.addProperty(property.id());
        ownerRepository.save(owner);
    }
}
