package com.rupeek.hotelbooking.domain.search;

/**
 * Specification-style predicate over a search candidate. New filters are new implementations;
 * the search service applies whatever list it is given and never changes.
 */
@FunctionalInterface
public interface SearchFilter {

    boolean matches(PropertyAvailability candidate);
}
