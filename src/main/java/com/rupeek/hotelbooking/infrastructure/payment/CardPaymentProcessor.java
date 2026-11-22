package com.rupeek.hotelbooking.infrastructure.payment;

import com.rupeek.hotelbooking.domain.common.Guard;
import com.rupeek.hotelbooking.domain.payment.ChargeRequest;
import com.rupeek.hotelbooking.domain.payment.PaymentGateway;
import com.rupeek.hotelbooking.domain.payment.PaymentMethod;
import org.springframework.stereotype.Component;

/** Details: {@code cardNumber} (Luhn-valid, 12-19 digits). */
@Component
public class CardPaymentProcessor extends GatewayPaymentProcessor {

    public CardPaymentProcessor(PaymentGateway gateway) {
        super(gateway);
    }

    @Override
    public PaymentMethod method() {
        return PaymentMethod.CARD;
    }

    @Override
    protected Instrument resolveInstrument(ChargeRequest request) {
        String number = requiredDetail(request, "cardNumber").replace(" ", "");
        Guard.require(number.matches("\\d{12,19}") && passesLuhn(number), "invalid card number");
        String last4 = number.substring(number.length() - 4);
        return new Instrument("card_tok_" + last4, "CARD ****" + last4);
    }

    private static boolean passesLuhn(String digits) {
        int sum = 0;
        boolean doubleIt = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (doubleIt) {
                d = d * 2 > 9 ? d * 2 - 9 : d * 2;
            }
            sum += d;
            doubleIt = !doubleIt;
        }
        return sum % 10 == 0;
    }
}
