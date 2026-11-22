package com.rupeek.hotelbooking.infrastructure.persistence;

import com.rupeek.hotelbooking.domain.property.Property;
import com.rupeek.hotelbooking.domain.property.PropertyRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryPropertyRepository implements PropertyRepository {

    private final Map<String, Property> properties = new ConcurrentHashMap<>();

    @Override
    public Property save(Property property) {
        properties.put(property.id(), property);
        return property;
    }

    @Override
    public Optional<Property> findById(String id) {
        return Optional.ofNullable(properties.get(id));
    }

    /** A real store would index by city; a scan is fine for an in-memory demo. */
    @Override
    public List<Property> findByLocation(String city, String locality) {
        return properties.values().stream().filter(p -> p.location().isIn(city, locality)).toList();
    }
}
