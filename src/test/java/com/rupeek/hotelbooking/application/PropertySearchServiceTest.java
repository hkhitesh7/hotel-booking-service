package com.rupeek.hotelbooking.application;

import com.rupeek.hotelbooking.application.OnboardingCommands.NewRoomType;
import com.rupeek.hotelbooking.domain.common.DateRange;
import com.rupeek.hotelbooking.domain.common.Money;
import com.rupeek.hotelbooking.domain.property.Amenity;
import com.rupeek.hotelbooking.domain.property.Property;
import com.rupeek.hotelbooking.domain.property.StarRating;
import com.rupeek.hotelbooking.domain.search.AmenitiesFilter;
import com.rupeek.hotelbooking.domain.search.MinimumStarRatingFilter;
import com.rupeek.hotelbooking.domain.search.PriceRangeFilter;
import com.rupeek.hotelbooking.domain.search.PropertyAvailability;
import com.rupeek.hotelbooking.domain.search.SearchCriteria;
import com.rupeek.hotelbooking.domain.search.SearchFilter;
import com.rupeek.hotelbooking.testsupport.Fixture;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PropertySearchServiceTest {

    private final Fixture f = new Fixture();
    private final Property budget = f.hotel("Budget Stay", "Koramangala", 2, Set.of(Amenity.WIFI), "FLEXIBLE",
            List.of(new NewRoomType("Standard", 2, 1, Money.inr("1500"))));
    private final Property luxury = f.hotel("Grand Palace", "Indiranagar", 5,
            Set.of(Amenity.WIFI, Amenity.POOL, Amenity.SPA), "FLEXIBLE",
            List.of(new NewRoomType("Deluxe", 2, 3, Money.inr("8000")),
                    new NewRoomType("Family", 4, 1, Money.inr("12000"))));

    private DateRange stay() {
        return new DateRange(f.today().plusDays(5), f.today().plusDays(7));
    }

    private List<String> names(String locality, int guests, SearchFilter... filters) {
        return f.search.search(new SearchCriteria("bangalore", locality, stay(), guests, List.of(filters))).stream()
                .map(a -> a.property().name()).toList();
    }

    @Test
    void findsPropertiesByCityCheapestFirst() {
        assertThat(names(null, 2)).containsExactly("Budget Stay", "Grand Palace");
        assertThat(names("Indiranagar", 2)).containsExactly("Grand Palace");
        assertThat(f.search.search(new SearchCriteria("Mumbai", null, stay(), 2, List.of()))).isEmpty();
    }

    @Test
    void excludesPropertiesBookedOutForTheDates() {
        f.book(budget, 6, 1);
        assertThat(names(null, 2)).containsExactly("Grand Palace");
    }

    @Test
    void cancellationMakesPropertyDiscoverableAgain() {
        var booking = f.book(budget, 6, 1);
        f.cancellationService.cancel(booking.id());
        assertThat(names(null, 2)).contains("Budget Stay");
    }

    @Test
    void offersOnlyRoomTypesThatFitTheParty() {
        // 4 guests: Budget needs 2 rooms but has 1; Grand offers 2 Deluxe or 1 Family.
        List<PropertyAvailability> results =
                f.search.search(new SearchCriteria("Bangalore", null, stay(), 4, List.of()));

        assertThat(results).extracting(a -> a.property().name()).containsExactly("Grand Palace");
        assertThat(results.get(0).offers()).extracting(o -> o.roomType().name() + "x" + o.roomsRequired())
                .containsExactlyInAnyOrder("Deluxex2", "Familyx1");
    }

    @Test
    void appliesPluggableFilters() {
        assertThat(names(null, 2, new PriceRangeFilter(null, Money.inr("2000")))).containsExactly("Budget Stay");
        assertThat(names(null, 2, new AmenitiesFilter(Set.of(Amenity.POOL)))).containsExactly("Grand Palace");
        assertThat(names(null, 2, new MinimumStarRatingFilter(new StarRating(4)))).containsExactly("Grand Palace");
        assertThat(names(null, 2, new AmenitiesFilter(Set.of(Amenity.WIFI)),
                new PriceRangeFilter(Money.inr("9000"), null))).containsExactly("Grand Palace");
    }

    @Test
    void acceptsAdHocFiltersWithoutChangingTheService() {
        SearchFilter nameStartsWithG = candidate -> candidate.property().name().startsWith("G");
        assertThat(names(null, 2, nameStartsWithG)).containsExactly("Grand Palace");
    }
}
