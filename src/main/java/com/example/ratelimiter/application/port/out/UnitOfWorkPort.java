package com.example.ratelimiter.application.port.out;

import java.util.function.Supplier;

/**
 * Transaction boundary. Keeps the application layer free of framework annotations while still
 * making every use case atomic.
 */
public interface UnitOfWorkPort {

    <T> T inTransaction(Supplier<T> work);

    <T> T inReadOnlyTransaction(Supplier<T> work);

    default void runInTransaction(Runnable work) {
        inTransaction(() -> {
            work.run();
            return null;
        });
    }
}
