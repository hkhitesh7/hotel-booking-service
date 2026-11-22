package com.rupeek.hotelbooking.domain.payment;

import com.rupeek.hotelbooking.domain.common.Money;

import java.util.Map;

/** Method-agnostic charge request; {@code details} carries method-specific fields (card number, VPA...). */
public record ChargeRequest(String bookingId, Money amount, Map<String, String> details, String idempotencyKey) {

    public ChargeRequest {
        details = details == null ? Map.of() : Map.copyOf(details);
    }

    public String detail(String name) {
        return details.get(name);
    }
}
