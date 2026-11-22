package com.rupeek.hotelbooking.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * {@code details} is method specific: CARD {cardNumber}, UPI {vpa}, WALLET {provider, walletId}.
 * Kept as a map so a new method needs no new request type.
 */
public record PaymentRequest(@NotBlank String method, Map<String, String> details) {
}
