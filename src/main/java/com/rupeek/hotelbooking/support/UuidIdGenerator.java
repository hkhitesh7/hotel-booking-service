package com.rupeek.hotelbooking.support;

import java.util.UUID;

public final class UuidIdGenerator implements IdGenerator {

    @Override
    public String next(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
