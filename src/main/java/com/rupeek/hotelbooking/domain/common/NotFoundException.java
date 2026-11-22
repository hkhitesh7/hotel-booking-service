package com.rupeek.hotelbooking.domain.common;

public class NotFoundException extends DomainException {

    public NotFoundException(String entity, String id) {
        super(entity + " not found: " + id);
    }
}
