package com.example.ratelimiter.unit.support;

import com.example.ratelimiter.application.port.out.UnitOfWorkPort;
import java.util.function.Supplier;

/** Test double that runs the work immediately, without a real transaction. */
public class DirectUnitOfWork implements UnitOfWorkPort {

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        return work.get();
    }

    @Override
    public <T> T inReadOnlyTransaction(Supplier<T> work) {
        return work.get();
    }
}
