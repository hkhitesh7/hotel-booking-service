package com.rupeek.hotelbooking.api.dto;

import com.rupeek.hotelbooking.application.PaymentResult;
import com.rupeek.hotelbooking.domain.payment.Payment;
import com.rupeek.hotelbooking.domain.payment.PaymentStatus;

public record PaymentResponse(String paymentId, String method, PaymentStatus status, MoneyDto amount,
                              String instrument, String failureReason, boolean replayed, BookingResponse booking) {

    public static PaymentResponse from(PaymentResult result) {
        Payment payment = result.payment();
        return new PaymentResponse(payment.id(), payment.method().code(), payment.status(),
                MoneyDto.from(payment.amount()), payment.instrumentSummary(), payment.failureReason().orElse(null),
                result.replayed(), BookingResponse.from(result.booking()));
    }
}
