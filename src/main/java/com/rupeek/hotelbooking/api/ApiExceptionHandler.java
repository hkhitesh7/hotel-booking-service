package com.rupeek.hotelbooking.api;

import com.rupeek.hotelbooking.domain.common.ConflictException;
import com.rupeek.hotelbooking.domain.common.DomainException;
import com.rupeek.hotelbooking.domain.common.InvalidRequestException;
import com.rupeek.hotelbooking.domain.common.NotFoundException;
import com.rupeek.hotelbooking.domain.payment.PaymentGatewayException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;
import java.util.stream.Collectors;

/** Maps domain exceptions to RFC 7807 problem responses. */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Map<Class<? extends DomainException>, HttpStatus> STATUS_BY_TYPE = Map.of(
            InvalidRequestException.class, HttpStatus.BAD_REQUEST,
            NotFoundException.class, HttpStatus.NOT_FOUND,
            ConflictException.class, HttpStatus.CONFLICT,
            PaymentGatewayException.class, HttpStatus.BAD_GATEWAY);

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomain(DomainException e) {
        return ProblemDetail.forStatusAndDetail(statusFor(e), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }

    @ExceptionHandler({MissingRequestHeaderException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    public ProblemDetail handleMalformed(Exception e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    private static HttpStatus statusFor(DomainException e) {
        Class<?> type = e.getClass();
        while (DomainException.class.isAssignableFrom(type)) {
            HttpStatus status = STATUS_BY_TYPE.get(type);
            if (status != null) {
                return status;
            }
            type = type.getSuperclass();
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
