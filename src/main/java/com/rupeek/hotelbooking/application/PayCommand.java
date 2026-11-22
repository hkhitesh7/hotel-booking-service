package com.rupeek.hotelbooking.application;

import com.rupeek.hotelbooking.domain.payment.PaymentMethod;

import java.util.Map;

public record PayCommand(String bookingId, PaymentMethod method, Map<String, String> details, String idempotencyKey) {
}
