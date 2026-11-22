package com.rupeek.hotelbooking.api;

import com.rupeek.hotelbooking.api.dto.PropertyResponse;
import com.rupeek.hotelbooking.api.dto.SearchResultResponse;
import com.rupeek.hotelbooking.application.PropertyOnboardingService;
import com.rupeek.hotelbooking.application.PropertySearchService;
import com.rupeek.hotelbooking.domain.common.DateRange;
import com.rupeek.hotelbooking.domain.property.Amenity;
import com.rupeek.hotelbooking.domain.search.SearchCriteria;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/properties")
@Tag(name = "Discovery")
public class PropertyController {

    private final PropertySearchService searchService;
    private final PropertyOnboardingService onboarding;

    public PropertyController(PropertySearchService searchService, PropertyOnboardingService onboarding) {
        this.searchService = searchService;
        this.onboarding = onboarding;
    }

    @GetMapping("/search")
    @Operation(summary = "Find properties with rooms available for the given dates and party size")
    public List<SearchResultResponse> search(
            @RequestParam String city,
            @RequestParam(required = false) String locality,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(defaultValue = "1") int guests,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Set<Amenity> amenities,
            @RequestParam(required = false) Integer minStars) {
        SearchCriteria criteria = new SearchCriteria(city, locality, new DateRange(checkIn, checkOut), guests,
                SearchFilterAssembler.assemble(minPrice, maxPrice, amenities, minStars));
        return searchService.search(criteria).stream().map(SearchResultResponse::from).toList();
    }

    @GetMapping("/{propertyId}")
    public PropertyResponse getProperty(@PathVariable String propertyId) {
        return PropertyResponse.from(onboarding.getProperty(propertyId));
    }
}
