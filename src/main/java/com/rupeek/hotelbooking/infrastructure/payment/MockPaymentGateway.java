package com.rupeek.hotelbooking.infrastructure.payment;

import com.rupeek.hotelbooking.domain.common.Money;
import com.rupeek.hotelbooking.domain.payment.PaymentGateway;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stand-in for a real provider. Approves everything except instruments whose reference contains
 * "decline" or ends in "0002" (mirrors the usual sandbox test-card convention). Like real gateways,
 * it is itself idempotent on the idempotency key.
 */
@Component
public class MockPaymentGateway implements PaymentGateway {

    private final Map<String, GatewayResponse> responsesByIdempotencyKey = new ConcurrentHashMap<>();

    @Override
    public GatewayResponse charge(GatewayChargeRequest request) {
        return responsesByIdempotencyKey.computeIfAbsent(request.idempotencyKey(), key -> decide(request));
    }

    @Override
    public GatewayResponse refund(String transactionId, Money amount) {
        return new GatewayResponse(true, "rf_" + shortId(), "Refunded " + amount + " against " + transactionId);
    }

    private static GatewayResponse decide(GatewayChargeRequest request) {
        String reference = request.instrumentReference().toLowerCase(Locale.ROOT);
        if (reference.contains("decline") || reference.endsWith("0002")) {
            return new GatewayResponse(false, null, "Declined by issuer");
        }
        return new GatewayResponse(true, "tx_" + shortId(), "Approved");
    }

    private static String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
