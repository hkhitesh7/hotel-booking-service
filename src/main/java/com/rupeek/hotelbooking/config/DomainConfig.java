package com.rupeek.hotelbooking.config;

import com.rupeek.hotelbooking.domain.cancellation.CancellationPolicy;
import com.rupeek.hotelbooking.domain.cancellation.NonRefundableCancellationPolicy;
import com.rupeek.hotelbooking.domain.cancellation.RefundTier;
import com.rupeek.hotelbooking.domain.cancellation.TieredCancellationPolicy;
import com.rupeek.hotelbooking.domain.pricing.BaseRatePricingStrategy;
import com.rupeek.hotelbooking.domain.pricing.PriceCalculator;
import com.rupeek.hotelbooking.domain.pricing.PricingStrategy;
import com.rupeek.hotelbooking.domain.pricing.WeekendSurchargePricingStrategy;
import com.rupeek.hotelbooking.support.IdGenerator;
import com.rupeek.hotelbooking.support.KeyedLocks;
import com.rupeek.hotelbooking.support.UuidIdGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.List;

/**
 * Wires framework-free domain objects into Spring. Swapping a pricing rule or adding a
 * cancellation policy is a change here (or a new bean), never in the services.
 */
@Configuration
public class DomainConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public IdGenerator idGenerator() {
        return new UuidIdGenerator();
    }

    /** Shared by every service that changes booking state, so pay / cancel / expire never interleave. */
    @Bean
    public KeyedLocks bookingLocks() {
        return new KeyedLocks(256);
    }

    @Bean
    public PricingStrategy pricingStrategy(@Value("${pricing.weekend-surcharge-percent:20}") int weekendSurcharge) {
        return new WeekendSurchargePricingStrategy(new BaseRatePricingStrategy(), weekendSurcharge);
    }

    @Bean
    public PriceCalculator priceCalculator(PricingStrategy pricingStrategy) {
        return new PriceCalculator(pricingStrategy);
    }

    @Bean
    public CancellationPolicy flexibleCancellationPolicy() {
        return new TieredCancellationPolicy("FLEXIBLE", List.of(new RefundTier(2, 100), new RefundTier(1, 50)));
    }

    @Bean
    public CancellationPolicy moderateCancellationPolicy() {
        return new TieredCancellationPolicy("MODERATE", List.of(new RefundTier(7, 100), new RefundTier(3, 50)));
    }

    @Bean
    public CancellationPolicy nonRefundableCancellationPolicy() {
        return new NonRefundableCancellationPolicy();
    }
}
