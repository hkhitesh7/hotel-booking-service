package com.rupeek.hotelbooking.domain.payment;

public record ChargeOutcome(boolean succeeded, String transactionId, String instrumentSummary, String failureReason) {

    public static ChargeOutcome success(String transactionId, String instrumentSummary) {
        return new ChargeOutcome(true, transactionId, instrumentSummary, null);
    }

    public static ChargeOutcome failure(String instrumentSummary, String reason) {
        return new ChargeOutcome(false, null, instrumentSummary, reason);
    }
}
