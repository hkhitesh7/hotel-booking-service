package com.rupeek.hotelbooking.api.dto;

import com.rupeek.hotelbooking.application.CancellationResult;

public record CancellationResponse(BookingResponse booking, MoneyDto refundAmount, String refundReason) {

    public static CancellationResponse from(CancellationResult result) {
        return new CancellationResponse(BookingResponse.from(result.booking()),
                MoneyDto.from(result.refund().amount()), result.refund().reason());
    }
}
