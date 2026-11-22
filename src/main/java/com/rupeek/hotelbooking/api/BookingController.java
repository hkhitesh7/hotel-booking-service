package com.rupeek.hotelbooking.api;

import com.rupeek.hotelbooking.api.dto.BookingResponse;
import com.rupeek.hotelbooking.api.dto.CancellationResponse;
import com.rupeek.hotelbooking.api.dto.CreateBookingRequest;
import com.rupeek.hotelbooking.api.dto.PaymentRequest;
import com.rupeek.hotelbooking.api.dto.PaymentResponse;
import com.rupeek.hotelbooking.application.BookingService;
import com.rupeek.hotelbooking.application.CancellationService;
import com.rupeek.hotelbooking.application.PayCommand;
import com.rupeek.hotelbooking.application.PaymentService;
import com.rupeek.hotelbooking.domain.payment.PaymentMethod;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
@Tag(name = "Bookings")
public class BookingController {

    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final CancellationService cancellationService;

    public BookingController(BookingService bookingService, PaymentService paymentService,
                             CancellationService cancellationService) {
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        this.cancellationService = cancellationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Hold rooms for a stay; the booking stays PENDING_PAYMENT until paid or the hold lapses")
    public BookingResponse create(@Valid @RequestBody CreateBookingRequest request) {
        return BookingResponse.from(bookingService.createBooking(request.toCommand()));
    }

    @GetMapping("/{bookingId}")
    public BookingResponse get(@PathVariable String bookingId) {
        return BookingResponse.from(bookingService.getBooking(bookingId));
    }

    @PostMapping("/{bookingId}/payments")
    @Operation(summary = "Pay for a booking. Retrying with the same Idempotency-Key never charges twice")
    public PaymentResponse pay(@PathVariable String bookingId,
                               @RequestHeader("Idempotency-Key") String idempotencyKey,
                               @Valid @RequestBody PaymentRequest request) {
        PayCommand command = new PayCommand(bookingId, new PaymentMethod(request.method()), request.details(),
                idempotencyKey);
        return PaymentResponse.from(paymentService.pay(command));
    }

    @PostMapping("/{bookingId}/cancellation")
    @Operation(summary = "Cancel a booking, apply the property's refund policy and release the rooms")
    public CancellationResponse cancel(@PathVariable String bookingId) {
        return CancellationResponse.from(cancellationService.cancel(bookingId));
    }
}
