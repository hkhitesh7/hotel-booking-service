package com.rupeek.hotelbooking.domain.common;

/** The request is valid but conflicts with the current state of the system. */
public class ConflictException extends DomainException {

    public ConflictException(String message) {
        super(message);
    }
}
