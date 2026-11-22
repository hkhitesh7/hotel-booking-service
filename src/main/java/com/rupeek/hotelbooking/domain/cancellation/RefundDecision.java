package com.rupeek.hotelbooking.domain.cancellation;

import com.rupeek.hotelbooking.domain.common.Money;

import java.util.Currency;

public record RefundDecision(Money amount, String reason) {

    public static RefundDecision none(Currency currency, String reason) {
        return new RefundDecision(Money.zero(currency), reason);
    }

    public boolean isRefundDue() {
        return !amount.isZero();
    }
}
