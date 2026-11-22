package com.rupeek.hotelbooking.application;

import com.rupeek.hotelbooking.application.OnboardingCommands.NewProperty;
import com.rupeek.hotelbooking.application.OnboardingCommands.NewRoomType;
import com.rupeek.hotelbooking.application.OnboardingCommands.OnboardOwner;
import com.rupeek.hotelbooking.domain.common.InvalidRequestException;
import com.rupeek.hotelbooking.domain.common.Money;
import com.rupeek.hotelbooking.domain.property.Location;
import com.rupeek.hotelbooking.domain.property.OwnerType;
import com.rupeek.hotelbooking.domain.property.Property;
import com.rupeek.hotelbooking.domain.property.PropertyOwner;
import com.rupeek.hotelbooking.testsupport.Fixture;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropertyOnboardingServiceTest {

    private final Fixture f = new Fixture();

    private static NewProperty property(String name, String policy, List<NewRoomType> roomTypes) {
        return new NewProperty(name, new Location("Goa", "Calangute", null), 4, Set.of(), roomTypes, policy);
    }

    private static List<NewRoomType> oneRoomType() {
        return List.of(new NewRoomType("Sea View", 2, 4, Money.inr("5000")));
    }

    @Test
    void chainOnboardsManyPropertiesUnderOneAccount() {
        PropertyOwner chain = f.onboarding.onboardOwner(new OnboardOwner("Coastal Group", OwnerType.CHAIN, List.of(
                property("Coastal North", "FLEXIBLE", oneRoomType()),
                property("Coastal South", "NON_REFUNDABLE", oneRoomType()))));

        assertThat(chain.propertyIds()).hasSize(2);
        chain.propertyIds().forEach(id -> assertThat(f.onboarding.getProperty(id).ownerId()).isEqualTo(chain.id()));
    }

    @Test
    void standaloneIsAPortfolioOfOneAndCanGrowLater() {
        PropertyOwner owner = f.onboarding.onboardOwner(new OnboardOwner("Solo", OwnerType.INDIVIDUAL,
                List.of(property("Solo Villa", "FLEXIBLE", oneRoomType()))));
        assertThat(owner.propertyIds()).hasSize(1);

        Property second = f.onboarding.addProperty(owner.id(), property("Solo Villa 2", "FLEXIBLE", oneRoomType()));

        assertThat(f.onboarding.getOwner(owner.id()).propertyIds()).hasSize(2).contains(second.id());
    }

    @Test
    void onboardedPropertyIsImmediatelyBookable() {
        PropertyOwner owner = f.onboarding.onboardOwner(new OnboardOwner("Solo", OwnerType.INDIVIDUAL,
                List.of(property("Solo Villa", "FLEXIBLE", oneRoomType()))));
        Property property = f.onboarding.getProperty(owner.propertyIds().iterator().next());

        assertThat(f.book(property, 3, 2).totalAmount()).isEqualTo(Money.inr("10000"));
    }

    @Test
    void invalidPropertyRejectsWholeOnboardingAtomically() {
        OnboardOwner command = new OnboardOwner("Broken Chain", OwnerType.CHAIN, List.of(
                property("Good", "FLEXIBLE", oneRoomType()),
                property("Bad", "NO_SUCH_POLICY", oneRoomType())));

        assertThatThrownBy(() -> f.onboarding.onboardOwner(command)).isInstanceOf(InvalidRequestException.class);
        assertThat(f.properties.findByLocation("Goa", null)).isEmpty();
    }

    @Test
    void propertyNeedsAtLeastOneValidRoomType() {
        assertThatThrownBy(() -> f.onboarding.onboardOwner(new OnboardOwner("X", OwnerType.INDIVIDUAL,
                List.of(property("Empty", "FLEXIBLE", List.of()))))).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> f.onboarding.onboardOwner(new OnboardOwner("X", OwnerType.INDIVIDUAL,
                List.of(property("NoRooms", "FLEXIBLE", List.of(new NewRoomType("Zero", 2, 0, Money.inr("1"))))))))
                .isInstanceOf(InvalidRequestException.class);
    }
}
