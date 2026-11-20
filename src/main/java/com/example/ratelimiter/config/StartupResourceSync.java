package com.example.ratelimiter.config;

import com.example.ratelimiter.adapter.out.http.TargetApiProperties;
import com.example.ratelimiter.application.port.in.SyncResourcesUseCase;
import com.example.ratelimiter.domain.exception.DomainException;
import com.example.ratelimiter.domain.model.ResourceSyncReport;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Imports the target API's endpoints once the proxy is up (MUC1.4 is also available on demand
 * via {@code POST /admin/api/resources/sync}). Failure is logged, never fatal: the proxy keeps
 * working with the default policy and the admin can re-run the sync.
 */
@Component
public class StartupResourceSync {

    private static final Logger LOG = LoggerFactory.getLogger(StartupResourceSync.class);
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(3);

    private final SyncResourcesUseCase sync;
    private final TargetApiProperties properties;

    public StartupResourceSync(SyncResourcesUseCase sync, TargetApiProperties properties) {
        this.sync = sync;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void synchronizeOnStartup() {
        if (properties.syncOnStartup()) {
            Thread.ofVirtual().name("startup-resource-sync").start(this::synchronizeWithRetry);
        }
    }

    private void synchronizeWithRetry() {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                ResourceSyncReport report = sync.synchronize();
                LOG.info("Startup resource sync: {} created, {} unchanged, {} stale",
                        report.created().size(), report.unchanged(), report.stale().size());
                return;
            } catch (DomainException exception) {
                LOG.warn("Startup resource sync attempt {}/{} failed", attempt, MAX_ATTEMPTS,
                        exception);
                sleepBeforeRetry();
            }
        }
        LOG.error("Startup resource sync gave up; run POST /admin/api/resources/sync manually");
    }

    private static void sleepBeforeRetry() {
        try {
            Thread.sleep(RETRY_DELAY);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
