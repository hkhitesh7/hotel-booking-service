package com.rupeek.hotelbooking.domain.payment;

import com.rupeek.hotelbooking.domain.common.Money;

/** Port to the external payment provider. The only implementation today is a mock. */
public interface PaymentGateway {

    GatewayResponse charge(GatewayChargeRequest request);

    GatewayResponse refund(String transactionId, Money amount);

    record GatewayChargeRequest(String merchantReference, Money amount, String instrumentReference,
                                String idempotencyKey) {
    }

    record GatewayResponse(boolean approved, String transactionId, String message) {
    }
}
