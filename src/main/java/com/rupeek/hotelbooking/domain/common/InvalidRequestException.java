package com.rupeek.hotelbooking.domain.common;

/** Input that violates a domain invariant (bad dates, capacity exceeded, unknown option...). */
public class InvalidRequestException extends DomainException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
