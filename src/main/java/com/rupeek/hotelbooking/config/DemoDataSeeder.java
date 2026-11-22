package com.rupeek.hotelbooking.config;

import com.rupeek.hotelbooking.application.OnboardingCommands.NewProperty;
import com.rupeek.hotelbooking.application.OnboardingCommands.NewRoomType;
import com.rupeek.hotelbooking.application.OnboardingCommands.OnboardOwner;
import com.rupeek.hotelbooking.application.PropertyOnboardingService;
import com.rupeek.hotelbooking.domain.common.Money;
import com.rupeek.hotelbooking.domain.property.Amenity;
import com.rupeek.hotelbooking.domain.property.Location;
import com.rupeek.hotelbooking.domain.property.OwnerType;
import com.rupeek.hotelbooking.domain.property.PropertyOwner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;

/** Seeds one chain and one standalone owner so the API can be explored immediately. */
@Component
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final PropertyOnboardingService onboarding;

    public DemoDataSeeder(PropertyOnboardingService onboarding) {
        this.onboarding = onboarding;
    }

    @Override
    public void run(ApplicationArguments args) {
        PropertyOwner chain = onboarding.onboardOwner(new OnboardOwner("Sunrise Hotels", OwnerType.CHAIN, List.of(
                property("Sunrise Koramangala", "Koramangala", 4,
                        EnumSet.of(Amenity.WIFI, Amenity.POOL, Amenity.BREAKFAST), "FLEXIBLE"),
                property("Sunrise Indiranagar", "Indiranagar", 3,
                        EnumSet.of(Amenity.WIFI, Amenity.PARKING), "MODERATE"))));
        PropertyOwner individual = onboarding.onboardOwner(new OnboardOwner("Asha Homestays", OwnerType.INDIVIDUAL,
                List.of(property("Asha Garden Homestay", "Jayanagar", 2,
                        EnumSet.of(Amenity.WIFI, Amenity.PET_FRIENDLY), "NON_REFUNDABLE"))));
        log.info("Seeded demo owners {} (chain) and {} (individual) in Bangalore", chain.id(), individual.id());
    }

    private static NewProperty property(String name, String locality, int stars, EnumSet<Amenity> amenities,
                                        String policy) {
        return new NewProperty(name, new Location("Bangalore", locality, null), stars, amenities, List.of(
                new NewRoomType("Standard", 2, 5, Money.inr("2500")),
                new NewRoomType("Family Suite", 4, 2, Money.inr("6000"))), policy);
    }
}
