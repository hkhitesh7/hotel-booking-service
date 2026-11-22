package com.rupeek.hotelbooking.support;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Striped mutual exclusion by key: work on the same key is serialised, work on different keys
 * mostly runs in parallel. A fixed stripe count keeps memory bounded (no per-key lock leak) at the
 * cost of occasional false sharing between keys that hash to the same stripe. Locks are
 * reentrant, so nested calls on the same key from one thread are safe.
 */
public final class KeyedLocks {

    private final ReentrantLock[] stripes;

    public KeyedLocks(int stripeCount) {
        if (stripeCount <= 0) {
            throw new IllegalArgumentException("stripeCount must be positive");
        }
        stripes = new ReentrantLock[stripeCount];
        for (int i = 0; i < stripeCount; i++) {
            stripes[i] = new ReentrantLock();
        }
    }

    public <T> T withLock(String key, Supplier<T> action) {
        ReentrantLock lock = stripes[Math.floorMod(key.hashCode(), stripes.length)];
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    public void withLock(String key, Runnable action) {
        withLock(key, () -> {
            action.run();
            return null;
        });
    }
}
