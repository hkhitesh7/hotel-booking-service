package com.rupeek.hotelbooking.domain;

import com.rupeek.hotelbooking.domain.booking.Booking;
import com.rupeek.hotelbooking.domain.cancellation.CancellationPolicy;
import com.rupeek.hotelbooking.domain.cancellation.NonRefundableCancellationPolicy;
import com.rupeek.hotelbooking.domain.cancellation.RefundTier;
import com.rupeek.hotelbooking.domain.cancellation.TieredCancellationPolicy;
import com.rupeek.hotelbooking.domain.common.DateRange;
import com.rupeek.hotelbooking.domain.common.Money;
import com.rupeek.hotelbooking.domain.inventory.InventoryKey;
import com.rupeek.hotelbooking.domain.pricing.BaseRatePricingStrategy;
import com.rupeek.hotelbooking.domain.pricing.PriceCalculator;
import com.rupeek.hotelbooking.domain.pricing.PriceQuote;
import com.rupeek.hotelbooking.domain.pricing.WeekendSurchargePricingStrategy;
import com.rupeek.hotelbooking.domain.property.Location;
import com.rupeek.hotelbooking.domain.property.Property;
import com.rupeek.hotelbooking.domain.property.RoomType;
import com.rupeek.hotelbooking.domain.property.StarRating;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PoliciesAndPricingTest {

    private static final LocalDate CHECK_IN = LocalDate.of(2026, 12, 10);
    private static final RoomType STANDARD = new RoomType("rt", "Standard", 2, 5, Money.inr("1000"));
    private static final Property PROPERTY = new Property("p", "o", "Inn", new Location("Pune", "Baner", null),
            new StarRating(3), Set.of(), List.of(STANDARD), "FLEXIBLE");

    private static Booking booking(String amount) {
        Instant now = Instant.parse("2026-12-01T00:00:00Z");
        return Booking.pendingPayment("bk", "g", new InventoryKey("p", "rt"),
                new DateRange(CHECK_IN, CHECK_IN.plusDays(2)), 2, 1, Money.inr(amount), now, now.plusSeconds(60));
    }

    @ParameterizedTest(name = "cancel {0} day(s) before check-in -> refund {1}")
    @CsvSource({"5, 2000.00", "2, 2000.00", "1, 1000.00", "0, 0.00"})
    void tieredPolicyRefundsByNoticeGiven(int daysBefore, String expectedRefund) {
        CancellationPolicy policy = new TieredCancellationPolicy("FLEXIBLE",
                List.of(new RefundTier(1, 50), new RefundTier(2, 100)));
        assertThat(policy.evaluate(booking("2000"), CHECK_IN.minusDays(daysBefore)).amount())
                .isEqualTo(Money.inr(expectedRefund));
    }

    @Test
    void nonRefundablePolicyNeverRefunds() {
        assertThat(new NonRefundableCancellationPolicy().evaluate(booking("2000"), CHECK_IN.minusDays(30))
                .isRefundDue()).isFalse();
    }

    @Test
    void quoteMultipliesNightlyRateByNightsAndRooms() {
        PriceQuote quote = new PriceCalculator(new BaseRatePricingStrategy())
                .quote(PROPERTY, STANDARD, new DateRange(CHECK_IN, CHECK_IN.plusDays(3)), 2);
        assertThat(quote.total()).isEqualTo(Money.inr("6000"));
        assertThat(quote.averageNightlyRate()).isEqualTo(Money.inr("1000"));
    }

    @Test
    void weekendSurchargeAppliesOnlyToFridayAndSaturdayNights() {
        LocalDate thursday = LocalDate.of(2026, 12, 10);
        PriceCalculator calculator = new PriceCalculator(
                new WeekendSurchargePricingStrategy(new BaseRatePricingStrategy(), 20));
        // Thu, Fri, Sat, Sun nights: 1000 + 1200 + 1200 + 1000
        PriceQuote quote = calculator.quote(PROPERTY, STANDARD, new DateRange(thursday, thursday.plusDays(4)), 1);
        assertThat(quote.total()).isEqualTo(Money.inr("4400"));
        assertThat(quote.averageNightlyRate()).isEqualTo(Money.inr("1100"));
    }
}
