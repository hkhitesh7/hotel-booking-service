package com.rupeek.hotelbooking.infrastructure.payment;

import com.rupeek.hotelbooking.domain.payment.ChargeRequest;
import com.rupeek.hotelbooking.domain.payment.PaymentGateway;
import com.rupeek.hotelbooking.domain.payment.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.Locale;

/** Details: {@code provider} (e.g. PAYTM) and {@code walletId}. */
@Component
public class WalletPaymentProcessor extends GatewayPaymentProcessor {

    public WalletPaymentProcessor(PaymentGateway gateway) {
        super(gateway);
    }

    @Override
    public PaymentMethod method() {
        return PaymentMethod.WALLET;
    }

    @Override
    protected Instrument resolveInstrument(ChargeRequest request) {
        String provider = requiredDetail(request, "provider").toUpperCase(Locale.ROOT);
        String walletId = requiredDetail(request, "walletId");
        return new Instrument("wallet:" + provider + ":" + walletId, "WALLET " + provider);
    }
}
