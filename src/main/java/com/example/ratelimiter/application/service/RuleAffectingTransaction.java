package com.example.ratelimiter.application.service;

import com.example.ratelimiter.application.port.out.RuleCacheInvalidationPort;
import com.example.ratelimiter.application.port.out.UnitOfWorkPort;
import java.util.function.Supplier;

/**
 * Runs a configuration change in a transaction and invalidates the rule cache once it has been
 * committed. Invalidating after the commit guarantees that no instance can re-populate the cache
 * from data that is about to be rolled back or is not yet visible.
 */
public final class RuleAffectingTransaction {

    private final UnitOfWorkPort unitOfWork;
    private final RuleCacheInvalidationPort ruleCache;

    public RuleAffectingTransaction(UnitOfWorkPort unitOfWork,
            RuleCacheInvalidationPort ruleCache) {
        this.unitOfWork = unitOfWork;
        this.ruleCache = ruleCache;
    }

    public <T> T execute(Supplier<T> change) {
        T result = unitOfWork.inTransaction(change);
        ruleCache.invalidateAll();
        return result;
    }

    public void run(Runnable change) {
        unitOfWork.runInTransaction(change);
        ruleCache.invalidateAll();
    }
}
