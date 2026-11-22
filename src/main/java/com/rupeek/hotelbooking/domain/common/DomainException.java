package com.rupeek.hotelbooking.domain.common;

/** Base type for every business-rule violation raised by the domain or application layers. */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
