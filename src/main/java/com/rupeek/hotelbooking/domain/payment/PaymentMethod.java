package com.rupeek.hotelbooking.domain.payment;

import com.rupeek.hotelbooking.domain.common.Guard;

import java.util.Locale;

/**
 * Open-ended identifier for a payment method ("CARD", "UPI", "WALLET", ...). Deliberately not an
 * enum: a new method is added by registering a new {@link PaymentProcessor}, without editing
 * any existing type.
 */
public record PaymentMethod(String code) {

    public static final PaymentMethod CARD = new PaymentMethod("CARD");
    public static final PaymentMethod UPI = new PaymentMethod("UPI");
    public static final PaymentMethod WALLET = new PaymentMethod("WALLET");

    public PaymentMethod {
        code = Guard.requireText(code, "paymentMethod").toUpperCase(Locale.ROOT);
    }
}
