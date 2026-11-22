package com.rupeek.hotelbooking.domain.payment;

import com.rupeek.hotelbooking.domain.common.DomainException;

/** The upstream provider rejected or failed an operation we could not recover from locally. */
public class PaymentGatewayException extends DomainException {

    public PaymentGatewayException(String message) {
        super(message);
    }
}
