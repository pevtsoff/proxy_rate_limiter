package com.example.ratelimiter.adapter.out.persistence;

import com.example.ratelimiter.application.port.out.UnitOfWorkPort;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * {@link UnitOfWorkPort} implemented with Spring's programmatic transactions. Joins an already
 * running transaction (propagation REQUIRED), which is what makes {@code @Transactional}
 * rollback work in the integration tests.
 */
@Component
public class TransactionTemplateUnitOfWork implements UnitOfWorkPort {

    private final TransactionTemplate readWrite;
    private final TransactionTemplate readOnly;

    public TransactionTemplateUnitOfWork(PlatformTransactionManager transactionManager) {
        this.readWrite = new TransactionTemplate(transactionManager);
        this.readOnly = new TransactionTemplate(transactionManager);
        this.readOnly.setReadOnly(true);
    }

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        return readWrite.execute(status -> work.get());
    }

    @Override
    public <T> T inReadOnlyTransaction(Supplier<T> work) {
        return readOnly.execute(status -> work.get());
    }
}
