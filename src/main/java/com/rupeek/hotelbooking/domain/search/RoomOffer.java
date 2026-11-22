package com.rupeek.hotelbooking.domain.search;

import com.rupeek.hotelbooking.domain.pricing.PriceQuote;
import com.rupeek.hotelbooking.domain.property.RoomType;

/** A room type that can host the requested party for the requested dates, with its price. */
public record RoomOffer(RoomType roomType, int roomsRequired, int roomsAvailable, PriceQuote price) {
}
