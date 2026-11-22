package com.rupeek.hotelbooking.infrastructure.payment;

import com.rupeek.hotelbooking.domain.common.Guard;
import com.rupeek.hotelbooking.domain.common.Money;
import com.rupeek.hotelbooking.domain.payment.ChargeOutcome;
import com.rupeek.hotelbooking.domain.payment.ChargeRequest;
import com.rupeek.hotelbooking.domain.payment.Payment;
import com.rupeek.hotelbooking.domain.payment.PaymentGateway;
import com.rupeek.hotelbooking.domain.payment.PaymentGateway.GatewayChargeRequest;
import com.rupeek.hotelbooking.domain.payment.PaymentGateway.GatewayResponse;
import com.rupeek.hotelbooking.domain.payment.PaymentGatewayException;
import com.rupeek.hotelbooking.domain.payment.PaymentProcessor;

/**
 * Template for processors backed by a {@link PaymentGateway}: subclasses only validate and
 * describe their instrument; the charge/refund round-trip is shared.
 */
public abstract class GatewayPaymentProcessor implements PaymentProcessor {

    private final PaymentGateway gateway;

    protected GatewayPaymentProcessor(PaymentGateway gateway) {
        this.gateway = gateway;
    }

    /** Validates the method-specific details and returns how to reference / display the instrument. */
    protected abstract Instrument resolveInstrument(ChargeRequest request);

    @Override
    public final ChargeOutcome charge(ChargeRequest request) {
        Instrument instrument = resolveInstrument(request);
        GatewayResponse response = gateway.charge(new GatewayChargeRequest(request.bookingId(), request.amount(),
                instrument.reference(), request.idempotencyKey()));
        return response.approved()
                ? ChargeOutcome.success(response.transactionId(), instrument.summary())
                : ChargeOutcome.failure(instrument.summary(), response.message());
    }

    @Override
    public final void refund(Payment payment, Money amount) {
        String transactionId = payment.transactionId()
                .orElseThrow(() -> new IllegalStateException("Payment " + payment.id() + " was never captured"));
        GatewayResponse response = gateway.refund(transactionId, amount);
        if (!response.approved()) {
            throw new PaymentGatewayException("Refund failed for payment " + payment.id() + ": " + response.message());
        }
    }

    protected static String requiredDetail(ChargeRequest request, String name) {
        return Guard.requireText(request.detail(name), "payment detail '" + name + "'");
    }

    protected record Instrument(String reference, String summary) {
    }
}
