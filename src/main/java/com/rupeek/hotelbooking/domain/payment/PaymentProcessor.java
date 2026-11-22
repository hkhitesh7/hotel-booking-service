package com.rupeek.hotelbooking.domain.payment;

import com.rupeek.hotelbooking.domain.common.Money;

/**
 * Strategy for one payment method. Implementations validate their own method-specific details
 * and talk to whatever gateway backs them. Add a method = add an implementation.
 */
public interface PaymentProcessor {

    PaymentMethod method();

    /** Declines are reported in the outcome; invalid input is thrown as InvalidRequestException. */
    ChargeOutcome charge(ChargeRequest request);

    void refund(Payment payment, Money amount);
}
