package com.rupeek.hotelbooking.infrastructure.payment;

import com.rupeek.hotelbooking.domain.common.Guard;
import com.rupeek.hotelbooking.domain.payment.ChargeRequest;
import com.rupeek.hotelbooking.domain.payment.PaymentGateway;
import com.rupeek.hotelbooking.domain.payment.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/** Details: {@code vpa}, e.g. {@code guest@okbank}. */
@Component
public class UpiPaymentProcessor extends GatewayPaymentProcessor {

    private static final Pattern VPA = Pattern.compile("^[A-Za-z0-9._-]{2,256}@[A-Za-z]{2,64}$");

    public UpiPaymentProcessor(PaymentGateway gateway) {
        super(gateway);
    }

    @Override
    public PaymentMethod method() {
        return PaymentMethod.UPI;
    }

    @Override
    protected Instrument resolveInstrument(ChargeRequest request) {
        String vpa = requiredDetail(request, "vpa");
        Guard.require(VPA.matcher(vpa).matches(), "invalid UPI VPA");
        return new Instrument("upi:" + vpa, "UPI " + vpa);
    }
}
