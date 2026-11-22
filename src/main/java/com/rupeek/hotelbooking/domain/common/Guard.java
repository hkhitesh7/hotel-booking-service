package com.rupeek.hotelbooking.domain.common;

/** Tiny precondition helper so constructors read as a list of invariants. */
public final class Guard {

    private Guard() {
    }

    public static void require(boolean condition, String message) {
        if (!condition) {
            throw new InvalidRequestException(message);
        }
    }

    public static String requireText(String value, String field) {
        require(value != null && !value.isBlank(), field + " must not be blank");
        return value.trim();
    }

    public static <T> T requireNonNull(T value, String field) {
        require(value != null, field + " is required");
        return value;
    }
}
