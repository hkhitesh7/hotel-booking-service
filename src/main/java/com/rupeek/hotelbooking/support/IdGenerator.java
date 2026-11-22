package com.rupeek.hotelbooking.support;

@FunctionalInterface
public interface IdGenerator {

    String next(String prefix);
}
