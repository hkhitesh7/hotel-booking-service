package com.rupeek.hotelbooking.domain.common;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

/**
 * A stay expressed as [checkIn, checkOut). The check-out day is not an occupied night, so
 * back-to-back stays (one checks out on the day the next checks in) do not overlap.
 */
public record DateRange(LocalDate checkIn, LocalDate checkOut) {

    public static final int MAX_NIGHTS = 30;

    public DateRange {
        Guard.requireNonNull(checkIn, "checkIn");
        Guard.requireNonNull(checkOut, "checkOut");
        Guard.require(checkOut.isAfter(checkIn), "checkOut must be after checkIn");
        Guard.require(ChronoUnit.DAYS.between(checkIn, checkOut) <= MAX_NIGHTS,
                "a stay cannot exceed " + MAX_NIGHTS + " nights");
    }

    public long nights() {
        return ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    /** Every occupied night of the stay. */
    public Stream<LocalDate> nightsStream() {
        return checkIn.datesUntil(checkOut);
    }

    public boolean overlaps(DateRange other) {
        return checkIn.isBefore(other.checkOut) && other.checkIn.isBefore(checkOut);
    }
}
