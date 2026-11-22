package com.rupeek.hotelbooking.domain.pricing;

import com.rupeek.hotelbooking.domain.common.Money;

/** Total for the stay plus the per-room, per-night average used for display and price filters. */
public record PriceQuote(Money total, Money averageNightlyRate) {
}
